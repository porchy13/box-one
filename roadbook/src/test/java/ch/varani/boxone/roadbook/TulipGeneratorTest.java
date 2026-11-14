package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link TulipGenerator} (development plan, phase 4) on junctions written by hand: the angles of the branches
 * from the arrival, the entry at the bottom; the branches closer than the separation drawn as one, the exit kept; the
 * branches too close to the entry not drawn; the roundabouts and their separation; the kinds of the branches.
 */
final class TulipGeneratorTest {

  /** A crossroads, its roads to the north, the east, the south and the west, in degrees. */
  private static final List<Double> CROSSROADS = List.of(0.0, 90.0, 180.0, 270.0);
  /** The index of the road of the south in {@link #CROSSROADS}. */
  private static final int SOUTH = 2;
  /** The index of the road of the east in {@link #CROSSROADS}. */
  private static final int EAST = 1;
  /** The index of the road of the west in {@link #CROSSROADS}. */
  private static final int WEST = 3;
  /** The angle of a branch to the left, in degrees. */
  private static final int LEFT = -90;
  /** The angle of a branch to the right, in degrees. */
  private static final int RIGHT = 90;
  /** A fork: the road of the south-west it comes from, two roads to the north-east, 8 degrees apart, in degrees. */
  private static final List<Double> FORK = List.of(225.0, 41.0, 49.0);
  /** The angles of the two roads of {@link #FORK}, in degrees: 4 degrees left and 4 degrees right. */
  private static final List<Integer> FORK_ANGLES = List.of(-4, 4);
  /**
   * A crossing entered from the south, two of its roads beside the entry: 5 degrees east of the south, and 3 degrees
   * west of it, the exit, a hairpin; and a road to the north, in degrees.
   */
  private static final List<Double> HAIRPIN = List.of(180.0, 175.0, 183.0, 0.0);
  /** The angle of the exit of {@link #HAIRPIN}, in degrees: behind, 3 degrees left of the entry. */
  private static final int HAIRPIN_EXIT = -177;
  /**
   * A roundabout: the entry at the east, then, counterclockwise, the roads meeting the ring just east of the north and
   * at the north, 10 degrees apart, the second the exit, and at the south, in degrees.
   */
  private static final List<Double> ROUNDABOUT = List.of(90.0, 10.0, 0.0, 180.0);
  /** The index of the exit of {@link #ROUNDABOUT}. */
  private static final int ROUNDABOUT_EXIT = 2;
  /**
   * The angles of the tulip of {@link #ROUNDABOUT}, in degrees: the south to the left, the north, the exit, to the
   * right.
   */
  private static final List<Integer> ROUNDABOUT_ANGLES = List.of(LEFT, RIGHT);

  /**
   * A crossroads entered from the south, its exit the east: its roads at −90 (west), 0 (north) and 90 (east), the
   * exit to the right; entered from the west, its exit the east, the exit is straight ahead.
   */
  @Test
  @DisplayName("Phase 4: the angles of the tulip are measured from the arrival, the entry at the bottom")
  void crossroadsTest() {
    final Tulip fromSouth = TulipGenerator.tulip(junction(JunctionShape.CROSSING, CROSSROADS, SOUTH, EAST));
    final Tulip fromWest = TulipGenerator.tulip(junction(JunctionShape.CROSSING, CROSSROADS, WEST, EAST));
    assertAll(
        () -> assertEquals(tulip(JunctionShape.CROSSING, List.of(LEFT, 0, RIGHT), 2), fromSouth),
        () -> assertEquals(tulip(JunctionShape.CROSSING, List.of(LEFT, 0, RIGHT), 1), fromWest));
  }

  /**
   * Two roads 8 degrees apart, below the separation of a crossing, are drawn as one, the exit; the other way round,
   * the exit first, the same.
   */
  @Test
  @DisplayName("Phase 4: two branches closer than the separation are drawn as one, the exit")
  void mergeTest() {
    final Tulip second = TulipGenerator.tulip(junction(JunctionShape.CROSSING, FORK, 0, 2));
    final Tulip first = TulipGenerator.tulip(junction(JunctionShape.CROSSING, FORK, 0, 1));
    assertAll(
        () -> assertEquals(tulip(JunctionShape.CROSSING, List.of(FORK_ANGLES.getLast()), 0), second),
        () -> assertEquals(tulip(JunctionShape.CROSSING, List.of(FORK_ANGLES.getFirst()), 0), first));
  }

  /**
   * A road beside the entry is not drawn, but the exit, drawn where it is; a road just behind the entry is drawn
   * at 179 degrees.
   */
  @Test
  @DisplayName("Phase 4: a branch beside the entry is not drawn, unless it is the exit")
  void entryTest() {
    final Tulip hairpin = TulipGenerator.tulip(junction(JunctionShape.CROSSING, HAIRPIN, 0, 2));
    final Tulip behind = TulipGenerator.tulip(junction(JunctionShape.CROSSING, List.of(0.0, 0.0), 0, 1));
    assertAll(
        () -> assertEquals(tulip(JunctionShape.CROSSING, List.of(HAIRPIN_EXIT, 0), 0), hairpin),
        () -> assertEquals(tulip(JunctionShape.CROSSING, List.of(Tulip.MAX_ANGLE), 0), behind));
  }

  /**
   * A roundabout keeps its shape; two roads meeting its ring 10 degrees apart, below the separation of a roundabout,
   * are drawn as one, the exit.
   */
  @Test
  @DisplayName("Phase 4: a roundabout keeps its shape, its close branches drawn as one")
  void roundaboutTest() {
    final Junction roundabout = junction(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, ROUNDABOUT, 0,
                                             ROUNDABOUT_EXIT);
    assertEquals(tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, ROUNDABOUT_ANGLES, 1),
                 TulipGenerator.tulip(roundabout));
  }

  /**
   * The angles are rounded to the degree, from −179 to 180.
   */
  @Test
  @DisplayName("Phase 4: the angles of a tulip are whole degrees from −179 to 180")
  void angleTest() {
    final int halfTurn = Tulip.ENTRY_ANGLE;
    final int fullTurn = 2 * halfTurn;
    final double right = LEFT - halfTurn - 0.4;
    assertAll(
        () -> assertEquals(Tulip.ENTRY_ANGLE, TulipGenerator.angle(-halfTurn)),
        () -> assertEquals(Tulip.ENTRY_ANGLE, TulipGenerator.angle(fullTurn + halfTurn)),
        () -> assertEquals(Tulip.MIN_ANGLE, TulipGenerator.angle(halfTurn + 1)),
        () -> assertEquals(RIGHT, TulipGenerator.angle(right)),
        () -> assertEquals(LEFT, TulipGenerator.angle(LEFT)));
  }

  /**
   * A track is drawn as a track, and the kind of the entry is kept: a crossroads entered from the south by a track,
   * its road to the north a track too.
   */
  @Test
  @DisplayName("Phase 4: a tulip keeps the kinds of its branches and of its entry")
  void kindsTest() {
    final List<BranchKind> kinds = List.of(BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TRACK, BranchKind.TARMAC);
    final Tulip tulip = TulipGenerator.tulip(new Junction(JunctionShape.CROSSING, CROSSROADS, kinds, SOUTH, EAST, 0));
    assertAll(
        () -> assertEquals(BranchKind.TRACK, tulip.entry()),
        () -> assertEquals(List.of(BranchKind.TARMAC, BranchKind.TRACK, BranchKind.TARMAC), tulip.kinds()));
  }

  /**
   * Creates the geometry of a junction whose roads are all tarmac roads.
   *
   * @param shape the shape of the junction.
   * @param bearings the bearings of its branches, in degrees.
   * @param entry the index of the entry.
   * @param exit the index of the exit.
   *
   * @return the junction.
   */
  private static Junction junction(final JunctionShape shape, final List<Double> bearings, final int entry,
                                   final int exit) {
    return new Junction(shape, bearings, Collections.nCopies(bearings.size(), BranchKind.TARMAC), entry, exit, 0);
  }

  /**
   * Creates a tulip whose roads, the entry included, are all tarmac roads.
   *
   * @param shape the shape of the junction.
   * @param angles the angles of its branches, in degrees.
   * @param exit the index of the exit.
   *
   * @return the tulip.
   */
  private static Tulip tulip(final JunctionShape shape, final List<Integer> angles, final int exit) {
    return new Tulip(shape, BranchKind.TARMAC, angles, Collections.nCopies(angles.size(), BranchKind.TARMAC), exit);
  }

}
