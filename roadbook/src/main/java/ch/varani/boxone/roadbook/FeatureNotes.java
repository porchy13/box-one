package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadFeature;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Notes the features of the map along a route in its roadbook (development plan, phase 5, increment 3b; decisions of
 * the maintainer, 2026-11-15: the level crossings, the barriers and the tarmac turning into a track noted from the map,
 * changed and removed as the other notes; 2026-11-18: in the box near them, or in a box of their own).
 *
 * <p><b>Algorithm</b>:</p>
 * <ol>
 *   <li>{@link #route(Route, List, List)}: a feature farther than {@value #NEAR_BOX} m along the route from every box
 *       of a junction (the boxes of the maneuvers, those of the features before it included; the start and the finish,
 *       which have no tulip to draw a symbol of the road in, excepted) becomes a maneuver of its own, unless the user
 *       removed a box within {@value RoadbookEditor#SAME_PLACE} m of it; a feature at the start or at the finish has no
 *       box of its own;</li>
 *   <li>{@link #noted(Roadbook, List)}: each feature is noted in the box of a junction nearest to it along the route,
 *       within {@value #NEAR_BOX} m: a level crossing as the rail road, a gate as the fence gate, a cattle grid as the
 *       cattle gate, a tunnel as the tunnel, a bridge as over a bridge and a ford as wading, symbols of the road at
 *       the entry of the box if the feature is before it, at its exit otherwise ({@link #symbol(FeatureKind)}); the
 *       tarmac turning into a track as the abbreviation of the track
 *       ({@link Abbreviation#TRACK}), once, and a danger of level 1 if the box has none ({@link #TRACK_DANGER}). A
 *       note for which the box has no room left ({@link Notes#MAX_SYMBOLS} on a side,
 *       {@link Notes#MAX_ABBREVIATIONS}) is left out.</li>
 * </ol>
 *
 * <p>The notes so placed are those of the box generated: the user changes or removes them in the editor of the notes
 * as the others, and "Reset the box" brings them back.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class FeatureNotes {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /**
   * The largest distance along the route between a feature and the box it is noted in, in metres; farther from every
   * box, the feature has a box of its own (decision of the maintainer, 2026-11-18): the distance of the trip meter
   * stays right within the precision a rider reads it with.
   */
  public static final double NEAR_BOX = 50;
  /**
   * The danger of the box where the route leaves the tarmac for a track (decision of the maintainer, 2026-11-18): a
   * danger of level 1, unless the box has one already.
   */
  public static final Danger TRACK_DANGER = Danger.LEVEL_1;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private FeatureNotes() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Adds a maneuver at each feature of a route far from the boxes of its junctions, so that it has a box of its own.
   *
   * @param route the route, with the boxes added and removed by the user ({@link RoadbookEditor#route}).
   * @param features the features of the map along the route, in the order of the route.
   * @param removed the places of the boxes removed by the user: no box is added there.
   *
   * @return the route with a maneuver at each feature farther than {@value #NEAR_BOX} m from the boxes of its
   *         junctions; the same points, distance and duration.
   */
  public static Route route(final Route route, final List<RoadFeature> features, final List<GeoPoint> removed) {
    final List<GeoPoint> points = route.points();
    final double[] along = RoadbookGenerator.along(points);
    final TreeSet<Integer> maneuvers = new TreeSet<>(route.maneuvers());
    for (final RoadFeature feature : features) {
      final int index = feature.index();
      final boolean inside = index > 0 && index < points.size() - 1;
      if (inside && !isRemoved(removed, points.get(index)) && nearest(maneuvers, along, index).isEmpty()) {
        maneuvers.add(index);
      }
    }
    return new Route(points, List.copyOf(maneuvers), route.distance(), route.duration());
  }

  /**
   * Notes the features of a route in the boxes of its junctions near them.
   *
   * @param roadbook the roadbook generated from the route given by {@link #route(Route, List, List)}: a box at the
   *                 start, one at each maneuver, one at the finish.
   * @param features the features of the map along the route, in the order of the route.
   *
   * @return the roadbook, the boxes near the features with their notes.
   */
  public static Roadbook noted(final Roadbook roadbook, final List<RoadFeature> features) {
    final Route route = roadbook.route();
    final double[] along = RoadbookGenerator.along(route.points());
    final TreeSet<Integer> maneuvers = new TreeSet<>(route.maneuvers());
    final List<Box> boxes = new ArrayList<>(roadbook.boxes());
    for (final RoadFeature feature : features) {
      final Optional<Integer> place = nearest(maneuvers, along, feature.index());
      if (place.isPresent()) {
        final int rank = route.maneuvers().indexOf(place.get()) + 1;
        final Box box = boxes.get(rank);
        final SymbolPlace side = feature.index() < place.get() ? SymbolPlace.ENTRY : SymbolPlace.EXIT;
        boxes.set(rank, new Box(box.number(), box.position(), box.total(), box.partial(), box.heading(), box.tulip(),
                                box.display(), note(box.notes(), feature.kind(), side), box.waypoint()));
      }
    }
    return new Roadbook(route, boxes);
  }

  /**
   * Finds the box of a junction nearest to a point of a route, within {@value #NEAR_BOX} m along it.
   *
   * @param maneuvers the indices of the points of the boxes of the junctions.
   * @param along the distance of each point of the line from the start ({@link RoadbookGenerator#along(List)}).
   * @param index the index of the point.
   *
   * @return the index of the point of the nearest box, the one before in a tie, or empty if none is within
   *         {@value #NEAR_BOX} m.
   */
  static Optional<Integer> nearest(final TreeSet<Integer> maneuvers, final double[] along, final int index) {
    final Integer before = maneuvers.floor(index);
    final Integer after = maneuvers.ceiling(index);
    final double back = before == null ? Double.POSITIVE_INFINITY : along[index] - along[before];
    final double ahead = after == null ? Double.POSITIVE_INFINITY : along[after] - along[index];
    if (Math.min(back, ahead) > NEAR_BOX) {
      return Optional.empty();
    }
    return Optional.of(back <= ahead ? before : after);
  }

  /**
   * Tells whether the user removed the box at a point of a route.
   *
   * @param removed the places of the boxes removed by the user.
   * @param point the point.
   *
   * @return {@code true} if a place removed is within {@value RoadbookEditor#SAME_PLACE} m of the point.
   */
  private static boolean isRemoved(final List<GeoPoint> removed, final GeoPoint point) {
    return removed.stream().anyMatch(place -> Geodesy.distance(place, point) <= RoadbookEditor.SAME_PLACE);
  }

  /**
   * Adds the note of a feature to the notes of a box, if the box has room for it.
   *
   * @param notes the notes of the box.
   * @param kind the kind of the feature.
   * @param side where the feature is: before the box ({@link SymbolPlace#ENTRY}) or after it
   *             ({@link SymbolPlace#EXIT}).
   *
   * @return the notes with the symbol of the feature on its side, or the abbreviation of the track and a danger of
   *         {@link #TRACK_DANGER} if the box has none; the same notes if the box has no room left for the symbol,
   *         without the abbreviation if it has no room for it or already notes the track.
   */
  static Notes note(final Notes notes, final FeatureKind kind, final SymbolPlace side) {
    final Optional<Symbol> symbol = symbol(kind);
    if (symbol.isEmpty()) {
      final Notes warned = notes.danger().isPresent() ? notes : notes.withDanger(Optional.of(TRACK_DANGER));
      final List<Abbreviation> abbreviations = warned.abbreviations();
      if (abbreviations.contains(Abbreviation.TRACK) || abbreviations.size() >= Notes.MAX_ABBREVIATIONS) {
        return warned;
      }
      return warned.withAbbreviations(Stream.concat(abbreviations.stream(), Stream.of(Abbreviation.TRACK)).toList());
    }
    if (notes.symbols().stream().filter(placed -> placed.place() == side).count() >= Notes.MAX_SYMBOLS) {
      return notes;
    }
    return notes.withSymbols(Stream.concat(notes.symbols().stream(), Stream.of(new BoxSymbol(symbol.get(), side)))
                                   .toList());
  }

  /**
   * Gives the symbol of the road that notes a feature (FIA, 2026 Cross-Country Rally Sporting Regulations, Appendix
   * III, §5.14).
   *
   * @param kind the kind of the feature.
   *
   * @return the rail road for a level crossing, the fence gate for a gate, the cattle gate for a cattle grid, the
   *         tunnel for a tunnel, over a bridge for a bridge, wading for a ford; empty for the tarmac turning into a
   *         track, noted by an abbreviation.
   */
  static Optional<Symbol> symbol(final FeatureKind kind) {
    return switch (kind) {
      case LEVEL_CROSSING -> Optional.of(Symbol.RAIL_ROAD);
      case GATE -> Optional.of(Symbol.FENCE_GATE);
      case CATTLE_GRID -> Optional.of(Symbol.CATTLE_GATE);
      case TUNNEL -> Optional.of(Symbol.TUNNEL);
      case BRIDGE -> Optional.of(Symbol.ABOVE_BRIDGE);
      case FORD -> Optional.of(Symbol.WADING);
      case TRACK -> Optional.empty();
    };
  }

}
