package ch.varani.boxone.core;

import java.util.Objects;

/**
 * The correction of the tulip of a box by the user, in the editor of the roadbook (development plan, phase 5; decision
 * of the maintainer, 2026-11-14: move, add or remove a branch, a tarmac road or a track, the exit): the tulip drawn
 * instead of the tulip generated.
 *
 * <p>The tulip generated is kept with the correction: the correction applies only while the junction of the box still
 * gives the same tulip. A correction is attached to the place of a box and kept when the route changes; a new route
 * that crosses the same junction from another branch, or leaves it by another, gives another tulip there, which the
 * correction would draw wrong: it is then ignored, and the tulip generated is drawn.</p>
 *
 * <p><b>Preconditions</b>: two tulips. Otherwise the correction is rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param generated the tulip generated from the map, which the correction replaces.
 * @param tulip the tulip corrected by the user.
 */
public record TulipEdit(Tulip generated, Tulip tulip) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the correction of a tulip.
   *
   * @param generated the tulip generated from the map.
   * @param tulip the tulip corrected by the user.
   *
   * @throws NullPointerException if a tulip is {@code null}.
   */
  public TulipEdit {
    Objects.requireNonNull(generated);
    Objects.requireNonNull(tulip);
  }

}
