package ch.varani.boxone.core;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A waypoint of a box of the roadbook (development plan, phase 5, increment 4; FIA, 2026 Cross-Country Rally Sporting
 * Regulations, §43, and Appendix III, §5.12.5: every waypoint is assigned to a box): its kind and its radii of opening
 * and of validation, those of the FIA by default ({@link #of(WaypointKind)}), changed by the user (decision of the
 * maintainer, 2026-11-18).
 *
 * <p><b>Preconditions</b>: a kind; a radius of opening, when present, and a radius of validation from
 * {@value #MIN_RADIUS} to {@value #MAX_RADIUS} metres. Otherwise the waypoint is rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param kind the kind of the waypoint.
 * @param open the radius of opening, in metres; empty for none (opened by the previous waypoint, always open, or
 *             without opening).
 * @param clear the radius of validation, in metres.
 */
public record Waypoint(WaypointKind kind, OptionalInt open, int clear) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest radius, in metres: OpenRally writes the radii as positive integers. */
  public static final int MIN_RADIUS = 1;
  /**
   * The largest radius, in metres: ten times the largest radius of the FIA (1000 m, §43.1.6), so that a typing error
   * of a digit is refused.
   */
  public static final int MAX_RADIUS = 10_000;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a waypoint.
   *
   * @param kind the kind of the waypoint.
   * @param open the radius of opening, in metres, or empty.
   * @param clear the radius of validation, in metres.
   *
   * @throws DomainException {@link ValidationError#INVALID_WAYPOINT} if a radius is beyond its bounds.
   * @throws NullPointerException if the kind or the radius of opening is {@code null}.
   */
  public Waypoint {
    Objects.requireNonNull(kind);
    Objects.requireNonNull(open);
    if (!isRadius(clear) || open.isPresent() && !isRadius(open.getAsInt())) {
      throw new DomainException(ValidationError.INVALID_WAYPOINT);
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives a waypoint with the radii of the FIA.
   *
   * @param kind the kind of the waypoint.
   *
   * @return the waypoint, its radii those of its kind by default (§43.1.6).
   */
  public static Waypoint of(final WaypointKind kind) {
    return new Waypoint(kind, kind.open(), kind.clear());
  }

  /**
   * Tells whether a value is a radius of a waypoint.
   *
   * @param metres the value, in metres.
   *
   * @return {@code true} if it is from {@value #MIN_RADIUS} to {@value #MAX_RADIUS}.
   */
  public static boolean isRadius(final int metres) {
    return metres >= MIN_RADIUS && metres <= MAX_RADIUS;
  }

}
