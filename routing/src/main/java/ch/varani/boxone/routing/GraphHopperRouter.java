package ch.varani.boxone.routing;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.GHUtility;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.PointList;
import com.graphhopper.util.TurnCostsConfig;
import com.graphhopper.util.exceptions.PointNotFoundException;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@link Router} of GraphHopper: the fastest route by car on a graph imported from an extract of OpenStreetMap
 * (PBF or XML, as those of Geofabrik), kept in a folder so that later starts load it instead of importing it again.
 *
 * <p><b>Sequence</b>: {@link #importMap(Path, Path)} deletes the graph of the folder, reads the extract and writes the
 * graph of the profile {@value #PROFILE} with its contraction hierarchy (the import of a country takes minutes);
 * {@link #load(Path)} loads the graph of an earlier import; {@link #route(GeoPoint, GeoPoint)} then computes the
 * fastest route with the hierarchy, in milliseconds, with the instructions of the engine: the point where each
 * instruction begins, except the start and the finish, is a maneuver of the route (development plan, phase 2).</p>
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
  /** The values stored on the edges of the graph, needed by the custom models of the profile. */
  static final String ENCODED_VALUES = "car_access, car_average_speed, road_access";
  /** The custom model of GraphHopper for a car. */
  static final String CAR_MODEL = "car.json";
  /** The custom model of GraphHopper avoiding the private roads and the roads of delivery. */
  static final String AVOID_PRIVATE_MODEL = "car_avoid_private_etc.json";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The engine, with its graph loaded. */
  private final GraphHopper hopper;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a router on an engine whose graph is loaded.
   *
   * @param hopper the engine, with its graph loaded.
   */
  private GraphHopperRouter(final GraphHopper hopper) {
    this.hopper = hopper;
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
  private static GraphHopper engine(final Path graph) {
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
   * <p>The engine places the start and the finish on the nearest roads; the route runs between these two places. Its
   * maneuvers are the points where the instructions of the engine begin, the first one (the start) excepted.</p>
   */
  @Override
  public Route route(final GeoPoint start, final GeoPoint finish) {
    final GHResponse response = hopper.route(new GHRequest(start.latitude(), start.longitude(), finish.latitude(),
                                                           finish.longitude()).setProfile(PROFILE));
    if (response.hasErrors()) {
      throw new RoutingException(error(response.getErrors()));
    }
    final ResponsePath path = response.getBest();
    final PointList line = path.getPoints();
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
   * Finds the maneuvers of a route: the points of its line where the instructions of the engine begin, between the
   * start and the finish. Each instruction covers the points from its beginning to the beginning of the next one; the
   * last one, the finish, covers the last point.
   *
   * @param instructions the instructions of the engine, in their order along the line.
   * @param size the number of points of the line.
   *
   * @return the indices, in the line, of the points of the maneuvers, in increasing order.
   */
  static List<Integer> maneuvers(final List<Instruction> instructions, final int size) {
    final List<Integer> maneuvers = new ArrayList<>(instructions.size());
    int index = 0;
    for (final Instruction instruction : instructions) {
      if (index > 0 && index < size - 1) {
        maneuvers.add(index);
      }
      index += instruction.getLength();
    }
    return maneuvers;
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
