package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Tulip}: a tulip keeps its shape, the kind of its entry, its branches and their kinds, and its exit,
 * is not changed by the lists it was built from, and rejects a tulip without branch, an angle beyond −179–179 degrees
 * or out of order, not one kind per branch, and an exit that is not a branch.
 */
final class TulipTest {

  /** The angles of the branches of a crossroads, the entry excepted: left, straight ahead, right. */
  private static final List<Integer> CROSSROADS = List.of(-90, 0, 90);
  /** The kinds of the branches of the crossroads: a track to the left, tarmac roads elsewhere. */
  private static final List<BranchKind> KINDS = List.of(BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TARMAC);
  /** The index of the branch to the right, the one to take. */
  private static final int RIGHT = 2;
  /** The angles of the branches of a crossroads out of order: right, then straight ahead. */
  private static final List<Integer> UNORDERED = List.of(90, 0);

  /**
   * A tulip keeps its shape, the kind of its entry, its branches and their kinds, and its exit.
   */
  @Test
  @DisplayName("Phase 4: a tulip keeps its shape, the kind of its entry, its branches, their kinds and its exit")
  void valuesTest() {
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, CROSSROADS, KINDS, RIGHT);
    assertAll(
        () -> assertEquals(JunctionShape.CROSSING, tulip.shape()),
        () -> assertEquals(BranchKind.TRACK, tulip.entry()),
        () -> assertEquals(CROSSROADS, tulip.branches()),
        () -> assertEquals(KINDS, tulip.kinds()),
        () -> assertEquals(RIGHT, tulip.exit()));
  }

  /**
   * The lists given are copied: changing them later does not change the tulip, whose lists cannot be changed.
   */
  @Test
  @DisplayName("Phase 4: a tulip is not changed by the lists it was built from")
  void copyTest() {
    final List<Integer> branches = new ArrayList<>(CROSSROADS);
    final List<BranchKind> kinds = new ArrayList<>(KINDS);
    final Tulip tulip = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, branches, kinds, RIGHT);
    branches.clear();
    kinds.clear();
    final List<?> keptBranches = tulip.branches();
    final List<?> keptKinds = tulip.kinds();
    assertAll(
        () -> assertEquals(CROSSROADS, keptBranches),
        () -> assertEquals(KINDS, keptKinds),
        () -> assertThrows(UnsupportedOperationException.class, keptBranches::clear),
        () -> assertThrows(UnsupportedOperationException.class, keptKinds::clear));
  }

  /**
   * A tulip of the smallest and the largest angles, either side of the entry, is accepted.
   */
  @Test
  @DisplayName("Phase 4: a tulip whose branches are at −179 and 179 degrees, either side of the entry, is accepted")
  void boundsTest() {
    final List<Integer> hairpins = List.of(Tulip.MIN_ANGLE, Tulip.MAX_ANGLE);
    assertEquals(hairpins, new Tulip(JunctionShape.CLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, hairpins, tarmac(2), 0)
        .branches());
  }

  /**
   * A tulip without branch, with an angle beyond its range or out of order, not one kind per branch, or whose exit is
   * not a branch is rejected.
   */
  @Test
  @DisplayName("Phase 4: a tulip without branch, its angles out of range or of order, not one kind per branch, or "
               + "its exit not a branch, is rejected")
  void invalidTest() {
    assertAll(
        () -> assertInvalid(List.of(), tarmac(0), 0),
        () -> assertInvalid(CROSSROADS, KINDS, -1),
        () -> assertInvalid(CROSSROADS, KINDS, CROSSROADS.size()),
        () -> assertInvalid(CROSSROADS, tarmac(2), 0),
        () -> assertInvalid(List.of(Tulip.MIN_ANGLE - 1), tarmac(1), 0),
        () -> assertInvalid(List.of(Tulip.ENTRY_ANGLE), tarmac(1), 0),
        () -> assertInvalid(List.of(0, 0), tarmac(2), 0),
        () -> assertInvalid(UNORDERED, tarmac(2), 0));
  }

  /**
   * A missing shape, kind of the entry, list, angle or kind is a programming error.
   */
  @Test
  @DisplayName("A tulip without its shape, the kind of its entry, its branches, their kinds or one of them is a "
               + "programming error")
  void nullTest() {
    final List<Integer> withNull = new ArrayList<>(CROSSROADS);
    withNull.set(0, null);
    final List<BranchKind> kindsWithNull = new ArrayList<>(KINDS);
    kindsWithNull.set(0, null);
    final BranchKind tarmac = BranchKind.TARMAC;
    final JunctionShape crossing = JunctionShape.CROSSING;
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Tulip(null, tarmac, CROSSROADS, KINDS, RIGHT)),
        () -> assertThrows(NullPointerException.class, () -> new Tulip(crossing, null, CROSSROADS, KINDS, RIGHT)),
        () -> assertThrows(NullPointerException.class, () -> new Tulip(crossing, tarmac, null, KINDS, RIGHT)),
        () -> assertThrows(NullPointerException.class, () -> new Tulip(crossing, tarmac, withNull, KINDS, RIGHT)),
        () -> assertThrows(NullPointerException.class, () -> new Tulip(crossing, tarmac, CROSSROADS, null, RIGHT)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Tulip(crossing, tarmac, CROSSROADS, kindsWithNull, RIGHT)));
  }

  /**
   * Gives the kinds of branches all tarmac roads.
   *
   * @param size the number of branches.
   *
   * @return as many tarmac roads.
   */
  private static List<BranchKind> tarmac(final int size) {
    return Collections.nCopies(size, BranchKind.TARMAC);
  }

  /**
   * Asserts that a tulip is rejected for its branches, their kinds or its exit.
   *
   * @param branches the angles of the branches.
   * @param kinds the kinds of the branches.
   * @param exit the index of the exit.
   */
  private static void assertInvalid(final List<Integer> branches, final List<BranchKind> kinds, final int exit) {
    final DomainException rejected = assertThrows(DomainException.class,
        () -> new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, branches, kinds, exit));
    assertEquals(ValidationError.INVALID_TULIP, rejected.getError());
  }

}
