package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Junction}: the geometry of a junction keeps its shape, its branches and their kinds, its entry and
 * its exit, is not changed by the lists it was built from, and rejects fewer than two branches, a bearing beyond 0–360
 * degrees, not one kind per branch, and an entry or an exit that is not a branch, or the same.
 */
final class JunctionTest {

  /** The bearings of the branches of a crossroads: north, east, south, west. */
  private static final List<Double> CROSSROADS = List.of(0.0, 90.0, 180.0, 270.0);
  /** The kinds of the branches of the crossroads: a track to the north, tarmac roads elsewhere. */
  private static final List<BranchKind> KINDS = List.of(BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TARMAC,
                                                        BranchKind.TARMAC);
  /** The index of the branch of the south, where the route comes from. */
  private static final int SOUTH = 2;
  /** The index of the branch of the east, the road taken. */
  private static final int EAST = 1;
  /** The departure of a roundabout: the route leaves its ring three points after its entry. */
  private static final int ALONG_RING = 3;
  /** A bearing just below zero, in degrees. */
  private static final double NEGATIVE = -0.1;

  /**
   * A junction keeps its shape, its branches and their kinds, its entry, its exit and its departure.
   */
  @Test
  @DisplayName("Phase 4: the geometry of a junction keeps its shape, its branches, their kinds, its entry, its exit "
               + "and its departure")
  void valuesTest() {
    final Junction junction = new Junction(JunctionShape.CROSSING, CROSSROADS, KINDS, SOUTH, EAST, ALONG_RING);
    assertAll(
        () -> assertEquals(JunctionShape.CROSSING, junction.shape()),
        () -> assertEquals(CROSSROADS, junction.branches()),
        () -> assertEquals(KINDS, junction.kinds()),
        () -> assertEquals(SOUTH, junction.entry()),
        () -> assertEquals(EAST, junction.exit()),
        () -> assertEquals(ALONG_RING, junction.departure()));
  }

  /**
   * The lists given are copied: changing them later does not change the junction, whose lists cannot be changed.
   */
  @Test
  @DisplayName("Phase 4: the geometry of a junction is not changed by the lists it was built from")
  void copyTest() {
    final List<Double> branches = new ArrayList<>(CROSSROADS);
    final List<BranchKind> kinds = new ArrayList<>(KINDS);
    final Junction junction = new Junction(JunctionShape.CROSSING, branches, kinds, SOUTH, EAST, 0);
    branches.clear();
    kinds.clear();
    final List<?> keptBranches = junction.branches();
    final List<?> keptKinds = junction.kinds();
    assertAll(
        () -> assertEquals(CROSSROADS, keptBranches),
        () -> assertEquals(KINDS, keptKinds),
        () -> assertThrows(UnsupportedOperationException.class, keptBranches::clear),
        () -> assertThrows(UnsupportedOperationException.class, keptKinds::clear));
  }

  /**
   * Two branches, the smallest bearing and the largest below a full turn are accepted.
   */
  @Test
  @DisplayName("Phase 4: a junction of two branches, bearing 0 and just below 360 degrees, is accepted")
  void boundsTest() {
    final List<Double> bend = List.of(Junction.MIN_BEARING, Math.nextDown(Junction.MAX_BEARING));
    assertEquals(bend, new Junction(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, bend, tarmac(2), 1, 0, 0).branches());
  }

  /**
   * A bearing below 0, of a full turn or more, or not a number is rejected.
   *
   * @param bearing the invalid bearing, in degrees.
   */
  @ParameterizedTest
  @ValueSource(doubles = {NEGATIVE, Junction.MAX_BEARING, Double.NaN, Double.POSITIVE_INFINITY})
  @DisplayName("Phase 4: a bearing of a junction beyond 0–360 degrees is rejected")
  void invalidBearingTest(final double bearing) {
    assertInvalid(List.of(0.0, bearing), tarmac(2), 0, 1);
  }

  /**
   * A junction of one branch, not one kind per branch, an entry or an exit out of the branches, an exit that is the
   * entry, and a negative departure are rejected.
   */
  @Test
  @DisplayName("Phase 4: a junction of one branch, not one kind per branch, whose entry or exit is not a branch or "
               + "the same, or of a negative departure, is rejected")
  void invalidBranchesTest() {
    final List<Double> one = List.of(0.0);
    final int size = CROSSROADS.size();
    assertAll(
        () -> assertInvalid(one, tarmac(1), 0, 0),
        () -> assertInvalid(CROSSROADS, tarmac(size - 1), SOUTH, EAST),
        () -> assertInvalid(CROSSROADS, KINDS, -1, EAST),
        () -> assertInvalid(CROSSROADS, KINDS, size, EAST),
        () -> assertInvalid(CROSSROADS, KINDS, SOUTH, -1),
        () -> assertInvalid(CROSSROADS, KINDS, SOUTH, size),
        () -> assertInvalid(CROSSROADS, KINDS, SOUTH, SOUTH),
        () -> assertEquals(ValidationError.INVALID_JUNCTION,
                           assertThrows(DomainException.class,
                                        () -> new Junction(JunctionShape.CROSSING, CROSSROADS, KINDS, SOUTH, EAST,
                                                           -1)).getError()));
  }

  /**
   * A missing shape, list, bearing or kind is a programming error.
   */
  @Test
  @DisplayName("A junction without its shape, its branches, their kinds or one of them is a programming error")
  void nullTest() {
    final List<Double> withNull = new ArrayList<>(CROSSROADS);
    withNull.set(0, null);
    final List<BranchKind> kindsWithNull = new ArrayList<>(KINDS);
    kindsWithNull.set(0, null);
    assertAll(
        () -> assertThrows(NullPointerException.class, () -> new Junction(null, CROSSROADS, KINDS, SOUTH, EAST, 0)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Junction(JunctionShape.CROSSING, null, KINDS, SOUTH, EAST, 0)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Junction(JunctionShape.CROSSING, withNull, KINDS, SOUTH, EAST, 0)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Junction(JunctionShape.CROSSING, CROSSROADS, null, SOUTH, EAST, 0)),
        () -> assertThrows(NullPointerException.class,
                           () -> new Junction(JunctionShape.CROSSING, CROSSROADS, kindsWithNull, SOUTH, EAST, 0)));
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
   * Asserts that a junction is rejected for its branches, their kinds, its entry or its exit.
   *
   * @param branches the bearings of the branches.
   * @param kinds the kinds of the branches.
   * @param entry the index of the entry.
   * @param exit the index of the exit.
   */
  private static void assertInvalid(final List<Double> branches, final List<BranchKind> kinds, final int entry,
                                    final int exit) {
    final DomainException rejected = assertThrows(DomainException.class,
        () -> new Junction(JunctionShape.CROSSING, branches, kinds, entry, exit, 0));
    assertEquals(ValidationError.INVALID_JUNCTION, rejected.getError());
  }

}
