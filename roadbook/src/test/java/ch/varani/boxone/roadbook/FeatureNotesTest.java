package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.RoadFeature;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests of {@link FeatureNotes} (development plan, phase 5, increment 3b): the features of the map noted in the box of
 * a junction within 50 m, or in a box of their own.
 *
 * <p>The route of the tests goes north from the station of Yverdon-les-Bains, a point every ten thousandth of a
 * degree of latitude (about 11 m), {@value #POINTS} points; its maneuver at the point {@value #MANEUVER}.</p>
 */
final class FeatureNotesTest {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The number of points of the route. */
  private static final int POINTS = 41;
  /** The latitude of the start. */
  private static final double LATITUDE = 46.78;
  /** The longitude of the route. */
  private static final double LONGITUDE = 6.64;
  /** The latitude between two points, in degrees: about 11 m. */
  private static final double STEP = 0.0001;
  /** The point of the maneuver of the route. */
  private static final int MANEUVER = 20;
  /** A level crossing two points (22 m) before the maneuver. */
  private static final int BEFORE = 18;
  /** A gate one point before the maneuver. */
  private static final int JUST_BEFORE = 19;
  /** A gate three points (33 m) after the maneuver. */
  private static final int AFTER = 23;
  /** A cattle grid ten points (111 m) after the maneuver, and ten points before the finish: a box of its own. */
  private static final int FAR = 30;
  /** The tarmac turning into a track, two points after the cattle grid. */
  private static final int TRACK = 32;
  /** A gate two points after the start, far from the maneuver: a box of its own. */
  private static final int NEAR_START = 2;
  /** A gate four points before the finish, its box removed by the user. */
  private static final int REMOVED = 36;
  /** The box of the cattle grid, a box of its own: the fourth. */
  private static final int FAR_BOX = 3;
  /** The distances of the points of a line, in metres, for the nearest box. */
  private static final double[] ALONG = {0, 10, 20, 30, 40, 100, 200};
  /** The point of the first box of {@link #ALONG}. */
  private static final int FIRST_BOX = 1;
  /** The point of the second box of {@link #ALONG}. */
  private static final int SECOND_BOX = 3;
  /** The point of {@link #ALONG} as far from both boxes. */
  private static final int TIE = 2;
  /** The point of {@link #ALONG} just after the second box. */
  private static final int AFTER_SECOND = 4;
  /** The length of the route, in metres. */
  private static final double LENGTH = 445;

  // *******************************************************************************************************************
  // Tests
  // *******************************************************************************************************************

  /**
   * The features far from every box of a junction have a box of their own, but where the user removed one and at
   * the start; those near a box are noted in it, before it at its entry, after it at its exit, the track once, a
   * symbol for which a side has no room left out.
   */
  @Test
  @DisplayName("Phase 5: a feature is noted in the box of a junction within 50 m, or in a box of its own")
  void notedTest() {
    final Route route = route();
    final List<RoadFeature> features = List.of(
        new RoadFeature(0, FeatureKind.LEVEL_CROSSING), new RoadFeature(NEAR_START, FeatureKind.GATE),
        new RoadFeature(BEFORE, FeatureKind.LEVEL_CROSSING), new RoadFeature(JUST_BEFORE, FeatureKind.GATE),
        new RoadFeature(JUST_BEFORE, FeatureKind.CATTLE_GRID), new RoadFeature(MANEUVER, FeatureKind.TRACK),
        new RoadFeature(AFTER, FeatureKind.GATE), new RoadFeature(FAR, FeatureKind.CATTLE_GRID),
        new RoadFeature(TRACK, FeatureKind.TRACK), new RoadFeature(TRACK + 1, FeatureKind.TRACK),
        new RoadFeature(REMOVED, FeatureKind.GATE), new RoadFeature(POINTS - 1, FeatureKind.GATE));
    final Route boxed = FeatureNotes.route(route, features, List.of(route.points().get(REMOVED)));
    final List<Box> boxes = FeatureNotes.noted(RoadbookGenerator.generate(boxed), features).boxes();
    assertAll(
        () -> assertEquals(List.of(NEAR_START, MANEUVER, FAR), boxed.maneuvers()),
        () -> assertEquals(route.points(), boxed.points()),
        () -> assertEquals(Notes.NONE, boxes.get(0).notes()),
        () -> assertEquals(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.ENTRY),
                                   new BoxSymbol(Symbol.FENCE_GATE, SymbolPlace.EXIT)),
                           boxes.get(1).notes().symbols()),
        () -> assertEquals(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.ENTRY),
                                   new BoxSymbol(Symbol.FENCE_GATE, SymbolPlace.ENTRY),
                                   new BoxSymbol(Symbol.FENCE_GATE, SymbolPlace.EXIT)),
                           boxes.get(2).notes().symbols()),
        () -> assertEquals(List.of(Abbreviation.TRACK), boxes.get(2).notes().abbreviations()),
        () -> assertEquals(Optional.of(Danger.LEVEL_1), boxes.get(2).notes().danger()),
        () -> assertEquals(Optional.empty(), boxes.get(1).notes().danger()),
        () -> assertEquals(List.of(new BoxSymbol(Symbol.CATTLE_GATE, SymbolPlace.EXIT)),
                           boxes.get(FAR_BOX).notes().symbols()),
        () -> assertEquals(List.of(Abbreviation.TRACK), boxes.get(FAR_BOX).notes().abbreviations()),
        () -> assertEquals(Notes.NONE, boxes.getLast().notes()));
  }

  /**
   * A route without features keeps its maneuvers, and its roadbook its notes.
   */
  @Test
  @DisplayName("Phase 5: a route without features of the map has no box nor note added")
  void noneTest() {
    final Route route = route();
    final Roadbook roadbook = RoadbookGenerator.generate(route);
    assertAll(
        () -> assertEquals(route, FeatureNotes.route(route, List.of(), List.of())),
        () -> assertEquals(roadbook, FeatureNotes.noted(roadbook, List.of())));
  }

  /**
   * The nearest box of a junction is the one before in a tie; none is near beyond 50 m on both sides.
   */
  @Test
  @DisplayName("Phase 5: the box nearest to a feature is the one before it in a tie, none beyond 50 m")
  void nearestTest() {
    final TreeSet<Integer> maneuvers = new TreeSet<>(List.of(FIRST_BOX, SECOND_BOX));
    assertAll(
        () -> assertEquals(Optional.of(FIRST_BOX), FeatureNotes.nearest(maneuvers, ALONG, TIE)),
        () -> assertEquals(Optional.of(SECOND_BOX), FeatureNotes.nearest(maneuvers, ALONG, AFTER_SECOND)),
        () -> assertEquals(Optional.of(FIRST_BOX), FeatureNotes.nearest(maneuvers, ALONG, 0)),
        () -> assertEquals(Optional.empty(), FeatureNotes.nearest(maneuvers, ALONG, ALONG.length - 1)),
        () -> assertEquals(Optional.empty(), FeatureNotes.nearest(new TreeSet<>(), ALONG, TIE)));
  }

  /**
   * The track is not noted in a box that has all its abbreviations, nor twice, but its danger of level 1 is, unless
   * the box has a danger already; each feature has its symbol of the
   * road, the track none.
   */
  @Test
  @DisplayName("Phase 5: a level crossing is the rail road, a gate the fence gate, a cattle grid the cattle gate, "
               + "a tunnel the tunnel, a bridge over a bridge, a ford wading, the track an abbreviation")
  void noteTest() {
    final Notes full = Notes.NONE.withAbbreviations(List.of(Abbreviation.ROAD, Abbreviation.SANDY,
                                                            Abbreviation.GRAVEL, Abbreviation.STONY,
                                                            Abbreviation.NARROW, Abbreviation.BAD));
    final Notes dangerous = full.withDanger(Optional.of(Danger.LEVEL_3));
    assertAll(
        () -> assertEquals(full.withDanger(Optional.of(Danger.LEVEL_1)),
                           FeatureNotes.note(full, FeatureKind.TRACK, SymbolPlace.EXIT)),
        () -> assertSame(dangerous, FeatureNotes.note(dangerous, FeatureKind.TRACK, SymbolPlace.EXIT)),
        () -> assertEquals(Optional.of(Symbol.RAIL_ROAD), FeatureNotes.symbol(FeatureKind.LEVEL_CROSSING)),
        () -> assertEquals(Optional.of(Symbol.FENCE_GATE), FeatureNotes.symbol(FeatureKind.GATE)),
        () -> assertEquals(Optional.of(Symbol.CATTLE_GATE), FeatureNotes.symbol(FeatureKind.CATTLE_GRID)),
        () -> assertEquals(Optional.of(Symbol.TUNNEL), FeatureNotes.symbol(FeatureKind.TUNNEL)),
        () -> assertEquals(Optional.of(Symbol.ABOVE_BRIDGE), FeatureNotes.symbol(FeatureKind.BRIDGE)),
        () -> assertEquals(Optional.of(Symbol.WADING), FeatureNotes.symbol(FeatureKind.FORD)),
        () -> assertEquals(Optional.empty(), FeatureNotes.symbol(FeatureKind.TRACK)));
  }

  // *******************************************************************************************************************
  // Helpers
  // *******************************************************************************************************************

  /**
   * Builds the route of the tests.
   *
   * @return the route north from the station, its maneuver at the point {@value #MANEUVER}.
   */
  private static Route route() {
    final List<GeoPoint> points = IntStream.range(0, POINTS)
                                           .mapToObj(i -> new GeoPoint(LATITUDE + i * STEP, LONGITUDE)).toList();
    return new Route(points, List.of(MANEUVER), LENGTH, Duration.ofMinutes(1));
  }

}
