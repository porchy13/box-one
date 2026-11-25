package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideScenery}.
 */
final class RideSceneryTest {

  /** The seed of the tests. */
  private static final long SEED = 20_261_121;
  /** The boxes of the roadbook of the tests. */
  private static final int BOXES = 60;
  /** The distance between two boxes of the tests, in metres: room for an obstacle. */
  private static final double APART = 200;
  /** A crossing of three roads: left, straight on, right. */
  private static final List<Integer> THREE = List.of(-Fixtures.QUARTER, 0, Fixtures.QUARTER);
  /** A tulip whose roads pass by every corner of its diagram. */
  private static final List<Integer> STAR = List.of(-135, -45, 0, 45, 135);
  /** A tulip whose roads pass by every corner of its diagram but the one ahead at the left. */
  private static final List<Integer> BUT_AHEAD_LEFT = List.of(-135, 45, 135);

  /**
   * Gives a box of the tests.
   *
   * @param number its number.
   * @param total its total distance.
   * @param branches the angles of the branches of its tulip, or {@code null} for none.
   * @param notes its notes.
   *
   * @return the box.
   */
  private static Box box(final int number, final double total, final List<Integer> branches, final Notes notes) {
    final Optional<Tulip> tulip = Optional.ofNullable(branches).map(
        angles -> new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, angles,
                            Collections.nCopies(angles.size(), BranchKind.TARMAC), 0));
    return new Box(number, Fixtures.CENTRE, total, 0, OptionalInt.empty(), tulip, HeadingDisplay.RULE, notes,
                   Optional.empty());
  }

  /**
   * Some boxes with a tulip get an element beside the road off the roads of their diagram, some an obstacle on their
   * exit when the next box is far enough; the boxes without tulip or with notes are kept; the same seed gives the
   * same boxes.
   */
  @Test
  @DisplayName("Phase 7c: the roadbook dressed with elements beside the road and obstacles, from a seed")
  void dressTest() {
    final Notes noted = Notes.NONE.withText("KEEP");
    final List<Box> boxes = new ArrayList<>(List.of(box(1, 0, null, Notes.NONE), box(2, 1, THREE, noted)));
    for (int i = 2 + 1; i <= BOXES; i++) {
      boxes.add(box(i, i * APART, THREE, Notes.NONE));
    }
    boxes.add(box(BOXES + 1, BOXES * APART + 1, THREE, Notes.NONE));
    final List<Box> dressed = RideScenery.dress(boxes, SEED);
    final List<BoxSymbol> symbols = dressed.stream().flatMap(box -> box.notes().symbols().stream()).toList();
    assertAll(
        () -> assertEquals(boxes.subList(0, 2), dressed.subList(0, 2)),
        () -> assertEquals(dressed, RideScenery.dress(boxes, SEED)),
        () -> assertTrue(symbols.stream().anyMatch(symbol -> symbol.at().isPresent())),
        () -> assertTrue(symbols.stream().anyMatch(symbol -> symbol.at().isEmpty())),
        () -> assertTrue(symbols.stream().allMatch(symbol -> RideItem.Kind.of(symbol.symbol()).isPresent())),
        () -> assertTrue(dressed.get(BOXES - 1).notes().symbols().stream().allMatch(s -> s.at().isPresent())),
        () -> assertTrue(dressed.getLast().notes().symbols().stream().allMatch(s -> s.at().isPresent())),
        () -> assertTrue(dressed.stream().anyMatch(box -> box.notes().equals(Notes.NONE) && box.tulip().isPresent())));
  }

  /**
   * A corner is chosen off every road of the diagram; none if every corner has a road.
   */
  @Test
  @DisplayName("Phase 7c: an element beside the road placed off the roads of the diagram")
  void slotTest() {
    final RandomGenerator random = RandomGeneratorFactory.of("L64X128MixRandom").create(SEED);
    assertAll(
        () -> assertEquals(Optional.empty(), RideScenery.slot(STAR, random)),
        () -> assertEquals(Optional.of(RideScenery.SLOTS.get(1)), RideScenery.slot(BUT_AHEAD_LEFT, random)),
        () -> assertTrue(RideScenery.SLOTS.contains(RideScenery.slot(THREE, random).orElseThrow())));
  }

}
