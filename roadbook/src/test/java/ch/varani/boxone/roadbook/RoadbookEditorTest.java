package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.TulipEdit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RoadbookEditor}: the boxes added at the nearest point of the line, a point inserted between two
 * points of it, the boxes removed, the corrections of the totals, the headings and their displays, a shift that would
 * put a box out of its order ignored, the tulips corrected where the junction gives the same tulip, the changes kept
 * on a new route where it still passes, and the changes applied twice giving the same roadbook.
 */
final class RoadbookEditorTest {

  /** A junction of Yverdon-les-Bains, the start of the line. */
  private static final GeoPoint START = new GeoPoint(46.7800, 6.6400);
  /** About 111 m north of the start, a turn to the right. */
  private static final GeoPoint NORTH = new GeoPoint(46.7810, 6.6400);
  /** About 76 m east of {@link #NORTH}. */
  private static final GeoPoint NORTH_EAST = new GeoPoint(46.7810, 6.6410);
  /** About 152 m east of {@link #NORTH_EAST}, the finish. */
  private static final GeoPoint FINISH = new GeoPoint(46.7810, 6.6430);
  /** About 76 m west of {@link #NORTH}, the finish of another route. */
  private static final GeoPoint NORTH_WEST = new GeoPoint(46.7810, 6.6390);
  /** Halfway from the start to {@link #NORTH}, on the line. */
  private static final GeoPoint HALFWAY = new GeoPoint(46.7805, 6.6400);
  /** About 7.6 m east of {@link #HALFWAY}: a click near the line. */
  private static final GeoPoint NEAR_HALFWAY = new GeoPoint(46.7805, 6.6401);
  /** About 76 m east of {@link #HALFWAY}: a click far from the line. */
  private static final GeoPoint FAR = new GeoPoint(46.7805, 6.6410);
  /** About 0.6 m south of {@link #NORTH}, on the line: the same place. */
  private static final GeoPoint ALMOST_NORTH = new GeoPoint(46.780_995, 6.6400);
  /** The duration of the routes, unused by the roadbook. */
  private static final Duration DURATION = Duration.ofMinutes(1);
  /** A shift of the total distance, in metres. */
  private static final double SHIFT = 10;
  /** A shift that puts a box beyond the next one, in metres. */
  private static final double TOO_FAR = 500;
  /** A heading typed, in degrees: north-east. */
  private static final OptionalInt NORTH_EASTWARDS = OptionalInt.of(45);
  /** The number of places clicked along a segment, less one. */
  private static final int CLICKS = 20;
  /** The offset of the places clicked from the line, in degrees of latitude: about 3 m. */
  private static final double OFF_LINE = 0.000_03;
  /** The tulip of the turn to the right at {@link #NORTH}: straight ahead, or to the right. */
  private static final Tulip TURN = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0, 90),
                                              List.of(BranchKind.TARMAC, BranchKind.TARMAC), 1);
  /** The tolerance of the distances, in metres. */
  private static final double TOLERANCE = 1e-6;

  /**
   * The route north, a turn to the right at {@link #NORTH}, then east.
   *
   * @return the route.
   */
  private static Route route() {
    return route(List.of(START, NORTH, NORTH_EAST, FINISH), List.of(1));
  }

  /**
   * Creates a route on a line, with its maneuvers; its length is the length of the line.
   *
   * @param points the points of the line.
   * @param maneuvers the indices of the points of the maneuvers.
   *
   * @return the route.
   */
  private static Route route(final List<GeoPoint> points, final List<Integer> maneuvers) {
    return new Route(points, maneuvers, RoadbookGenerator.along(points)[points.size() - 1], DURATION);
  }

  /**
   * A place near the line is moved to its nearest point; a place far from it is not.
   */
  @Test
  @DisplayName("Phase 5: a box is added at the nearest point of the line, within 25 m of it only")
  void projectTest() {
    final Optional<GeoPoint> near = RoadbookEditor.project(route(), NEAR_HALFWAY);
    assertAll(
        () -> assertEquals(HALFWAY.latitude(), near.orElseThrow().latitude(), TOLERANCE),
        () -> assertEquals(HALFWAY.longitude(), near.orElseThrow().longitude(), TOLERANCE),
        () -> assertEquals(Optional.empty(), RoadbookEditor.project(route(), FAR)),
        () -> assertEquals(Optional.of(NORTH), RoadbookEditor.project(route(), ALMOST_NORTH)));
  }

  /**
   * A box added at a place projected on a slanting segment, whose nearest point computed again differs by a rounding,
   * is at that place exactly in the roadbook, so that it is selected and removed by its place; a place off the line
   * (in a file) inserts its nearest point.
   */
  @Test
  @DisplayName("Phase 5: a box added is at its place exactly, to be selected and removed again")
  void addedPlaceTest() {
    final Route slanting = route(List.of(START, NORTH_EAST, FINISH), List.of());
    final List<GeoPoint> places = IntStream.range(1, CLICKS).mapToObj(i -> RoadbookEditor.project(slanting,
        new GeoPoint(START.latitude() + (NORTH_EAST.latitude() - START.latitude()) * i / CLICKS + OFF_LINE,
                     START.longitude() + (NORTH_EAST.longitude() - START.longitude()) * i / CLICKS)).orElseThrow())
        .toList();
    final RoadbookEdits added = places.stream().reduce(RoadbookEdits.NONE, RoadbookEdits::withAdded, (a, b) -> b);
    final Roadbook roadbook = RoadbookGenerator.generate(RoadbookEditor.route(slanting, added));
    final RoadbookEdits removed = places.stream().reduce(added, RoadbookEdits::withRemoved, (a, b) -> b);
    final Route offLine = RoadbookEditor.route(route(), RoadbookEdits.NONE.withAdded(NEAR_HALFWAY));
    assertAll(
        () -> assertTrue(roadbook.boxes().stream().map(Box::position).toList().containsAll(places)),
        () -> assertEquals(slanting, RoadbookEditor.route(slanting, removed)),
        () -> assertEquals(HALFWAY.longitude(), offLine.points().get(1).longitude(), TOLERANCE));
  }

  /**
   * A box added between two points of the line inserts a point there, the maneuvers after it moving by one; a box added
   * at a point of the line, or within 1 m of it, makes it a maneuver; a box added at the start or at the finish
   * changes nothing.
   */
  @Test
  @DisplayName("Phase 5: a box added becomes a maneuver, at a point of the line or at a point inserted into it")
  void addedTest() {
    final Route between = RoadbookEditor.route(route(), RoadbookEdits.NONE.withAdded(HALFWAY));
    final Route atPoint = RoadbookEditor.route(route(), RoadbookEdits.NONE.withAdded(NORTH_EAST));
    final Route nearPoint = RoadbookEditor.route(route(List.of(START, NORTH, FINISH), List.of()),
                                                 RoadbookEdits.NONE.withAdded(ALMOST_NORTH));
    final Route ends = RoadbookEditor.route(route(), RoadbookEdits.NONE.withAdded(START).withAdded(FINISH));
    assertAll(
        () -> assertEquals(List.of(START, HALFWAY, NORTH, NORTH_EAST, FINISH), between.points()),
        () -> assertEquals(List.of(1, 2), between.maneuvers()),
        () -> assertEquals(route().distance(), between.distance()),
        () -> assertEquals(DURATION, between.duration()),
        () -> assertEquals(List.of(1, 2), atPoint.maneuvers()),
        () -> assertEquals(List.of(1), nearPoint.maneuvers()),
        () -> assertEquals(List.of(START, NORTH, FINISH), nearPoint.points()),
        () -> assertEquals(route(), ends));
  }

  /**
   * The boxes added are taken out of a route that holds them, within 1 m, the other maneuvers kept; the changes
   * applied again give the same route.
   */
  @Test
  @DisplayName("Phase 5: the boxes added are taken out of a route saved with them, and added again")
  void withoutAddedTest() {
    final RoadbookEdits added = RoadbookEdits.NONE.withAdded(HALFWAY).withAdded(ALMOST_NORTH);
    final Route holding = RoadbookEditor.route(route(), added);
    final Route without = RoadbookEditor.withoutAdded(holding, RoadbookEdits.NONE.withAdded(HALFWAY));
    assertAll(
        () -> assertEquals(List.of(2), without.maneuvers()),
        () -> assertEquals(holding.points(), without.points()),
        () -> assertEquals(List.of(), RoadbookEditor.withoutAdded(holding, added).maneuvers()),
        () -> assertEquals(holding, RoadbookEditor.route(RoadbookEditor.withoutAdded(holding, added), added)));
  }

  /**
   * A box removed is no longer a maneuver; the start and the finish stay.
   */
  @Test
  @DisplayName("Phase 5: a box removed is no longer a maneuver, the start and the finish stay")
  void removedTest() {
    final Route removed = RoadbookEditor.route(route(), RoadbookEdits.NONE.withRemoved(ALMOST_NORTH)
                                                                          .withRemoved(START));
    final Roadbook roadbook = RoadbookGenerator.generate(removed);
    assertAll(
        () -> assertEquals(List.of(), removed.maneuvers()),
        () -> assertEquals(2, roadbook.boxes().size()));
  }

  /**
   * A box corrected takes its shifted total, its heading typed and its display; the partial distances follow; a box
   * corrected without heading keeps the heading computed.
   */
  @Test
  @DisplayName("Phase 5: a box corrected takes its total shifted, its heading and its display, the partials follow")
  void correctedTest() {
    final Roadbook generated = RoadbookGenerator.generate(route());
    final RoadbookEdits edits = RoadbookEdits.NONE
        .with(new BoxEdit(NORTH).withShift(SHIFT).withHeading(NORTH_EASTWARDS).withDisplay(HeadingDisplay.SHOWN))
        .with(new BoxEdit(FINISH).withDisplay(HeadingDisplay.HIDDEN));
    final List<Box> boxes = RoadbookEditor.roadbook(generated, edits).boxes();
    final List<Box> before = generated.boxes();
    assertAll(
        () -> assertEquals(before.get(0), boxes.get(0)),
        () -> assertEquals(before.get(1).total() + SHIFT, boxes.get(1).total(), TOLERANCE),
        () -> assertEquals(before.get(1).partial() + SHIFT, boxes.get(1).partial(), TOLERANCE),
        () -> assertEquals(NORTH_EASTWARDS, boxes.get(1).heading()),
        () -> assertEquals(HeadingDisplay.SHOWN, boxes.get(1).display()),
        () -> assertEquals(before.get(2).total(), boxes.get(2).total(), TOLERANCE),
        () -> assertEquals(before.get(2).partial() - SHIFT, boxes.get(2).partial(), TOLERANCE),
        () -> assertEquals(OptionalInt.empty(), boxes.get(2).heading()),
        () -> assertEquals(HeadingDisplay.HIDDEN, boxes.get(2).display()));
  }

  /**
   * A box takes its tulip corrected if it has the tulip the correction was made on; otherwise, the route crossing the
   * junction another way, the tulip generated.
   */
  @Test
  @DisplayName("Phase 5: a box takes its tulip corrected while its junction gives the tulip it was made on")
  void tulipTest() {
    final List<Box> boxes = RoadbookGenerator.generate(route()).boxes();
    final Box turn = boxes.get(1);
    final Roadbook generated = new Roadbook(route(), List.of(boxes.get(0), new Box(
        turn.number(), turn.position(), turn.total(), turn.partial(), turn.heading(), Optional.of(TURN)),
        boxes.get(2)));
    final Tulip track = TURN.withKind(0, BranchKind.TRACK);
    final Tulip otherWay = TURN.withExit(0);
    final Optional<TulipEdit> corrected = Optional.of(new TulipEdit(TURN, track));
    final Optional<TulipEdit> elsewhere = Optional.of(new TulipEdit(otherWay, otherWay.withKind(0, BranchKind.TRACK)));
    assertAll(
        () -> assertEquals(Optional.of(track), RoadbookEditor.roadbook(
            generated, RoadbookEdits.NONE.with(new BoxEdit(NORTH).withTulip(corrected))).boxes().get(1).tulip()),
        () -> assertEquals(Optional.of(TURN), RoadbookEditor.roadbook(
            generated, RoadbookEdits.NONE.with(new BoxEdit(NORTH).withTulip(elsewhere))).boxes().get(1).tulip()),
        () -> assertEquals(Optional.of(TURN), RoadbookEditor.roadbook(
            generated, RoadbookEdits.NONE.with(new BoxEdit(NORTH).withShift(SHIFT))).boxes().get(1).tulip()),
        () -> assertEquals(Optional.empty(), RoadbookEditor.roadbook(
            generated, RoadbookEdits.NONE.with(new BoxEdit(START).withTulip(corrected))).boxes().get(0).tulip()));
  }

  /**
   * A box corrected takes the notes of its correction; the other boxes keep none.
   */
  @Test
  @DisplayName("Phase 5: a box takes the notes of its correction, the other boxes have none")
  void notesTest() {
    final Notes notes = Notes.NONE.withDanger(Optional.of(Danger.LEVEL_1))
                                  .withAbbreviations(List.of(Abbreviation.FOLLOW_ROAD))
                                  .withSymbols(List.of(new BoxSymbol(Symbol.RAIL_ROAD, SymbolPlace.EXIT)));
    final RoadbookEdits edits = RoadbookEdits.NONE.with(new BoxEdit(NORTH).withNotes(Optional.of(notes)));
    final List<Box> boxes = RoadbookEditor.roadbook(RoadbookGenerator.generate(route()), edits).boxes();
    assertAll(
        () -> assertEquals(Notes.NONE, boxes.get(0).notes()),
        () -> assertEquals(notes, boxes.get(1).notes()),
        () -> assertEquals(Notes.NONE, boxes.get(2).notes()));
  }

  /**
   * A shift that puts a box beyond the total of the next box, or before the total of the previous one, is ignored.
   */
  @Test
  @DisplayName("Phase 5: a shift putting a box before the previous one or beyond the next one is ignored")
  void outOfOrderTest() {
    final Roadbook generated = RoadbookGenerator.generate(route());
    final Roadbook further = RoadbookEditor.roadbook(generated,
                                                     RoadbookEdits.NONE.with(new BoxEdit(NORTH).withShift(TOO_FAR)));
    final Roadbook closer = RoadbookEditor.roadbook(generated,
                                                    RoadbookEdits.NONE.with(new BoxEdit(NORTH).withShift(-TOO_FAR)));
    assertAll(
        () -> assertEquals(generated.boxes().get(1).total(), further.boxes().get(1).total()),
        () -> assertEquals(generated.boxes().get(1).total(), closer.boxes().get(1).total()));
  }

  /**
   * On a new route that turns left at {@link #NORTH} instead of right: the correction of the box there is kept, at
   * its place on the new route, and a second correction of the same box dropped; the box added near the common
   * stretch is kept at the nearest point of the line, with its correction; the box removed at the start is kept; the
   * changes on the stretch no longer driven and at the former finish are dropped.
   */
  @Test
  @DisplayName("Phase 5: the changes are kept on a new route where it still passes, the others dropped")
  void retainTest() {
    final BoxEdit atNorth = new BoxEdit(ALMOST_NORTH).withShift(SHIFT);
    final BoxEdit added = new BoxEdit(HALFWAY).withDisplay(HeadingDisplay.SHOWN);
    final RoadbookEdits edits = new RoadbookEdits(
        List.of(NEAR_HALFWAY, NORTH_EAST), List.of(START, NORTH_EAST),
        List.of(atNorth, new BoxEdit(NORTH).withDisplay(HeadingDisplay.HIDDEN), new BoxEdit(FINISH).withShift(SHIFT),
                added));
    final Route other = route(List.of(START, NORTH, NORTH_WEST), List.of(1));
    final RoadbookEdits kept = RoadbookEditor.retain(edits, other);
    assertAll(
        () -> assertEquals(1, kept.added().size()),
        () -> assertEquals(HALFWAY.latitude(), kept.added().getFirst().latitude(), TOLERANCE),
        () -> assertEquals(HALFWAY.longitude(), kept.added().getFirst().longitude(), TOLERANCE),
        () -> assertEquals(List.of(START), kept.removed()),
        () -> assertEquals(List.of(atNorth.at(NORTH), added.at(kept.added().getFirst())), kept.boxes()));
  }

  /**
   * The changes applied to a route that already holds them change nothing more, and are all kept on it: a roadbook
   * read back gives the same roadbook.
   */
  @Test
  @DisplayName("Phase 5: the changes applied twice give the same route, and are all kept on it")
  void idempotentTest() {
    final RoadbookEdits edits = RoadbookEdits.NONE.withAdded(HALFWAY).withRemoved(NORTH)
                                                  .with(new BoxEdit(HALFWAY).withShift(SHIFT));
    final Route once = RoadbookEditor.route(route(), edits);
    assertAll(
        () -> assertEquals(once, RoadbookEditor.route(once, edits)),
        () -> assertEquals(edits, RoadbookEditor.retain(edits, once)),
        () -> assertEquals(List.of(1), once.maneuvers()));
  }

  /**
   * A segment without length projects every place on its start; the first of two segments as near is kept.
   */
  @Test
  @DisplayName("Phase 5: a segment without length projects on its start, the first of two as near is kept")
  void segmentTest() {
    final RoadbookEditor.Projection tie = RoadbookEditor.nearest(List.of(START, NORTH, START), FAR);
    assertAll(
        () -> assertEquals(START, RoadbookEditor.onSegment(START, START, FAR)),
        () -> assertEquals(0, tie.segment()),
        () -> assertTrue(tie.point().latitude() > START.latitude()));
  }

}
