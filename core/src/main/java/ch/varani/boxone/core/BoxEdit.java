package ch.varani.boxone.core;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The correction of a box by the user, in the editor of the roadbook (development plan, phase 5: "correct a box"):
 * its total distance, its heading, whether the heading is shown, its tulip and its notes.
 *
 * <p>The correction is attached to the place of the box, not to its number, so that it is kept when the route
 * changes and still passes there (decision of the maintainer, 2026-11-14). The total distance is corrected by a
 * <i>shift</i> added to the distance computed along the route, which stays right when the start moves; the partial
 * distances follow the totals. The heading typed replaces the heading computed, the tulip corrected the tulip
 * generated, as long as the junction gives the same tulip ({@link TulipEdit}); the notes are those of the box.</p>
 *
 * <p><b>Preconditions</b>: a place; a finite shift; a heading, when present, between {@value Box#MIN_HEADING} and
 * {@value Box#MAX_HEADING}; a display; a tulip, when present; notes. Otherwise the correction is rejected.</p>
 *
 * <p>Its place is personal data (AGENTS.md §5): a correction is never logged.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param place the place of the box corrected.
 * @param shift the distance added to the total distance computed, in metres: positive further, negative closer.
 * @param heading the heading typed by the user, in degrees; empty for the heading computed.
 * @param display whether the heading is shown.
 * @param tulip the correction of the tulip; empty for the tulip generated.
 * @param notes the notes of the box; {@link Notes#NONE} for none.
 */
public record BoxEdit(GeoPoint place, double shift, OptionalInt heading, HeadingDisplay display,
                      Optional<TulipEdit> tulip, Notes notes) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the correction of a box.
   *
   * @param place the place of the box corrected.
   * @param shift the distance added to the total distance computed, in metres, finite.
   * @param heading the heading typed by the user, in degrees between {@value Box#MIN_HEADING} and
   *                {@value Box#MAX_HEADING}, or empty.
   * @param display whether the heading is shown.
   * @param tulip the correction of the tulip, or empty.
   * @param notes the notes of the box.
   *
   * @throws NullPointerException if the place, the heading, the display, the tulip or the notes are {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_DISTANCE} if the shift is not a finite number,
   *                         {@link ValidationError#INVALID_HEADING} if the heading is beyond its range.
   */
  public BoxEdit {
    Objects.requireNonNull(place);
    Objects.requireNonNull(heading);
    Objects.requireNonNull(display);
    Objects.requireNonNull(tulip);
    Objects.requireNonNull(notes);
    if (!Double.isFinite(shift)) {
      throw new DomainException(ValidationError.INVALID_DISTANCE);
    }
    if (heading.isPresent() && !Box.isHeading(heading.getAsInt())) {
      throw new DomainException(ValidationError.INVALID_HEADING);
    }
  }

  /**
   * Creates the correction of a box that changes nothing yet.
   *
   * @param place the place of the box.
   *
   * @throws NullPointerException if the place is {@code null}.
   */
  public BoxEdit(final GeoPoint place) {
    this(place, 0, OptionalInt.empty(), HeadingDisplay.RULE, Optional.empty(), Notes.NONE);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether the correction changes nothing: no shift, the heading computed, shown as the catalogue decides, the
   * tulip generated, no notes.
   *
   * @return {@code true} if the box is left as it was generated.
   */
  public boolean isEmpty() {
    return shift == 0 && heading.isEmpty() && display == HeadingDisplay.RULE && tulip.isEmpty() && notes.isEmpty();
  }

  /**
   * Returns the correction moved to another place: the place of the same box on another route.
   *
   * @param newPlace the new place.
   *
   * @return the same correction at the new place.
   *
   * @throws NullPointerException if the place is {@code null}.
   */
  public BoxEdit at(final GeoPoint newPlace) {
    return new BoxEdit(newPlace, shift, heading, display, tulip, notes);
  }

  /**
   * Returns the correction with another shift of the total distance.
   *
   * @param newShift the distance added to the total distance computed, in metres, finite.
   *
   * @return the correction with the new shift.
   *
   * @throws DomainException {@link ValidationError#INVALID_DISTANCE} if the shift is not a finite number.
   */
  public BoxEdit withShift(final double newShift) {
    return new BoxEdit(place, newShift, heading, display, tulip, notes);
  }

  /**
   * Returns the correction with another heading.
   *
   * @param newHeading the heading typed, or empty for the heading computed.
   *
   * @return the correction with the new heading.
   *
   * @throws NullPointerException if the heading is {@code null}.
   * @throws DomainException {@link ValidationError#INVALID_HEADING} if the heading is beyond its range.
   */
  public BoxEdit withHeading(final OptionalInt newHeading) {
    return new BoxEdit(place, shift, newHeading, display, tulip, notes);
  }

  /**
   * Returns the correction with another display of the heading.
   *
   * @param newDisplay whether the heading is shown.
   *
   * @return the correction with the new display.
   *
   * @throws NullPointerException if the display is {@code null}.
   */
  public BoxEdit withDisplay(final HeadingDisplay newDisplay) {
    return new BoxEdit(place, shift, heading, newDisplay, tulip, notes);
  }

  /**
   * Returns the correction with another tulip.
   *
   * @param newTulip the correction of the tulip, or empty for the tulip generated.
   *
   * @return the correction with the new tulip.
   *
   * @throws NullPointerException if the tulip is {@code null}.
   */
  public BoxEdit withTulip(final Optional<TulipEdit> newTulip) {
    return new BoxEdit(place, shift, heading, display, newTulip, notes);
  }

  /**
   * Returns the correction with other notes.
   *
   * @param newNotes the notes of the box; {@link Notes#NONE} for none.
   *
   * @return the correction with the new notes.
   *
   * @throws NullPointerException if the notes are {@code null}.
   */
  public BoxEdit withNotes(final Notes newNotes) {
    return new BoxEdit(place, shift, heading, display, tulip, newNotes);
  }

}
