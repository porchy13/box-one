package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Tulip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link HeadingRule}, the rule of the FIA: only the necessary headings (2026 Cross-Country Rally Sporting
 * Regulations, Appendix III, §5.11.1).
 */
final class HeadingRuleTest {

  /** The branches, besides the entry, of a crossing complicated enough for a heading. */
  private static final int COMPLICATED = 3;
  /** The rule of the catalogue of the FIA: three branches besides the entry, or an exit on a track. */
  private static final HeadingRule RULE = new HeadingRule(COMPLICATED, true);
  /** The place of the boxes: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** A heading, in degrees. */
  private static final OptionalInt HEADING = OptionalInt.of(90);
  /** The angles of a crossroads, the entry excepted. */
  private static final List<Integer> CROSSROADS = List.of(-90, 0, 90);
  /** The angles of a junction in T, the entry excepted. */
  private static final List<Integer> TEE = List.of(-90, 90);

  /**
   * The box of the start, without tulip, shows its heading; the box of the finish has none to show.
   */
  @Test
  @DisplayName("FIA §5.11: the box of the start shows its heading, the finish has none")
  void endsTest() {
    assertAll(
        () -> assertTrue(RULE.shows(box(HEADING, Optional.empty()))),
        () -> assertFalse(RULE.shows(box(OptionalInt.empty(), Optional.empty()))));
  }

  /**
   * A crossroads shows its heading, a junction in T between tarmac roads does not, unless its exit is a track and the
   * rule asks for it.
   */
  @Test
  @DisplayName("FIA §5.11: only a complicated crossing or an exit on a track shows its heading")
  void junctionsTest() {
    final List<BranchKind> tarmac = List.of(BranchKind.TARMAC, BranchKind.TARMAC);
    final List<BranchKind> track = List.of(BranchKind.TRACK, BranchKind.TARMAC);
    final Tulip crossroads = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, CROSSROADS,
                                       List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC), 0);
    final Tulip tee = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, TEE, tarmac, 0);
    final Tulip teeToTrack = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, TEE, track, 0);
    final Tulip teeFromTrack = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, TEE, track, 1);
    assertAll(
        () -> assertTrue(RULE.shows(box(HEADING, Optional.of(crossroads)))),
        () -> assertFalse(RULE.shows(box(HEADING, Optional.of(tee)))),
        () -> assertTrue(RULE.shows(box(HEADING, Optional.of(teeToTrack)))),
        () -> assertFalse(RULE.shows(box(HEADING, Optional.of(teeFromTrack)))),
        () -> assertFalse(new HeadingRule(COMPLICATED, false).shows(box(HEADING, Optional.of(teeToTrack)))));
  }

  /**
   * Creates a box.
   *
   * @param heading its heading.
   * @param tulip its tulip.
   *
   * @return the box.
   */
  private static Box box(final OptionalInt heading, final Optional<Tulip> tulip) {
    return new Box(2, STATION, 0, 0, heading, tulip);
  }

}
