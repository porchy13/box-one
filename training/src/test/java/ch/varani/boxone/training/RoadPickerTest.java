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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RoadPicker}: the roads of the map that make a junction.
 */
final class RoadPickerTest {

  /** The radius of the ring of the roundabout of the tests, in metres. */
  private static final double RADIUS = 15;
  /** The length of a road of the tests, in metres. */
  private static final double FAR = 60;
  /** A tulip of three branches. */
  private static final Tulip TULIP = new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC,
                                               List.of(-90, 0, 90),
                                               List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC), 1);

  /**
   * Gives a junction at the origin.
   *
   * @param ring the ring of its roundabout, or nothing.
   *
   * @return the junction.
   */
  private static RideJunction junction(final Optional<RideJunction.Ring> ring) {
    return new RideJunction(new Box(2, Fixtures.CENTRE, 0, 0, OptionalInt.empty(), Optional.of(TULIP)), 0, 0, 0, 0,
                            TULIP, ring);
  }

  /**
   * An end is the node of a junction near its centre, or near its ring; not elsewhere.
   */
  @Test
  @DisplayName("Phase 7c: an end of a road at the junction, or at its ring")
  void touchesTest() {
    final RideJunction crossing = junction(Optional.empty());
    final RideJunction roundabout = junction(Optional.of(new RideJunction.Ring(0, RADIUS, RADIUS)));
    assertAll(
        () -> assertTrue(RoadPicker.touches(new double[] {1, 1}, crossing)),
        () -> assertFalse(RoadPicker.touches(new double[] {RADIUS, RADIUS}, crossing)),
        () -> assertTrue(RoadPicker.touches(new double[] {RADIUS, RADIUS}, roundabout)),
        () -> assertFalse(RoadPicker.touches(new double[] {0, RADIUS}, roundabout)));
  }

  /**
   * A road goes on from another one at the same node, either way, when it turns little; not when it turns much, nor
   * from another node.
   */
  @Test
  @DisplayName("Phase 7c: a road carried straight on beyond a node, either way")
  void goesOnTest() {
    final List<double[]> line = List.of(new double[] {0, 0}, new double[] {0, FAR});
    assertAll(
        () -> assertTrue(RoadPicker.goesOn(line, List.of(new double[] {0, FAR}, new double[] {1, 2 * FAR}))),
        () -> assertTrue(RoadPicker.goesOn(line, List.of(new double[] {1, 2 * FAR}, new double[] {0, FAR}))),
        () -> assertFalse(RoadPicker.goesOn(line, List.of(new double[] {0, FAR}, new double[] {FAR, FAR}))),
        () -> assertFalse(RoadPicker.goesOn(line, List.of(new double[] {FAR, FAR}, new double[] {2 * FAR, FAR}))));
  }

  /**
   * A road meets the road ridden when one of its ends is on it, a crossing or a side road without box; not otherwise.
   */
  @Test
  @DisplayName("Phase 7c: a road meeting the road ridden, a crossing without box")
  void meetsTest() {
    final List<double[]> ridden = List.of(new double[] {0, 0}, new double[] {0, FAR});
    assertAll(
        () -> assertTrue(RoadPicker.meets(List.of(new double[] {0, FAR}, new double[] {FAR, FAR}), ridden)),
        () -> assertTrue(RoadPicker.meets(List.of(new double[] {-FAR, 0}, new double[] {1, 0}), ridden)),
        () -> assertFalse(RoadPicker.meets(List.of(new double[] {FAR, 0}, new double[] {FAR, FAR}), ridden)));
  }

  /**
   * Around a roundabout, the ring and its branches are kept, a branch carried on beyond its nodes and the road ridden;
   * the side road of a branch and a road elsewhere are left out.
   */
  @Test
  @DisplayName("Phase 7c: the roads that make a roundabout, the others left out")
  void pickTest() {
    final List<List<double[]>> lines = List.of(
        List.of(new double[] {0, 2 * RADIUS}, new double[] {0, 2 * RADIUS + FAR}),
        List.of(new double[] {0, 2 * RADIUS + 2 * FAR}, new double[] {0, 2 * RADIUS + FAR}),
        List.of(new double[] {0, 2 * RADIUS + FAR}, new double[] {FAR, 2 * RADIUS + FAR}),
        List.of(new double[] {-FAR, -FAR}, new double[] {-2 * FAR, -FAR}),
        List.of(new double[] {0, -FAR}, new double[] {0, -FAR - RADIUS}),
        List.of(new double[] {0, 2 * RADIUS + 2 * FAR}, new double[] {0, 2 * RADIUS + 2 * FAR + FAR}),
        List.of(new double[] {-FAR, RADIUS}, new double[] {-RADIUS, RADIUS}));
    final List<BranchKind> kinds = List.of(BranchKind.TARMAC, BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TARMAC,
                                           BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TRACK);
    final List<double[]> ridden = List.of(new double[] {0, -FAR}, new double[] {0, -FAR - RADIUS}, new double[] {0, 0});
    final List<RoadPicker.Picked> picked = RoadPicker.pick(lines, kinds,
        List.of(junction(Optional.of(new RideJunction.Ring(0, RADIUS, RADIUS)))), ridden);
    assertEquals(List.of(BranchKind.TARMAC, BranchKind.TRACK, BranchKind.TRACK, BranchKind.TARMAC, BranchKind.TRACK),
                 picked.stream().map(RoadPicker.Picked::kind).toList());
  }

}
