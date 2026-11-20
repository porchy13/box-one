package ch.varani.boxone.core;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A waypoint of a box of the roadbook (development plan, phase 5, increment 4; FIA, 2026 Cross-Country Rally Sporting
 * Regulations, §43, and Appendix III, §5.12.5: every waypoint is assigned to a box): its kind and its radii of opening
 * and of validation, those of the FIA by default ({@link #of(WaypointKind)}), changed by the user (decision of the
 * maintainer, 2026-11-18); the start of a speed zone (DZ, increment 5) carries its speed limit, any whole number of
 * km/h from {@value #MIN_SPEED} to {@value #MAX_SPEED} (decision of the maintainer, 2026-11-19: to train on the roads
 * open to traffic, where the FIA allows 30, 50 or 90 km/h only, §37.2.2), {@value #DEFAULT_SPEED} by default.
 *
 * <p><b>Preconditions</b>: a kind; a radius of opening, when present, and a radius of validation from
 * {@value #MIN_RADIUS} to {@value #MAX_RADIUS} metres; a speed from {@value #MIN_SPEED} to {@value #MAX_SPEED} km/h for
 * a DZ, none for the other kinds ({@link WaypointKind#hasSpeed()}). Otherwise the waypoint is rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param kind the kind of the waypoint.
 * @param open the radius of opening, in metres; empty for none (opened by the previous waypoint, always open, or
 *             without opening).
 * @param clear the radius of validation, in metres.
 * @param speed the speed limit of the zone it starts, in km/h; empty for the kinds without speed.
 */
public record Waypoint(WaypointKind kind, OptionalInt open, int clear, OptionalInt speed) {

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
  /** The lowest speed limit, in km/h: below the 20 km/h of the meeting zones of Switzerland, the slowest on a road. */
  public static final int MIN_SPEED = 10;
  /** The highest speed limit, in km/h: the highest limit of Europe on a road but the motorways of Germany. */
  public static final int MAX_SPEED = 130;
  /** The speed limit of a DZ by default, in km/h: one of the FIA (§37.2.2), the limit of the towns of Europe. */
  public static final int DEFAULT_SPEED = 50;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a waypoint.
   *
   * @param kind the kind of the waypoint.
   * @param open the radius of opening, in metres, or empty.
   * @param clear the radius of validation, in metres.
   * @param speed the speed limit of a DZ, in km/h; empty for the other kinds.
   *
   * @throws DomainException {@link ValidationError#INVALID_WAYPOINT} if a radius or the speed is beyond its bounds, or
   *                         if the speed is present for a kind without speed or missing for a DZ.
   * @throws NullPointerException if the kind, the radius of opening or the speed is {@code null}.
   */
  public Waypoint {
    Objects.requireNonNull(kind);
    Objects.requireNonNull(open);
    Objects.requireNonNull(speed);
    if (!isRadius(clear) || open.isPresent() && !isRadius(open.getAsInt()) || speed.isPresent() != kind.hasSpeed()
        || speed.isPresent() && !isSpeed(speed.getAsInt())) {
      throw new DomainException(ValidationError.INVALID_WAYPOINT);
    }
  }

  /**
   * Creates a waypoint with the speed of its kind by default: {@value #DEFAULT_SPEED} km/h for a DZ, none otherwise.
   *
   * @param kind the kind of the waypoint.
   * @param open the radius of opening, in metres, or empty.
   * @param clear the radius of validation, in metres.
   *
   * @throws DomainException {@link ValidationError#INVALID_WAYPOINT} if a radius is beyond its bounds.
   * @throws NullPointerException if the kind or the radius of opening is {@code null}.
   */
  public Waypoint(final WaypointKind kind, final OptionalInt open, final int clear) {
    this(kind, open, clear, kind.hasSpeed() ? OptionalInt.of(DEFAULT_SPEED) : OptionalInt.empty());
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives a waypoint with the radii of the FIA.
   *
   * @param kind the kind of the waypoint.
   *
   * @return the waypoint, its radii those of its kind by default (§43.1.6), a DZ at {@value #DEFAULT_SPEED} km/h.
   */
  public static Waypoint of(final WaypointKind kind) {
    return new Waypoint(kind, kind.open(), kind.clear());
  }

  /**
   * Gives a waypoint with the radii of the FIA, a DZ at the speed limit of the map at its box (phase 5, increment 5b;
   * decision of the maintainer, 2026-11-20: the limits of the map are an information for the user, which makes the
   * placing of a zone easier, never a zone placed automatically).
   *
   * @param kind the kind of the waypoint.
   * @param limit the speed limit of the map where the route leaves the box, in km/h, or empty if the map gives none.
   *
   * @return the waypoint, its radii those of its kind by default (§43.1.6), a DZ at the limit of the map if it is from
   *         {@value #MIN_SPEED} to {@value #MAX_SPEED} km/h, at {@value #DEFAULT_SPEED} km/h otherwise.
   */
  public static Waypoint of(final WaypointKind kind, final OptionalInt limit) {
    if (!kind.hasSpeed() || limit.isEmpty() || !isSpeed(limit.getAsInt())) {
      return of(kind);
    }
    return new Waypoint(kind, kind.open(), kind.clear(), limit);
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

  /**
   * Tells whether a value is a speed limit of a zone.
   *
   * @param kilometresPerHour the value, in km/h.
   *
   * @return {@code true} if it is from {@value #MIN_SPEED} to {@value #MAX_SPEED}.
   */
  public static boolean isSpeed(final int kilometresPerHour) {
    return kilometresPerHour >= MIN_SPEED && kilometresPerHour <= MAX_SPEED;
  }

}
