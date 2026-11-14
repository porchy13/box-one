package ch.varani.boxone.routing;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Route;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EnumEncodedValue;
import com.graphhopper.routing.ev.RoadClass;
import com.graphhopper.routing.ev.Roundabout;
import com.graphhopper.routing.ev.Surface;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.AngleCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Reads the geometry of the junctions of a route in the graph of GraphHopper (development plan, phase 4: "for each
 * junction, take all its branches in the graph and compute their angles; the roundabouts a case of their own from the
 * start").
 *
 * <p><b>Algorithm</b>, for each maneuver of the route:</p>
 * <ol>
 *   <li>the point of the maneuver is placed on the nearest road a car may drive; it is a node of the graph, where the
 *       instruction of the engine begins, or within {@value #SAME_NODE} m of it for a route read back from a roadbook
 *       ({@link #node(Snap, GeoPoint)}). Otherwise (a route not computed on this graph), the junction is the line of
 *       the route itself ({@link #along(List, int)});</li>
 *   <li>the branches are the edges of the node a car may drive one way at least, footways and cycleways excepted,
 *       whatever their access; the bearing of each is read {@value #BRANCH_DISTANCE} metres along it, or at its other
 *       end if it is shorter ({@link #branches(int)}); its kind is a track if the road is classified as a track, or if
 *       its surface is not hard ({@link #UNPAVED}), a tarmac road otherwise ({@link #kind(RoadClass, Surface)});</li>
 *   <li>the entry and the exit are the branches whose first stretch has the closest direction to the stretch of the
 *       route before the maneuver, and to the stretch after it: the points of the route are the points of the edges,
 *       or lie on them;</li>
 *   <li>a route that enters a roundabout there, its exit on the ring and its entry not, has a junction of the shape
 *       of a roundabout ({@link #roundabout(int, Branch, List, int)}), the two halves of a divided road meeting its
 *       ring drawn as one leg ({@link #joinHalves(List)}, {@link #halves(Leg, Leg)}); otherwise the junction is a
 *       crossing of its branches.</li>
 * </ol>
 *
 * <p>Safe for concurrent use: each call explores the graph with explorers of its own.</p>
 */
final class JunctionReader {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The distance along a branch where its bearing is read, in metres: beyond the curve that joins it to the other
   * roads, so that the tulip shows the direction of the road, and short enough to stay near the junction. Confirmed by
   * the maintainer (2026-11-14) on a roadbook of Yverdon-les-Bains: at 10 m, the curve leaving a ring turned an exit
   * straight ahead to the right; at 40 m, a road was read beyond its next bend.
   */
  static final double BRANCH_DISTANCE = 20;
  /**
   * The largest difference between two directions of the same road, in degrees: the stretch of the route leaving a
   * node of the ring and the first stretch of a branch of the node, along the same points, differ by the rounding of
   * the coordinates only — up to a few degrees on a stretch of a few metres for a route read back from a roadbook,
   * whose coordinates are written to six decimals (about 0.1 m).
   */
  static final double SAME_DIRECTION = 5;
  /**
   * The largest distance between a point of a route and the node of the graph it stands for, in metres: the points of
   * a route computed on the graph are its nodes, those of a route read back from a roadbook are rounded to six
   * decimals (about 0.1 m).
   */
  static final double SAME_NODE = 1;
  /**
   * The surfaces of the roads that a tulip draws as tracks: no hard surface (FIA, 2026 Cross-Country Rally Sporting
   * Regulations, Appendix III, §5.14: a track, a single line, as opposed to a tarmac road).
   */
  static final Set<Surface> UNPAVED = EnumSet.of(Surface.UNPAVED, Surface.COMPACTED, Surface.FINE_GRAVEL,
                                                 Surface.GRAVEL, Surface.GROUND, Surface.DIRT, Surface.GRASS,
                                                 Surface.SAND);
  /**
   * The largest angle between the directions of the two halves of a divided road meeting a ring, in degrees, read
   * {@value #BRANCH_DISTANCE} metres along them: the halves leave the ring side by side, towards the same road, even
   * when OpenStreetMap names them differently (trial in Terra Pirata, 2026-11-14: the Quai de Nogent and the Rue des
   * Cygnes, 23 degrees apart, at Yverdon-les-Bains). Confirmed by the maintainer (2026-11-14): at 15 degrees, the
   * fixed roundabouts came back with a road too many; at 45 degrees, distinct roads were drawn as one.
   */
  static final double DIVIDED_ROAD_ANGLE = 30;
  /** The degrees of a full turn. */
  static final double FULL_TURN = 360;
  /** The degrees of a half turn. */
  static final double HALF_TURN = 180;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The graph of the engine. */
  private final BaseGraph graph;
  /** The places of the nodes of the graph. */
  private final NodeAccess nodes;
  /** The index of the places of the graph, to find the node of a maneuver. */
  private final LocationIndex index;
  /** The roads where the point of a maneuver is placed: those of the routing. */
  private final EdgeFilter roads;
  /** The access of the roads for a car, stored on the edges, in both directions. */
  private final BooleanEncodedValue access;
  /** Whether a road is part of a roundabout, stored on the edges. */
  private final BooleanEncodedValue ring;
  /** The class of the roads, stored on the edges. */
  private final EnumEncodedValue<RoadClass> roadClass;
  /** The surface of the roads, stored on the edges; {@code null} for a graph imported before it. */
  private final EnumEncodedValue<Surface> surface;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the reader of the junctions of a graph.
   *
   * @param graph the graph of the engine.
   * @param index the index of the places of the graph.
   * @param roads the roads where the point of a maneuver is placed.
   * @param encodings the values stored on the edges of the graph: the access of a car, the roundabouts, the class of
   *                  the roads and, for the graphs imported since phase 4, their surface.
   */
  JunctionReader(final BaseGraph graph, final LocationIndex index, final EdgeFilter roads,
                 final EncodingManager encodings) {
    this.graph = graph;
    this.nodes = graph.getNodeAccess();
    this.index = index;
    this.roads = roads;
    this.access = encodings.getBooleanEncodedValue(VehicleAccess.key(GraphHopperRouter.PROFILE));
    this.ring = encodings.getBooleanEncodedValue(Roundabout.KEY);
    this.roadClass = encodings.getEnumEncodedValue(RoadClass.KEY, RoadClass.class);
    this.surface = encodings.hasEncodedValue(Surface.KEY) ? encodings.getEnumEncodedValue(Surface.KEY, Surface.class)
                                                          : null;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads the geometry of the junctions of a route.
   *
   * @param route the route.
   *
   * @return the junctions, one per maneuver of the route, in their order.
   */
  List<Junction> junctions(final Route route) {
    return route.maneuvers().stream().map(maneuver -> junction(route.points(), maneuver)).toList();
  }

  /**
   * Reads the geometry of the junction of a maneuver.
   *
   * @param points the points of the route.
   * @param maneuver the index of the point of the maneuver, between the start and the finish.
   *
   * @return the junction: a roundabout entered there, the crossing of the branches of its node, or the line of the
   *         route if its point is not a node of the graph, or if the route leaves it as it came.
   */
  Junction junction(final List<GeoPoint> points, final int maneuver) {
    final GeoPoint point = points.get(maneuver);
    final Snap snap = index.findClosest(point.latitude(), point.longitude(), roads);
    final Optional<Integer> found = snap.isValid() ? node(snap, point) : Optional.empty();
    if (found.isEmpty()) {
      return along(points, maneuver);
    }
    final int node = found.get();
    final List<Branch> branches = branches(node);
    final int entry = closest(branches, bearing(point, points.get(maneuver - 1)));
    final int exit = closest(branches, bearing(point, points.get(maneuver + 1)));
    if (entry == exit) {
      return along(points, maneuver);
    }
    if (branches.get(exit).roundabout() && !branches.get(entry).roundabout()) {
      final Optional<Junction> roundabout = roundabout(node, branches.get(entry), points, maneuver);
      if (roundabout.isPresent()) {
        return roundabout.get();
      }
    }
    return new Junction(JunctionShape.CROSSING, branches.stream().map(Branch::bearing).toList(),
                        branches.stream().map(Branch::kind).toList(), entry, exit, 0);
  }

  /**
   * Finds the node of the graph a point of a route stands for: the end of the nearest road closer to the point, if it
   * is within {@value #SAME_NODE} m of it.
   *
   * @param snap the place of the point on the nearest road, valid.
   * @param point the point.
   *
   * @return the node, or nothing if the point lies along the road, away from its ends.
   */
  private Optional<Integer> node(final Snap snap, final GeoPoint point) {
    final EdgeIteratorState edge = snap.getClosestEdge();
    final double base = distance(point, edge.getBaseNode());
    final double adjacent = distance(point, edge.getAdjNode());
    final int nearest = base <= adjacent ? edge.getBaseNode() : edge.getAdjNode();
    return Math.min(base, adjacent) <= SAME_NODE ? Optional.of(nearest) : Optional.empty();
  }

  /**
   * Computes the distance from a point to a node of the graph.
   *
   * @param point the point.
   * @param node the node.
   *
   * @return the distance, in metres.
   */
  private double distance(final GeoPoint point, final int node) {
    return DistanceCalcEarth.DIST_EARTH.calcDist(point.latitude(), point.longitude(), nodes.getLat(node),
                                                 nodes.getLon(node));
  }

  /**
   * Gives the junction of a maneuver as the line of the route draws it, when the graph tells nothing of it: two
   * branches, the stretch before the point and the stretch after it, both drawn as tarmac roads, the graph telling
   * nothing of their surface.
   *
   * @param points the points of the route.
   * @param maneuver the index of the point of the maneuver, between the start and the finish.
   *
   * @return a crossing of two branches, the entry first.
   */
  static Junction along(final List<GeoPoint> points, final int maneuver) {
    final GeoPoint point = points.get(maneuver);
    return new Junction(JunctionShape.CROSSING, List.of(bearing(point, points.get(maneuver - 1)),
                                                        bearing(point, points.get(maneuver + 1))),
                        List.of(BranchKind.TARMAC, BranchKind.TARMAC), 0, 1, 0);
  }

  /**
   * Reads the branches of a node: its edges a car may drive one way at least.
   *
   * @param node the node.
   *
   * @return the branches, in the order of the edges of the node in the graph.
   */
  private List<Branch> branches(final int node) {
    final List<Branch> branches = new ArrayList<>();
    final EdgeIterator edges = graph.createEdgeExplorer().setBaseNode(node);
    while (edges.next()) {
      if (edges.get(access) || edges.getReverse(access)) {
        final PointList line = edges.fetchWayGeometry(FetchMode.ALL);
        final Surface hard = surface == null ? Surface.MISSING : edges.get(surface);
        branches.add(new Branch(edges.getEdge(), edges.getAdjNode(), bearing(line, 1), bearing(line, reach(line)),
                                kind(edges.get(roadClass), hard), edges.get(ring), edges.getName(),
                                edges.getReverse(access), edges.get(access)));
      }
    }
    return branches;
  }

  /**
   * Tells the kind of a road, as a tulip draws it.
   *
   * @param roadClass the class of the road.
   * @param roadSurface the surface of the road, {@link Surface#MISSING} if unknown.
   *
   * @return a track if the road is classified as a track or its surface is not hard ({@link #UNPAVED}), a tarmac road
   *         otherwise, an unknown surface included.
   */
  static BranchKind kind(final RoadClass roadClass, final Surface roadSurface) {
    return roadClass == RoadClass.TRACK || UNPAVED.contains(roadSurface) ? BranchKind.TRACK : BranchKind.TARMAC;
  }

  /**
   * Finds the point of a line where the bearing of a branch is read: the first one at least
   * {@value #BRANCH_DISTANCE} metres from its first point, along the line, or its last point.
   *
   * @param line the line of the branch, from the node, at least two points.
   *
   * @return the index of the point in the line, from 1.
   */
  static int reach(final PointList line) {
    double distance = 0;
    int point = 1;
    while (point < line.size() - 1) {
      distance += DistanceCalcEarth.DIST_EARTH.calcDist(line.getLat(point - 1), line.getLon(point - 1),
                                                        line.getLat(point), line.getLon(point));
      if (distance >= BRANCH_DISTANCE) {
        return point;
      }
      point++;
    }
    return point;
  }

  /**
   * Reads the geometry of a roundabout entered at a node: its ring is driven from the node, edge after edge, until the
   * node comes back ({@link #ring(int)}); each road that meets the ring is a branch, given by the bearing of the place
   * where it meets the ring, from the centre of the ring (the mean of its points, {@link #centre(List)}); the
   * direction of the ring is the sign of its area ({@link #area(List)}); the roads meeting the ring at the same node
   * are in the order of their directions along the ring ({@link #aroundNode(boolean)}). The exit is the first branch,
   * along the ring, that the route takes ({@link #leaving(List, int, GeoPoint, double)}); the departure of the
   * junction is the point where the route takes it.
   *
   * @param node the node where the route enters the ring.
   * @param entry the branch the route comes from.
   * @param points the points of the route.
   * @param maneuver the index of the node in the route.
   *
   * @return the roundabout, or nothing if the ring does not come back to the node (a roundabout not closed in the map)
   *         or if the route does not leave it.
   */
  private Optional<Junction> roundabout(final int node, final Branch entry, final List<GeoPoint> points,
                                        final int maneuver) {
    final Optional<Ring> found = ring(node);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    final GeoPoint centre = centre(found.get().shape());
    final boolean counterclockwise = area(found.get().shape()) > 0;
    final List<Leg> legs = new ArrayList<>();
    int departure = -1;
    for (final int ringNode : found.get().nodes()) {
      final GeoPoint place = new GeoPoint(nodes.getLat(ringNode), nodes.getLon(ringNode));
      final List<Leg> atNode = new ArrayList<>();
      for (final Branch branch : branches(ringNode).stream().filter(road -> !road.roundabout()).toList()) {
        final boolean isEntry = branch.edge() == entry.edge();
        final OptionalInt leaving = isEntry || departure >= 0 ? OptionalInt.empty()
                                                               : leaving(points, maneuver, place, branch.first());
        final boolean isExit = leaving.isPresent();
        if (isExit) {
          departure = leaving.getAsInt() - maneuver;
        }
        atNode.add(new Leg(bearing(centre, place), branch.bearing(), branch.end(), branch.kind(), branch.name(),
                           branch.in(), branch.out(), isEntry, isExit));
      }
      atNode.sort(aroundNode(counterclockwise));
      legs.addAll(atNode);
    }
    if (departure < 0) {
      return Optional.empty();
    }
    final List<Leg> merged = joinHalves(legs);
    final JunctionShape direction = counterclockwise ? JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT
                                                     : JunctionShape.CLOCKWISE_ROUNDABOUT;
    return Optional.of(new Junction(direction, merged.stream().map(Leg::position).toList(),
                                    merged.stream().map(Leg::kind).toList(), index(merged, Leg::entry),
                                    index(merged, Leg::exit), departure));
  }

  /**
   * Orders the roads meeting a ring at the same node as the ring is driven: by the side of the node they leave to,
   * their direction less the bearing of the node from the centre of the ring, from the right to the left of the
   * outside for a ring driven counterclockwise, the other way for a ring driven clockwise. The node holds them all at
   * the same place around the ring; in this order, the halves of a divided road meeting the ring at two nodes stay
   * next to each other (trial in Terra Pirata, 2026-11-14: at Yverdon-les-Bains, the slip road from Neuchâtel and
   * the half of the Avenue du Lac leaving the ring at the same node, the half of the Avenue du Lac going in at the
   * next one).
   *
   * @param counterclockwise whether the ring is driven counterclockwise.
   *
   * @return the order of the legs of a node.
   */
  static Comparator<Leg> aroundNode(final boolean counterclockwise) {
    final Comparator<Leg> rightToLeft = Comparator.comparingDouble(JunctionReader::side);
    return counterclockwise ? rightToLeft.reversed() : rightToLeft;
  }

  /**
   * Computes the side a leg leaves its node to: its direction less the bearing of the node from the centre of the
   * ring, positive to the right of the outside of the ring, negative to its left.
   *
   * @param leg the leg.
   *
   * @return the side, in degrees, from -{@value #HALF_TURN} excluded to {@value #HALF_TURN} included.
   */
  static double side(final Leg leg) {
    final double side = ((leg.direction() - leg.position()) % FULL_TURN + FULL_TURN) % FULL_TURN;
    return side > HALF_TURN ? side - FULL_TURN : side;
  }

  /**
   * Draws as one the two halves of a divided road that meets a ring: two legs next to each other along the ring, of
   * the same name, one going into the ring only and the other out of it only ({@link #halves(Leg, Leg)}), the last
   * leg and the first one included, the ring being closed.
   *
   * @param legs the legs of the ring, in the order the ring is driven from the entry.
   *
   * @return the legs, each divided road as one leg, in the same order.
   */
  static List<Leg> joinHalves(final List<Leg> legs) {
    final List<Leg> merged = new ArrayList<>(legs.size());
    int i = 0;
    while (i < legs.size()) {
      if (i + 1 < legs.size() && halves(legs.get(i), legs.get(i + 1))) {
        merged.add(join(legs.get(i), legs.get(i + 1)));
        i += 2;
      } else {
        merged.add(legs.get(i));
        i++;
      }
    }
    if (halves(merged.getLast(), merged.getFirst())) {
      merged.set(0, join(merged.removeLast(), merged.getFirst()));
    }
    return merged;
  }

  /**
   * Tells whether two legs of a ring are the two halves of one divided road: one going into the ring only and the
   * other out of it only, of the same name, not empty, or meeting at their other end, or leaving the ring in
   * directions less than {@value #DIVIDED_ROAD_ANGLE} degrees apart; and not the entry and the exit of the route
   * together, which stay apart.
   *
   * @param first a leg.
   * @param second the next leg along the ring.
   *
   * @return {@code true} if the legs are drawn as one road.
   */
  static boolean halves(final Leg first, final Leg second) {
    return first.oneWay() && second.oneWay() && first.in() != second.in()
           && !(first.entry() && second.exit() || first.exit() && second.entry())
           && (sameName(first, second) || first.end() == second.end()
               || difference(first.direction(), second.direction()) <= DIVIDED_ROAD_ANGLE);
  }

  /**
   * Tells whether two legs bear the same name.
   *
   * @param first a leg.
   * @param second another leg.
   *
   * @return {@code true} if their names are equal and not empty.
   */
  private static boolean sameName(final Leg first, final Leg second) {
    return !first.name().isEmpty() && first.name().equals(second.name());
  }

  /**
   * Joins the two halves of a divided road into one leg, halfway between them around the ring.
   *
   * @param first a half.
   * @param second the other half.
   *
   * @return the leg: at the mean bearing of the halves, in the direction of the first, of its kind and name, both
   *         ways, the entry or the exit if one of the halves is.
   */
  static Leg join(final Leg first, final Leg second) {
    final double one = Math.toRadians(first.position());
    final double other = Math.toRadians(second.position());
    final double mean = Math.toDegrees(Math.atan2(Math.sin(one) + Math.sin(other), Math.cos(one) + Math.cos(other)));
    return new Leg(mean < 0 ? mean + FULL_TURN : mean, first.direction(), first.end(), first.kind(), first.name(),
                   true, true,
                   first.entry() || second.entry(), first.exit() || second.exit());
  }

  /**
   * Finds the index of the leg of a property.
   *
   * @param legs the legs.
   * @param property the property, held by one leg.
   *
   * @return the index of the first leg that holds it.
   */
  private static int index(final List<Leg> legs, final Predicate<Leg> property) {
    return IntStream.range(0, legs.size()).filter(i -> property.test(legs.get(i))).findFirst().orElseThrow();
  }

  /**
   * Drives the ring of a roundabout from a node, by the edges of the ring a car may drive away from each node.
   *
   * @param node the node where the route enters the ring.
   *
   * @return the ring, its nodes in the order they are driven, the first one the node; nothing if a node of the ring
   *         has no edge of the ring to go on, or if the ring comes back to another node than the first one.
   */
  private Optional<Ring> ring(final int node) {
    final List<Integer> ringNodes = new ArrayList<>();
    final List<GeoPoint> shape = new ArrayList<>();
    final Set<Integer> seen = new HashSet<>();
    int current = node;
    while (seen.add(current)) {
      ringNodes.add(current);
      final Optional<EdgeIteratorState> next = next(current);
      if (next.isEmpty()) {
        return Optional.empty();
      }
      final PointList line = next.get().fetchWayGeometry(FetchMode.BASE_AND_PILLAR);
      for (int i = 0; i < line.size(); i++) {
        shape.add(new GeoPoint(line.getLat(i), line.getLon(i)));
      }
      current = next.get().getAdjNode();
    }
    return current == node ? Optional.of(new Ring(ringNodes, shape)) : Optional.empty();
  }

  /**
   * Finds the edge that follows a node on the ring of a roundabout.
   *
   * @param node the node of the ring.
   *
   * @return the first edge of the ring a car may drive away from the node, from the node, or nothing if there is none.
   */
  private Optional<EdgeIteratorState> next(final int node) {
    final EdgeIterator edges = graph.createEdgeExplorer().setBaseNode(node);
    while (edges.next()) {
      if (edges.get(ring) && edges.get(access)) {
        return Optional.of(edges.detach(false));
      }
    }
    return Optional.empty();
  }

  /**
   * Finds where a route leaves a place by a road of a direction, after a maneuver: the route passes the place, and
   * its next stretch has the direction of the road.
   *
   * @param points the points of the route.
   * @param maneuver the index of the maneuver in the route.
   * @param place the place, a node of the graph.
   * @param direction the bearing of the first stretch of the road, from the place, in degrees.
   *
   * @return the index of the first point of the route after the maneuver, but the finish, that is the place, within
   *         {@value #SAME_NODE} m, the stretch from it to the next point having the direction of the road, within
   *         {@value #SAME_DIRECTION} degrees; or nothing if the route does not leave the place by that road.
   */
  static OptionalInt leaving(final List<GeoPoint> points, final int maneuver, final GeoPoint place,
                             final double direction) {
    for (int i = maneuver + 1; i < points.size() - 1; i++) {
      final GeoPoint point = points.get(i);
      if (DistanceCalcEarth.DIST_EARTH.calcDist(point.latitude(), point.longitude(), place.latitude(),
                                                place.longitude()) <= SAME_NODE
          && difference(bearing(point, points.get(i + 1)), direction) <= SAME_DIRECTION) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * Finds the branch of the closest direction to a bearing, by the first stretch of each branch.
   *
   * @param branches the branches, at least one.
   * @param bearing the bearing, in degrees.
   *
   * @return the index of the branch.
   */
  private static int closest(final List<Branch> branches, final double bearing) {
    int closest = 0;
    for (int i = 1; i < branches.size(); i++) {
      if (difference(branches.get(i).first(), bearing) < difference(branches.get(closest).first(), bearing)) {
        closest = i;
      }
    }
    return closest;
  }

  /**
   * Computes the difference between two bearings, the shorter way round.
   *
   * @param first the first bearing, in degrees from 0 to 360.
   * @param second the second bearing, in degrees from 0 to 360.
   *
   * @return the difference, in degrees from 0 to {@value #HALF_TURN}.
   */
  static double difference(final double first, final double second) {
    final double difference = Math.abs(first - second) % FULL_TURN;
    return Math.min(difference, FULL_TURN - difference);
  }

  /**
   * Computes the centre of a ring: the mean of its points, which are a few dozen metres apart.
   *
   * @param shape the points of the ring, at least one.
   *
   * @return the centre.
   */
  static GeoPoint centre(final List<GeoPoint> shape) {
    double latitude = 0;
    double longitude = 0;
    for (final GeoPoint point : shape) {
      latitude += point.latitude();
      longitude += point.longitude();
    }
    return new GeoPoint(latitude / shape.size(), longitude / shape.size());
  }

  /**
   * Computes the signed area of a ring, by the formula of the shoelace, the longitudes shrunk by the cosine of the
   * latitude so that the degrees of both axes have the same length.
   *
   * @param shape the points of the ring, in the order they are driven, the last one joined to the first.
   *
   * @return the area, in square degrees of latitude: positive if the ring is driven counterclockwise, seen from above,
   *         negative if it is driven clockwise.
   */
  static double area(final List<GeoPoint> shape) {
    final double shrink = Math.cos(Math.toRadians(shape.getFirst().latitude()));
    double area = 0;
    for (int i = 0; i < shape.size(); i++) {
      final GeoPoint from = shape.get(i);
      final GeoPoint to = shape.get((i + 1) % shape.size());
      area += from.longitude() * shrink * to.latitude() - to.longitude() * shrink * from.latitude();
    }
    return area / 2;
  }

  /**
   * Computes the bearing from a point of a line to another.
   *
   * @param line the line.
   * @param point the index of the point aimed at, from the first point of the line.
   *
   * @return the bearing, in degrees clockwise from the geographic north, from 0 included to 360 excluded.
   */
  private static double bearing(final PointList line, final int point) {
    return AngleCalc.ANGLE_CALC.calcAzimuth(line.getLat(0), line.getLon(0), line.getLat(point), line.getLon(point));
  }

  /**
   * Computes the bearing from a point to another.
   *
   * @param from the point of departure.
   * @param to the point aimed at.
   *
   * @return the bearing, in degrees clockwise from the geographic north, from 0 included to 360 excluded.
   */
  static double bearing(final GeoPoint from, final GeoPoint to) {
    return AngleCalc.ANGLE_CALC.calcAzimuth(from.latitude(), from.longitude(), to.latitude(), to.longitude());
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * A branch of a node of the graph.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param edge the edge of the branch in the graph.
   * @param end the node at the other end of the edge.
   * @param first the bearing of the first stretch of the edge, from the node, in degrees: the direction the route
   *              takes on it.
   * @param bearing the bearing of the branch, read {@value #BRANCH_DISTANCE} metres along it, in degrees: the
   *                direction the tulip draws.
   * @param kind the kind of the road of the branch.
   * @param roundabout whether the edge is part of the ring of a roundabout.
   * @param name the name of the road, empty if it has none.
   * @param in whether a car may drive the branch towards the node.
   * @param out whether a car may drive the branch away from the node.
   */
  private record Branch(int edge, int end, double first, double bearing, BranchKind kind, boolean roundabout,
                        String name, boolean in, boolean out) {
  }

  /**
   * A road that meets the ring of a roundabout: a leg of the roundabout.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param position the bearing, from the centre of the ring, of the place where the road meets the ring, in degrees.
   * @param direction the bearing of the road, read {@value #BRANCH_DISTANCE} metres along it from the ring, in degrees.
   * @param end the node of the graph at the other end of the edge of the road, away from the ring.
   * @param kind the kind of the road.
   * @param name the name of the road, empty if it has none.
   * @param in whether a car may drive the road into the ring.
   * @param out whether a car may drive the road out of the ring.
   * @param entry whether the route comes from this road.
   * @param exit whether the route leaves the ring by this road.
   */
  record Leg(double position, double direction, int end, BranchKind kind, String name, boolean in, boolean out,
             boolean entry, boolean exit) {

    /**
     * Tells whether the road is driven one way only, into the ring or out of it.
     *
     * @return {@code true} if a car may drive it one way and not the other.
     */
    boolean oneWay() {
      return in != out;
    }

  }

  /**
   * The ring of a roundabout, driven from the node where a route enters it.
   *
   * <p>Immutable once built, its lists not shared.</p>
   *
   * @param nodes the nodes of the ring, in the order they are driven.
   * @param shape the points of the ring, its nodes and the points of the edges between them, in the same order.
   */
  private record Ring(List<Integer> nodes, List<GeoPoint> shape) {
  }

}
