package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadAccess;
import com.graphhopper.json.Statement;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.RoadEnvironment;
import com.graphhopper.routing.ev.Subnetwork;
import com.graphhopper.routing.ev.Toll;
import com.graphhopper.routing.util.DefaultSnapFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import com.graphhopper.util.TurnCostsConfig;
import com.graphhopper.util.details.PathDetail;
import com.graphhopper.util.exceptions.PointNotFoundException;
import com.graphhopper.util.shapes.GHPoint;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@link Router} of GraphHopper: the fastest route by car on a graph imported from an extract of OpenStreetMap
 * (PBF or XML, as those of Geofabrik), kept in a folder so that later starts load it instead of importing it again.
 *
 * <p><b>Sequence</b>: {@link #importMap(Path, Path)} deletes the graph of the folder, reads the extract and writes the
 * graph of the profile {@value #PROFILE} with its contraction hierarchy (the import of a country takes minutes);
 * {@link #load(Path)} loads the graph of an earlier import; {@link #route(List)} then computes the fastest route
 * with the hierarchy, in milliseconds, through the waypoints of a detour if any (phase 3), with the instructions of
 * the engine: the point where each instruction begins, except the start, the finish and the waypoints, is a maneuver
 * of the route (development plan, phase 2).</p>
 *
 * <p><b>Kinds of road forbidden</b> (issue #11 of GitLab): a route that must not drive some kinds of road
 * ({@link RoadKind}) is computed with a custom model of the request, which multiplies the priority of these roads by 0,
 * without the hierarchy, which allows no model of the request: the search runs on the whole graph, in tens of
 * milliseconds over the distances of a detour (about 20 ms on Switzerland, against 4 ms with the hierarchy).</p>
 *
 * <p><b>Profile</b>: the custom models {@code car.json} and {@code car_avoid_private_etc.json} of GraphHopper (the
 * speeds of the roads for a car, the roads closed to cars and the private roads avoided), with the turn restrictions
 * and the turn costs of a car, so that the route only turns where a car may.</p>
 *
 * <p>Safe for concurrent routing once built; {@link #close()} must not run during a routing.</p>
 */
public final class GraphHopperRouter implements Router {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The name of the profile of the graph: a car. */
  static final String PROFILE = "car";
  /**
   * The values stored on the edges of the graph, needed by the custom models of the profile. GraphHopper adds the
   * class of the road ({@code road_class}, for its instructions) to every graph, the graphs of phases 1 and 2
   * included: it tells the service roads, where no waypoint is placed (phase 3), and the motorways and the
   * expressways; it adds the environment of the road ({@code road_environment}) too, which tells the ferries. The
   * toll ({@code toll}) tells the toll roads (issue #11 of GitLab): the graphs imported before it do not know it.
   */
  static final String ENCODED_VALUES = "car_access, car_average_speed, road_access, toll";
  /**
   * The access of the roads open to everyone: the others (residents, customers, delivery, private, agricultural,
   * forestry) are driven through by a route with waypoints only around its start and its finish.
   */
  static final RoadAccess OPEN = RoadAccess.YES;
  /** The value of {@link #OPEN} in the details of the paths of the engine. */
  static final String OPEN_DETAIL = OPEN.toString();
  /** The value of the class of the tracks in the details of the paths of the engine. */
  static final String TRACK_DETAIL = RoadClass.TRACK.toString();
  /** Tells whether a stretch of a line, by its access ({@code road_access}), is closed to through traffic. */
  static final Predicate<Object> RESTRICTED = value -> !OPEN_DETAIL.equals(value);
  /**
   * Tells whether a stretch of a line, by its class ({@code road_class}), is a track. In Switzerland, the forest
   * roads are closed to motor vehicles unless they are opened (federal act on forests, art. 15), whether or not
   * OpenStreetMap says so: most tracks carry no access tag. A detour drives them like the roads of restricted access
   * (trial in the forest of Charcotet, phase 3).
   */
  static final Predicate<Object> TRACK = TRACK_DETAIL::equals;
  /** The classes of road where no waypoint is placed: service roads (driveways…) and tracks. */
  static final Set<RoadClass> NO_WAYPOINT = EnumSet.of(RoadClass.SERVICE, RoadClass.TRACK);
  /** The custom model of GraphHopper for a car. */
  static final String CAR_MODEL = "car.json";
  /** The custom model of GraphHopper avoiding the private roads and the roads of delivery. */
  static final String AVOID_PRIVATE_MODEL = "car_avoid_private_etc.json";
  /**
   * The curbside of the waypoints: the side of the road where the route reaches them and leaves them, the same, so
   * that the route goes on in the direction it came (development plan, phase 3: no U-turn). The pass-through of
   * GraphHopper, the other way, does not work with the contraction hierarchy.
   */
  static final String WAYPOINT_CURBSIDE = Parameters.Curbsides.CURBSIDE_RIGHT;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The engine, with its graph loaded. */
  private final GraphHopper hopper;
  /**
   * The roads where the engine places the points of a route: those a car may drive, of the main network of the map
   * (the filter of the engine itself).
   */
  private final EdgeFilter roads;
  /** The access of the roads, stored on the edges. */
  private final EnumEncodedValue<RoadAccess> access;
  /** The class of the roads, stored on the edges. */
  private final EnumEncodedValue<RoadClass> roadClass;
  /** Whether the graph knows the toll roads: not those imported before issue #11 of GitLab. */
  private final boolean tolls;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a router on an engine whose graph is loaded.
   *
   * @param hopper the engine, with its graph loaded.
   */
  GraphHopperRouter(final GraphHopper hopper) {
    this.hopper = hopper;
    this.roads = new DefaultSnapFilter(hopper.createWeighting(hopper.getProfile(PROFILE), new PMap()),
                                       hopper.getEncodingManager().getBooleanEncodedValue(Subnetwork.key(PROFILE)));
    this.access = hopper.getEncodingManager().getEnumEncodedValue(RoadAccess.KEY, RoadAccess.class);
    this.roadClass = hopper.getEncodingManager().getEnumEncodedValue(RoadClass.KEY, RoadClass.class);
    this.tolls = hopper.getEncodingManager().hasEncodedValue(Toll.KEY);
  }

  /**
   * Imports an extract of OpenStreetMap into the graph folder, replacing the graph it held, and returns its router.
   *
   * @param map the extract of OpenStreetMap ({@code .osm.pbf}, {@code .osm}, {@code .osm.gz}).
   * @param graph the folder of the graph; its content is deleted first.
   *
   * @return the router of the new graph.
   *
   * @throws RoutingException {@link RoutingError#INVALID_MAP} if the extract cannot be read or imported.
   */
  public static GraphHopperRouter importMap(final Path map, final Path graph) {
    final GraphHopper hopper = engine(graph);
    hopper.setOSMFile(map.toString());
    hopper.clean();
    try {
      hopper.importOrLoad();
    } catch (final RuntimeException e) {
      hopper.close();
      throw new RoutingException(RoutingError.INVALID_MAP, e);
    }
    return new GraphHopperRouter(hopper);
  }

  /**
   * Loads the graph of an earlier import, if the folder holds one.
   *
   * @param graph the folder of the graph.
   *
   * @return the router of the graph, or nothing if the folder holds no graph.
   *
   * @throws RoutingException {@link RoutingError#INVALID_GRAPH} if the graph of the folder cannot be loaded.
   */
  public static Optional<GraphHopperRouter> load(final Path graph) {
    final GraphHopper hopper = engine(graph);
    final boolean loaded;
    try {
      loaded = hopper.load();
    } catch (final RuntimeException e) {
      hopper.close();
      throw new RoutingException(RoutingError.INVALID_GRAPH, e);
    }
    if (!loaded) {
      hopper.close();
      return Optional.empty();
    }
    return Optional.of(new GraphHopperRouter(hopper));
  }

  /**
   * Configures an engine for the profile of a car, its graph in a folder.
   *
   * @param graph the folder of the graph.
   *
   * @return the engine, without graph yet.
   */
  static GraphHopper engine(final Path graph) {
    final CustomModel model = CustomModel.merge(GHUtility.loadCustomModelFromJar(CAR_MODEL),
                                                GHUtility.loadCustomModelFromJar(AVOID_PRIVATE_MODEL));
    final GraphHopper hopper = new GraphHopper();
    hopper.setGraphHopperLocation(graph.toString());
    hopper.setEncodedValuesString(ENCODED_VALUES);
    hopper.setProfiles(new Profile(PROFILE).setCustomModel(model).setTurnCostsConfig(TurnCostsConfig.car()));
    hopper.getCHPreparationHandler().setCHProfiles(new CHProfile(PROFILE));
    return hopper;
  }

  // *******************************************************************************************************************
  // Router
  // *******************************************************************************************************************

  /**
   * {@inheritDoc}
   *
   * <p>The engine places each point on the nearest road; the route runs through these places, in their order. Each
   * waypoint has the curbside {@value #WAYPOINT_CURBSIDE} ({@link #curbsides(int)}): the route reaches it and leaves
   * it in the same direction, without turning back there. The curbside only holds on a road between two junctions:
   * a waypoint whose nearest place is a junction or the end of a dead end, where the route could turn back, gives no
   * route ({@link #checkWaypoints(List)}). No waypoint lies on a road of restricted access (residents, private…), on
   * a service road (driveway, parking aisle…) nor on a track, and a route through waypoints drives the roads of
   * restricted access and the tracks only around its start and its finish
   * ({@link #checkStretches(List, Predicate, int)}), where the user may well have placed them; otherwise there is no
   * route. Its maneuvers are the points where the instructions of the engine begin, the first one (the start)
   * excepted, and those of the waypoints excepted ({@link #maneuvers(List, int)}). The kinds of road forbidden are
   * avoided by a custom model of the request, without the hierarchy ({@link #avoiding(Set)}).</p>
   */
  @Override
  public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
    checkWaypoints(points);
    final List<GHPoint> places = points.stream().map(point -> new GHPoint(point.latitude(), point.longitude()))
        .toList();
    final GHRequest request = new GHRequest(places).setProfile(PROFILE).setCurbsides(curbsides(places.size()))
        .setPathDetails(List.of(RoadAccess.KEY, RoadClass.KEY));
    if (!avoided.isEmpty()) {
      request.setCustomModel(avoiding(avoided));
      request.putHint(Parameters.CH.DISABLE, true);
    }
    final GHResponse response = hopper.route(request);
    if (response.hasErrors()) {
      throw new RoutingException(error(response.getErrors()));
    }
    final ResponsePath path = response.getBest();
    final PointList line = path.getPoints();
    if (points.size() > 2) {
      checkStretches(path.getPathDetails().get(RoadAccess.KEY), RESTRICTED, line.size());
      checkStretches(path.getPathDetails().get(RoadClass.KEY), TRACK, line.size());
    }
    return new Route(points(line), maneuvers(path.getInstructions(), line.size()), path.getDistance(),
                     Duration.ofMillis(path.getTime()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    hopper.close();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Translates the errors of the engine into the reason of the failure: a point that no road is near, or no route.
   * The errors themselves, whose messages hold the positions, are dropped (AGENTS.md §5).
   *
   * @param errors the errors of the engine, at least one.
   *
   * @return {@link RoutingError#POINT_NOT_FOUND} if a point is far from every road of the map (or outside the map),
   *         {@link RoutingError#NO_ROUTE} otherwise.
   */
  static RoutingError error(final List<Throwable> errors) {
    if (errors.stream().anyMatch(PointNotFoundException.class::isInstance)) {
      return RoutingError.POINT_NOT_FOUND;
    }
    return RoutingError.NO_ROUTE;
  }

  /**
   * Builds the custom model of a request that never drives some kinds of road: their priority is multiplied by 0.
   *
   * @param avoided the kinds of road forbidden, at least one.
   *
   * @return the custom model, merged by the engine with the one of the profile.
   *
   * @throws RoutingException {@link RoutingError#OUTDATED_MAP} if the toll roads are forbidden and the graph, imported
   *                          before them, does not know them.
   */
  private CustomModel avoiding(final Set<RoadKind> avoided) {
    if (avoided.contains(RoadKind.TOLL) && !tolls) {
      throw new RoutingException(RoutingError.OUTDATED_MAP);
    }
    final String condition = avoided.stream().sorted().map(GraphHopperRouter::condition)
        .collect(Collectors.joining(" || "));
    return new CustomModel().addToPriority(Statement.If(condition, Statement.Op.MULTIPLY, "0"));
  }

  /**
   * Writes the condition of a custom model that matches the roads of a kind.
   *
   * @param kind the kind of road.
   *
   * @return the condition on the values stored on the edges: their class, their environment — a ferry, or a
   *         car-carrying train, a ferry for the engine — or their toll.
   */
  static String condition(final RoadKind kind) {
    return switch (kind) {
      case MOTORWAY -> RoadClass.KEY + " == " + RoadClass.MOTORWAY.name();
      case TRUNK -> RoadClass.KEY + " == " + RoadClass.TRUNK.name();
      case FERRY -> RoadEnvironment.KEY + " == " + RoadEnvironment.FERRY.name();
      case TOLL -> Toll.KEY + " == " + Toll.ALL.name();
    };
  }

  /**
   * Finds the maneuvers of a route: the points of its line where the instructions of the engine begin, between the
   * start and the finish. Each instruction covers the points from its beginning to the beginning of the next one; the
   * last one, the finish, covers the last point.
   *
   * <p>At a waypoint, the engine ends a leg with an instruction {@link Instruction#REACHED_VIA}, covering no point,
   * and begins the next leg with an instruction to go on, at the same point: neither is a maneuver, since the driver
   * goes on without being told anything. A turn made at the waypoint itself is lost with them: the engine places the
   * waypoints on the nearest point of a road, seldom a junction.</p>
   *
   * @param instructions the instructions of the engine, in their order along the line.
   * @param size the number of points of the line.
   *
   * @return the indices, in the line, of the points of the maneuvers, in increasing order.
   */
  static List<Integer> maneuvers(final List<Instruction> instructions, final int size) {
    final List<Integer> maneuvers = new ArrayList<>(instructions.size());
    int index = 0;
    boolean waypoint = false;
    for (final Instruction instruction : instructions) {
      final boolean via = instruction.getSign() == Instruction.REACHED_VIA;
      if (!via && !waypoint && index > 0 && index < size - 1) {
        maneuvers.add(index);
      }
      waypoint = via;
      index += instruction.getLength();
    }
    return maneuvers;
  }

  /**
   * Checks that the route can pass each waypoint without turning back, on a road open to everyone: its nearest place
   * on the roads must lie between two junctions, not on a junction nor at the end of a dead end, where the curbside
   * does not hold, and on a road of open access that is neither a service road (a driveway, often private without
   * being tagged so, a parking aisle…) nor a track ({@link #TRACK}).
   *
   * @param points the start, the waypoints and the finish.
   *
   * @throws RoutingException {@link RoutingError#POINT_NOT_FOUND} if no road is near a waypoint,
   *                          {@link RoutingError#NO_ROUTE} if the nearest place of a waypoint is a junction, the end
   *                          of a dead end, a road of restricted access, a service road or a track.
   */
  private void checkWaypoints(final List<GeoPoint> points) {
    for (final GeoPoint waypoint : points.subList(1, points.size() - 1)) {
      final Snap snap = hopper.getLocationIndex().findClosest(waypoint.latitude(), waypoint.longitude(), roads);
      if (!snap.isValid()) {
        throw new RoutingException(RoutingError.POINT_NOT_FOUND);
      }
      if (snap.getSnappedPosition() == Snap.Position.TOWER || snap.getClosestEdge().get(access) != OPEN
          || NO_WAYPOINT.contains(snap.getClosestEdge().get(roadClass))) {
        throw new RoutingException(RoutingError.NO_ROUTE);
      }
    }
  }

  /**
   * Checks that a route through waypoints drives some roads (of restricted access, tracks) only around its start and
   * its finish: each stretch of such roads must begin at the start or end at the finish.
   *
   * @param details the stretches of the line by a value of the roads (a detail of the engine: {@code road_access},
   *                {@code road_class}), each from the index of its first point to the index of its last one.
   * @param closed tells, from the value of a stretch, whether it is a road to drive only around the start and the
   *               finish ({@link #RESTRICTED}, {@link #TRACK}).
   * @param size the number of points of the line.
   *
   * @throws RoutingException {@link RoutingError#NO_ROUTE} if such a stretch lies between the start and the finish,
   *                          touching neither.
   */
  static void checkStretches(final List<PathDetail> details, final Predicate<Object> closed, final int size) {
    for (final PathDetail detail : details) {
      if (closed.test(detail.getValue()) && detail.getFirst() > 0 && detail.getLast() < size - 1) {
        throw new RoutingException(RoutingError.NO_ROUTE);
      }
    }
  }

  /**
   * Chooses the curbsides of the points of a route: any side for the start and the finish, the side
   * {@value #WAYPOINT_CURBSIDE} for the waypoints. A waypoint that cannot be reached on this side (a one-way road
   * where it lies on the other side) gives no route.
   *
   * @param size the number of points, the start and the finish included, at least two.
   *
   * @return the curbsides, one per point, or none for a route without waypoint, the engine then reaching the finish
   *         from any side.
   */
  static List<String> curbsides(final int size) {
    if (size == 2) {
      return List.of();
    }
    final List<String> curbsides = new ArrayList<>(size);
    curbsides.add(Parameters.Curbsides.CURBSIDE_ANY);
    for (int i = 2; i < size; i++) {
      curbsides.add(WAYPOINT_CURBSIDE);
    }
    curbsides.add(Parameters.Curbsides.CURBSIDE_ANY);
    return curbsides;
  }

  /**
   * Converts the points of the engine into points of the domain.
   *
   * @param list the points of the engine, in WGS 84.
   *
   * @return the points of the domain, in the same order.
   */
  private static List<GeoPoint> points(final PointList list) {
    final List<GeoPoint> points = new ArrayList<>(list.size());
    for (int i = 0; i < list.size(); i++) {
      points.add(new GeoPoint(list.getLat(i), list.getLon(i)));
    }
    return points;
  }

}
