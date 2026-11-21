package ch.varani.boxone.training;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Turn}: the direction of each branch of a tulip in words, at a crossing by its angle, at a roundabout
 * by the number of its exit in the direction the ring is driven.
 */
final class TurnTest {

  /** A branch slightly to the right, in degrees: at the bound of the slight turns. */
  private static final int SLIGHT = Turn.SLIGHT_ANGLE;
  /** A branch to the left, in degrees: at the bound of the plain turns. */
  private static final int LEFT = -Turn.PLAIN_ANGLE;
  /** A branch sharply to the right, in degrees. */
  private static final int SHARP = Turn.PLAIN_ANGLE + 1;
  /** A quarter turn, in degrees: a branch to a side. */
  private static final int QUARTER = 90;
  /** The number of the third exit of a roundabout. */
  private static final int THIRD = 3;

  /**
   * At a crossing, the angle of a branch gives its direction: straight on, slightly, plainly or sharply to its side.
   */
  @Test
  @DisplayName("Phase 7b: at a crossing, the angle of a branch gives its direction in words")
  void crossingTest() {
    final List<Integer> angles = List.of(-SHARP, LEFT, -SLIGHT - 1, -Turn.STRAIGHT_ANGLE, Turn.STRAIGHT_ANGLE + 1,
                                         SLIGHT, Turn.PLAIN_ANGLE, SHARP);
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, angles,
                                  Collections.nCopies(angles.size(), BranchKind.TARMAC), 1);
    assertAll(
        () -> assertEquals(List.of(TurnKind.SHARP_LEFT, TurnKind.LEFT, TurnKind.LEFT, TurnKind.STRAIGHT,
                                   TurnKind.SLIGHT_RIGHT, TurnKind.SLIGHT_RIGHT, TurnKind.RIGHT, TurnKind.SHARP_RIGHT),
                           Turn.of(tulip).stream().map(Turn::kind).toList()),
        () -> assertEquals(new Turn(TurnKind.LEFT, 0), Turn.taken(tulip)),
        () -> assertEquals(new Turn(TurnKind.SLIGHT_LEFT, 0), Turn.crossing(-SLIGHT)));
  }

  /**
   * At a roundabout driven counterclockwise, the exits are numbered from the right; driven clockwise, from the left.
   */
  @Test
  @DisplayName("Phase 7b: the exits of a roundabout numbered in the direction the ring is driven")
  void roundaboutTest() {
    final List<Integer> branches = List.of(-QUARTER, 0, QUARTER);
    final List<BranchKind> kinds = Collections.nCopies(branches.size(), BranchKind.TARMAC);
    final Tulip counterclockwise = new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, branches,
                                             kinds, 0);
    final Tulip clockwise = new Tulip(JunctionShape.CLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, branches, kinds, 0);
    assertAll(
        () -> assertEquals(List.of(THIRD, 2, 1), Turn.of(counterclockwise).stream().map(Turn::exit).toList()),
        () -> assertEquals(new Turn(TurnKind.ROUNDABOUT, THIRD), Turn.taken(counterclockwise)),
        () -> assertEquals(List.of(1, 2, THIRD), Turn.of(clockwise).stream().map(Turn::exit).toList()));
  }

  /**
   * A roundabout without the number of its exit, another kind with one, or no kind: a programming error.
   */
  @Test
  @DisplayName("Phase 7b: the number of an exit for a roundabout only")
  void invalidTest() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new Turn(TurnKind.ROUNDABOUT, 0)),
        () -> assertThrows(IllegalArgumentException.class, () -> new Turn(TurnKind.LEFT, 1)),
        () -> assertThrows(NullPointerException.class, () -> new Turn(null, 0)));
  }

}
