package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Tulip;

/**
 * Which boxes show their heading (CAP): the FIA asks for the necessary headings only, "e.g. at complicated crossings,
 * poorly visible tracks or off-road" (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.11.1).
 *
 * <p><b>Rule</b>: a box shows its heading if it has one and either has no tulip (the box of the start, where the
 * heading to take is given, §5.11.3), or its tulip has at least {@code branches} branches besides the entry (a
 * complicated crossing), or, if {@code tracks}, its exit is a track. Confirmed by the maintainer (2026-11-14), with 3
 * branches.
 * </p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param branches the smallest number of branches, besides the entry, of a crossing complicated enough for a heading.
 * @param tracks whether a box whose exit is a track shows its heading.
 */
public record HeadingRule(int branches, boolean tracks) {

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a box shows its heading.
   *
   * @param box the box.
   *
   * @return {@code true} if the box has a heading and the rule asks for it.
   */
  public boolean shows(final Box box) {
    return box.heading().isPresent() && box.tulip().map(this::needs).orElse(true);
  }

  /**
   * Tells whether the junction of a tulip needs a heading.
   *
   * @param tulip the tulip.
   *
   * @return {@code true} if the tulip has at least {@link #branches()} branches besides the entry, or, if
   *         {@link #tracks()}, its exit is a track.
   */
  private boolean needs(final Tulip tulip) {
    return tulip.branches().size() >= branches || tracks && tulip.kinds().get(tulip.exit()) == BranchKind.TRACK;
  }

}
