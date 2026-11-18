package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Applies the changes made by the user in the editor of the roadbook (development plan, phase 5) to a route and to
 * its roadbook, and keeps those that still apply when the route changes (decision of the maintainer, 2026-11-14: the
 * changes are kept by their places).
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>{@link #route(Route, RoadbookEdits)}: the boxes removed are the maneuvers within {@value #SAME_PLACE} m of a
 *       place removed, the boxes of the start and of the finish excepted; then each box added becomes a maneuver at
 *       the nearest point of the line ({@link #project(Route, GeoPoint)}): the point of the line within
 *       {@value #SAME_PLACE} m, or a new point of the line inserted there. The roadbook of the route so changed is then
 *       generated as any other ({@link RoadbookGenerator}), the junction of a box added read where it is, as for the
 *       maneuvers of the routing engine;</li>
 *   <li>{@link #roadbook(Roadbook, RoadbookEdits)}: each box corrected, the correction within {@value #SAME_PLACE} m
 *       of its place, takes the heading typed, its display, its tulip corrected if the box has the tulip the
 *       correction was made on ({@link TulipEdit}: the route still crosses the junction the same way), and its total
 *       distance shifted, unless the shift puts it
 *       before the total of the box before or after the total computed of the box after (the boxes follow each other
 *       along the route): the shift is then ignored. The partial distances are computed again from the totals;</li>
 *   <li>{@link #retain(RoadbookEdits, Route)}: on a new route, a box added is kept if its place is within
 *       {@value #NEAR_ROUTE} m of the line, at the nearest point of it; a box removed if the line passes within
 *       {@value #SAME_PLACE} m of its place; a correction likewise, the line with the boxes added and removed kept
 *       (the box of a feature of the map, {@link FeatureNotes}, is not a maneuver of the route: increment 3b); a
 *       correction where the new roadbook has no box changes nothing. Each change kept is moved to its place on the
 *       new route, the others are dropped.</li>
 * </ol>
 * <p>The changes are idempotent: applied twice, or to a route that already holds them, they give the same roadbook.
 * A roadbook is saved with its route as generated and its changes beside it, so that a box added can still be moved
 * or removed once it is opened again; {@link #withoutAdded(Route, RoadbookEdits)} takes the boxes added out of the
 * route of a roadbook saved with them (before 2026-11-15).</p>
 *
 * <p>The places are personal data (AGENTS.md §5): nothing is logged.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class RoadbookEditor {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The distance within which two places are the same box, in metres: the places of the boxes are points of the route,
   * the same nodes of the map from one route to the next, and the coordinates of a roadbook read back are rounded to
   * about 0.1 m; the tolerance of the nodes of the junctions ({@code JunctionReader.SAME_NODE} of the routing).
   */
  public static final double SAME_PLACE = 1;
  /**
   * The largest distance from the line of a route at which a box is added, in metres: the place clicked on the map is
   * moved to the nearest point of the route, within the width of a road and the precision of a click at the zoom
   * where a junction can be read. Chosen for phase 5 (development plan), to be checked by the maintainer.
   */
  public static final double NEAR_ROUTE = 25;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RoadbookEditor() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Finds the point of the line of a route nearest to a place, if the place is close enough to add a box there.
   *
   * @param route the route.
   * @param place the place, e.g. clicked on the map.
   *
   * @return the nearest point of the line, between its points, or the point of the line within {@value #SAME_PLACE} m
   *         of it, if it is within {@value #NEAR_ROUTE} m of the place; empty otherwise. The box added there by
   *         {@link #route(Route, RoadbookEdits)} is at this point exactly, so that it is found again by its place.
   */
  public static Optional<GeoPoint> project(final Route route, final GeoPoint place) {
    final List<GeoPoint> points = route.points();
    final Projection nearest = nearest(points, place);
    if (Geodesy.distance(place, nearest.point()) > NEAR_ROUTE) {
      return Optional.empty();
    }
    return Optional.of(Stream.of(points.get(nearest.segment()), points.get(nearest.segment() + 1))
                           .filter(point -> Geodesy.distance(point, nearest.point()) <= SAME_PLACE)
                           .findFirst().orElse(nearest.point()));
  }

  /**
   * Applies the boxes removed and added to a route: its maneuvers change, and a point is inserted into its line where
   * a box is added between two of its points.
   *
   * @param route the route generated.
   * @param edits the changes of the user.
   *
   * @return the route with the maneuvers removed and added; the same distance and duration.
   */
  public static Route route(final Route route, final RoadbookEdits edits) {
    final List<GeoPoint> points = new ArrayList<>(route.points());
    final TreeSet<Integer> maneuvers = new TreeSet<>(route.maneuvers());
    maneuvers.removeIf(index -> edits.removed().stream()
                                     .anyMatch(place -> Geodesy.distance(place, points.get(index)) <= SAME_PLACE));
    for (final GeoPoint place : edits.added()) {
      final int index = insert(points, maneuvers, place);
      if (index > 0 && index < points.size() - 1) {
        maneuvers.add(index);
      }
    }
    return new Route(points, List.copyOf(maneuvers), route.distance(), route.duration());
  }

  /**
   * Takes the boxes added out of a route that holds them: the maneuvers within {@value #SAME_PLACE} m of a box added
   * are no longer maneuvers, so that {@link #route(Route, RoadbookEdits)} adds them again, and a box added that is
   * moved or removed leaves no maneuver behind. A roadbook saved before 2026-11-15 holds its boxes added in its route;
   * its boxes removed cannot be brought back, the maneuvers of the route as generated being lost.
   *
   * @param route the route of a roadbook read back.
   * @param edits its changes.
   *
   * @return the route without the maneuvers of the boxes added; the same points, distance and duration.
   */
  public static Route withoutAdded(final Route route, final RoadbookEdits edits) {
    final List<Integer> maneuvers = route.maneuvers().stream()
                                         .filter(index -> within(edits.added(), route.points().get(index)).isEmpty())
                                         .toList();
    return new Route(route.points(), maneuvers, route.distance(), route.duration());
  }

  /**
   * Applies the corrections of the boxes to the roadbook of a route changed by {@link #route(Route, RoadbookEdits)}:
   * their shifted totals, their headings, their displays, their tulips and their notes.
   *
   * @param roadbook the roadbook generated from the route changed.
   * @param edits the changes of the user.
   *
   * @return the roadbook with the boxes corrected, the partial distances computed again.
   */
  public static Roadbook roadbook(final Roadbook roadbook, final RoadbookEdits edits) {
    final List<Box> boxes = roadbook.boxes();
    final List<Box> corrected = new ArrayList<>(boxes.size());
    double previous = 0;
    for (int i = 0; i < boxes.size(); i++) {
      final Box box = boxes.get(i);
      final double next = i + 1 < boxes.size() ? boxes.get(i + 1).total() : Double.POSITIVE_INFINITY;
      final Optional<BoxEdit> edit = edit(edits, box.position());
      double total = box.total();
      OptionalInt heading = box.heading();
      HeadingDisplay display = box.display();
      Optional<Tulip> tulip = box.tulip();
      Notes notes = box.notes();
      if (edit.isPresent()) {
        final double shifted = total + edit.get().shift();
        if (shifted >= previous && shifted <= next) {
          total = shifted;
        }
        if (edit.get().heading().isPresent()) {
          heading = edit.get().heading();
        }
        display = edit.get().display();
        tulip = tulip(tulip, edit.get().tulip());
        notes = edit.get().notes().orElse(notes);
      }
      corrected.add(new Box(box.number(), box.position(), total, total - previous, heading, tulip, display, notes));
      previous = total;
    }
    return new Roadbook(roadbook.route(), corrected);
  }

  /**
   * Keeps the changes that still apply to a new route, each moved to its place on it.
   *
   * @param edits the changes of the user, made on another route.
   * @param route the new route, as generated.
   *
   * @return the boxes added near the line, at its nearest points; the boxes removed where the line passes; the
   *         corrections where the line passes, at their places.
   */
  public static RoadbookEdits retain(final RoadbookEdits edits, final Route route) {
    RoadbookEdits kept = RoadbookEdits.NONE;
    for (final GeoPoint place : edits.removed()) {
      final Optional<GeoPoint> point = within(route.points(), place);
      if (point.isPresent()) {
        kept = kept.withRemoved(point.get());
      }
    }
    for (final GeoPoint place : edits.added()) {
      final Optional<GeoPoint> point = project(route, place);
      if (point.isPresent()) {
        kept = kept.withAdded(point.get());
      }
    }
    final List<GeoPoint> line = route(route, kept).points();
    for (final BoxEdit edit : edits.boxes()) {
      final Optional<GeoPoint> place = within(line, edit.place());
      if (place.isPresent() && kept.edit(place.get()).isEmpty()) {
        kept = kept.with(edit.at(place.get()));
      }
    }
    return kept;
  }

  /**
   * Gives the tulip of a box corrected: the tulip of the correction if the box has the tulip it was made on.
   *
   * @param generated the tulip of the box generated, or empty.
   * @param edit the correction of the tulip, or empty.
   *
   * @return the tulip corrected, or the tulip generated if there is no correction or if it was made on another tulip.
   */
  private static Optional<Tulip> tulip(final Optional<Tulip> generated, final Optional<TulipEdit> edit) {
    if (edit.isPresent() && generated.equals(Optional.of(edit.get().generated()))) {
      return Optional.of(edit.get().tulip());
    }
    return generated;
  }

  /**
   * Finds the correction of the box at a place.
   *
   * @param edits the changes of the user.
   * @param place the place of the box.
   *
   * @return the first correction within {@value #SAME_PLACE} m of the place, or empty.
   */
  private static Optional<BoxEdit> edit(final RoadbookEdits edits, final GeoPoint place) {
    return edits.boxes().stream().filter(edit -> Geodesy.distance(edit.place(), place) <= SAME_PLACE).findFirst();
  }

  /**
   * Finds the first of some places within {@value #SAME_PLACE} m of a place.
   *
   * @param places the places, in their order.
   * @param place the place looked for.
   *
   * @return the first place within {@value #SAME_PLACE} m, or empty if none is.
   */
  private static Optional<GeoPoint> within(final List<GeoPoint> places, final GeoPoint place) {
    return places.stream().filter(point -> Geodesy.distance(point, place) <= SAME_PLACE).findFirst();
  }

  /**
   * Finds the index of the point of a line where a box is added, inserting a point into the line if the nearest point
   * is not within {@value #SAME_PLACE} m of one of its points; the maneuvers after the point inserted move by one. The
   * point inserted is the place itself when it is on the line (within {@value #SAME_PLACE} m of the nearest point, as
   * given by {@link #project(Route, GeoPoint)}), so that the box is at its place exactly; the nearest point otherwise.
   *
   * @param points the points of the line, changed in place.
   * @param maneuvers the indices of the maneuvers, changed in place.
   * @param place the place of the box added.
   *
   * @return the index of the point of the box.
   */
  private static int insert(final List<GeoPoint> points, final TreeSet<Integer> maneuvers, final GeoPoint place) {
    final Projection nearest = nearest(points, place);
    final int segment = nearest.segment();
    if (Geodesy.distance(nearest.point(), points.get(segment)) <= SAME_PLACE) {
      return segment;
    }
    if (Geodesy.distance(nearest.point(), points.get(segment + 1)) <= SAME_PLACE) {
      return segment + 1;
    }
    final List<Integer> after = List.copyOf(maneuvers.tailSet(segment, false));
    maneuvers.removeAll(after);
    after.forEach(index -> maneuvers.add(index + 1));
    points.add(segment + 1, Geodesy.distance(place, nearest.point()) <= SAME_PLACE ? place : nearest.point());
    return segment + 1;
  }

  /**
   * Finds the point of a line nearest to a place: on each segment, the place is projected in a plane tangent to the
   * Earth at the start of the segment (the segments of a route are short), and the nearest projection is kept.
   *
   * @param points the points of the line, at least two.
   * @param place the place.
   *
   * @return the nearest point and the index of the first point of its segment; the first segment in a tie.
   */
  static Projection nearest(final List<GeoPoint> points, final GeoPoint place) {
    Projection best = null;
    double shortest = Double.POSITIVE_INFINITY;
    for (int i = 0; i < points.size() - 1; i++) {
      final GeoPoint point = onSegment(points.get(i), points.get(i + 1), place);
      final double distance = Geodesy.distance(place, point);
      if (distance < shortest) {
        shortest = distance;
        best = new Projection(i, point);
      }
    }
    return best;
  }

  /**
   * Projects a place on a segment, in a plane tangent to the Earth at its start.
   *
   * @param from the start of the segment.
   * @param to the end of the segment.
   * @param place the place.
   *
   * @return the point of the segment nearest to the place: its start if the segment has no length.
   */
  static GeoPoint onSegment(final GeoPoint from, final GeoPoint to, final GeoPoint place) {
    final double scale = Math.cos(Math.toRadians(from.latitude()));
    final double x = (to.longitude() - from.longitude()) * scale;
    final double y = to.latitude() - from.latitude();
    final double length = x * x + y * y;
    if (length == 0) {
      return from;
    }
    final double dot = (place.longitude() - from.longitude()) * scale * x + (place.latitude() - from.latitude()) * y;
    final double ratio = Math.clamp(dot / length, 0, 1);
    return new GeoPoint(from.latitude() + ratio * y, from.longitude() + ratio * (to.longitude() - from.longitude()));
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The point of a line nearest to a place.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param segment the index of the first point of the segment of the line holding the point.
   * @param point the point.
   */
  record Projection(int segment, GeoPoint point) {
  }

}
