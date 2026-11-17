package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Tulip}: a tulip keeps its shape, the kind of its entry, its branches and their kinds, and its exit,
 * is not changed by the lists it was built from, and rejects a tulip without branch, an angle beyond −179–179 degrees
 * or out of order, not one kind per branch, and an exit that is not a branch; a tulip is corrected (phase 5) by moving
 * a branch between its neighbours, changing the kind of a branch or of the entry, the exit, adding a branch where
 * there is room and removing a branch but the exit.
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
  /** The angle of the branch to the left, in degrees. */
  private static final int LEFT_ANGLE = -90;
  /** The angle of the branch to the right, in degrees. */
  private static final int RIGHT_ANGLE = 90;
  /** The index of the branch straight ahead. */
  private static final int AHEAD = 1;
  /** An angle between straight ahead and the right, in degrees. */
  private static final int HALF_RIGHT = 45;
  /** An angle to the left behind the branch to the left, in degrees. */
  private static final int BACK_LEFT = -135;
  /** The crossroads of the tests, the exit to the right. */
  private static final Tulip TULIP = new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, CROSSROADS, KINDS, RIGHT);

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
   * A branch moves between its neighbours; it cannot reach or pass one of them.
   */
  @Test
  @DisplayName("Phase 5: a branch of a tulip moves between its neighbours, not onto nor beyond one of them")
  void withAngleTest() {
    final int beyond = CROSSROADS.size();
    assertAll(
        () -> assertEquals(List.of(LEFT_ANGLE, HALF_RIGHT, RIGHT_ANGLE), TULIP.withAngle(AHEAD, HALF_RIGHT).branches()),
        () -> assertEquals(List.of(LEFT_ANGLE, 0, Tulip.MAX_ANGLE), TULIP.withAngle(RIGHT, Tulip.MAX_ANGLE).branches()),
        () -> assertEquals(RIGHT, TULIP.withAngle(AHEAD, HALF_RIGHT).exit()),
        () -> assertInvalid(() -> TULIP.withAngle(AHEAD, RIGHT_ANGLE)),
        () -> assertInvalid(() -> TULIP.withAngle(AHEAD, BACK_LEFT)),
        () -> assertInvalid(() -> TULIP.withAngle(RIGHT, Tulip.ENTRY_ANGLE)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> TULIP.withAngle(beyond, 0)));
  }

  /**
   * The kind of a branch, of the entry, and the exit change.
   */
  @Test
  @DisplayName("Phase 5: the kind of a branch or of the entry of a tulip, and its exit, are corrected")
  void withKindTest() {
    assertAll(
        () -> assertEquals(List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC),
                           TULIP.withKind(0, BranchKind.TARMAC).kinds()),
        () -> assertEquals(BranchKind.TARMAC, TULIP.withEntry(BranchKind.TARMAC).entry()),
        () -> assertEquals(AHEAD, TULIP.withExit(AHEAD).exit()),
        () -> assertEquals(CROSSROADS, TULIP.withExit(AHEAD).branches()),
        () -> assertInvalid(() -> TULIP.withExit(CROSSROADS.size())),
        () -> assertThrows(NullPointerException.class, () -> TULIP.withKind(0, null)),
        () -> assertThrows(NullPointerException.class, () -> TULIP.withEntry(null)));
  }

  /**
   * A branch added takes its place from the left to the right, the exit staying the same branch; an angle taken or
   * beyond the range is rejected.
   */
  @Test
  @DisplayName("Phase 5: a branch added to a tulip takes its place among the others, the exit staying the same")
  void withBranchTest() {
    final Tulip left = TULIP.withBranch(BACK_LEFT, BranchKind.TRACK);
    final Tulip right = TULIP.withBranch(Tulip.MAX_ANGLE, BranchKind.TARMAC);
    assertAll(
        () -> assertEquals(List.of(BACK_LEFT, LEFT_ANGLE, 0, RIGHT_ANGLE), left.branches()),
        () -> assertEquals(BranchKind.TRACK, left.kinds().getFirst()),
        () -> assertEquals(RIGHT + 1, left.exit()),
        () -> assertEquals(List.of(LEFT_ANGLE, 0, RIGHT_ANGLE, Tulip.MAX_ANGLE), right.branches()),
        () -> assertEquals(BranchKind.TARMAC, right.kinds().getLast()),
        () -> assertEquals(RIGHT, right.exit()),
        () -> assertInvalid(() -> TULIP.withBranch(0, BranchKind.TARMAC)),
        () -> assertInvalid(() -> TULIP.withBranch(Tulip.ENTRY_ANGLE, BranchKind.TARMAC)),
        () -> assertThrows(NullPointerException.class, () -> TULIP.withBranch(HALF_RIGHT, null)));
  }

  /**
   * A branch removed leaves the exit the same branch; the exit cannot be removed.
   */
  @Test
  @DisplayName("Phase 5: a branch of a tulip is removed, the exit staying the same; the exit cannot be removed")
  void withoutBranchTest() {
    final Tulip withoutLeft = TULIP.withoutBranch(0);
    final Tulip withExitAhead = TULIP.withExit(AHEAD).withoutBranch(RIGHT);
    assertAll(
        () -> assertEquals(List.of(0, RIGHT_ANGLE), withoutLeft.branches()),
        () -> assertEquals(List.of(BranchKind.TARMAC, BranchKind.TARMAC), withoutLeft.kinds()),
        () -> assertEquals(1, withoutLeft.exit()),
        () -> assertEquals(List.of(LEFT_ANGLE, 0), withExitAhead.branches()),
        () -> assertEquals(AHEAD, withExitAhead.exit()),
        () -> assertInvalid(() -> TULIP.withoutBranch(RIGHT)),
        () -> assertThrows(IndexOutOfBoundsException.class, () -> TULIP.withoutBranch(-1)));
  }

  /**
   * The angle free for a new branch is the middle of the widest space between the branches and the entry, the first
   * from the left in a tie; there is none when every angle is a branch.
   */
  @Test
  @DisplayName("Phase 5: a branch is added in the middle of the widest space of a tulip, if there is room")
  void freeAngleTest() {
    final List<Integer> every = new ArrayList<>();
    for (int angle = Tulip.MIN_ANGLE; angle <= Tulip.MAX_ANGLE; angle++) {
      every.add(angle);
    }
    final Tulip full = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, every, tarmac(every.size()), 0);
    final Tulip straight = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0), tarmac(1), 0);
    final Tulip hairpin = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(Tulip.MIN_ANGLE), tarmac(1), 0);
    assertAll(
        () -> assertEquals(OptionalInt.of(BACK_LEFT), TULIP.freeAngle()),
        () -> assertEquals(OptionalInt.of(LEFT_ANGLE), straight.freeAngle()),
        () -> assertEquals(OptionalInt.of(0), hairpin.freeAngle()),
        () -> assertEquals(OptionalInt.empty(), full.freeAngle()));
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

  /**
   * Asserts that a correction of a tulip is rejected.
   *
   * @param correction the correction.
   */
  private static void assertInvalid(final Executable correction) {
    final DomainException rejected = assertThrows(DomainException.class, correction);
    assertEquals(ValidationError.INVALID_TULIP, rejected.getError());
  }

}
