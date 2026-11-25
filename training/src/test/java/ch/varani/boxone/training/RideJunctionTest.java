package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideJunction}: the branches of a junction of a simulated run, the one chosen at first and the one to
 * take.
 */
final class RideJunctionTest {

  /** A slight angle to the right, in degrees. */
  private static final int SLIGHT = 20;
  /** A fork: left, slightly right, plainly right. */
  private static final List<Integer> FORK = List.of(-Fixtures.RIGHT, SLIGHT, Fixtures.QUARTER);

  /**
   * Gives a junction of a tulip.
   *
   * @param branches the angles of its branches.
   * @param exit the index of its exit.
   *
   * @return the junction, arriving to the east.
   */
  private static RideJunction junction(final List<Integer> branches, final int exit) {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, branches,
                                  branches.stream().map(angle -> BranchKind.TARMAC).toList(), exit);
    return new RideJunction(new Box(2, Fixtures.CENTRE, Fixtures.TOTAL, Fixtures.TOTAL, OptionalInt.empty(),
                                    Optional.of(tulip)), Fixtures.TOTAL, 0, 0, Fixtures.HEADING, tulip,
                            Optional.empty());
  }

  /**
   * Each branch leaves at the arrival plus its angle; the branch nearest to straight on is chosen at first; the exit is
   * the right one, another branch not.
   */
  @Test
  @DisplayName("Phase 7c: the branches of a junction, the first chosen nearest to straight on, the exit right")
  void branchesTest() {
    final RideJunction junction = junction(FORK, 2);
    assertAll(
        () -> assertEquals(Fixtures.HEADING - Fixtures.RIGHT, junction.bearing(0)),
        () -> assertEquals(1, junction.straightest()),
        () -> assertTrue(junction.isRight(2)),
        () -> assertFalse(junction.isRight(1)));
  }

  /**
   * A junction of one branch has nothing to choose: refused.
   */
  @Test
  @DisplayName("Phase 7c: a junction of one branch refused")
  void oneBranchTest() {
    final List<Integer> one = List.of(0);
    assertThrows(IllegalArgumentException.class, () -> junction(one, 0));
  }

}
