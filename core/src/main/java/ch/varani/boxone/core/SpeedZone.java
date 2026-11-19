package ch.varani.boxone.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The part of a speed zone a box of the roadbook belongs to (development plan, phase 5, increment 5; FIA, 2026
 * Cross-Country Rally Sporting Regulations, §37, and Appendix III, §5.9): its start, at a DZ, which shows the speed
 * limit in its description (§5.9.5), the boxes inside it, framed in the strip (§5.9.1), and its finish, at a FZ, which
 * shows the speed limit struck out (§5.13).
 *
 * <p><b>Algorithm</b> ({@link #along(List)}): the boxes are read in their order; a DZ starts a zone at its speed; a DZ
 * inside another zone finishes it at the same box and starts the new one (§37.2.3: "FZ50 DZ90"; asked by the
 * maintainer, 2026-11-19: a zone at 30 km/h to box 2, then at 40 km/h from box 2), so that its box shows the end of
 * the zone finished and the start of the new one; a FZ finishes the zone it is in; the boxes between are inside the
 * zone; a FZ outside any zone, and the boxes outside any zone, have no part.</p>
 *
 * <p><b>Preconditions</b>: speeds from {@value Waypoint#MIN_SPEED} to {@value Waypoint#MAX_SPEED} km/h, a part, and a
 * zone finished for a start only. Otherwise the zone is rejected.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param speed the speed limit of the zone, in km/h.
 * @param part the part of the zone the box is.
 * @param finished the speed limit of the zone the start finishes at the same box, in km/h; empty for none.
 */
public record SpeedZone(int speed, Part part, OptionalInt finished) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the part of a speed zone.
   *
   * @param speed the speed limit of the zone, in km/h.
   * @param part the part of the zone the box is.
   * @param finished the speed limit of the zone finished at the start, in km/h, or empty.
   *
   * @throws DomainException {@link ValidationError#INVALID_WAYPOINT} if a speed is beyond its bounds, or a zone
   *                         finished is given to another part than a start.
   * @throws NullPointerException if the part or the zone finished is {@code null}.
   */
  public SpeedZone {
    Objects.requireNonNull(part);
    Objects.requireNonNull(finished);
    if (!Waypoint.isSpeed(speed)
        || finished.isPresent() && (part != Part.START || !Waypoint.isSpeed(finished.getAsInt()))) {
      throw new DomainException(ValidationError.INVALID_WAYPOINT);
    }
  }

  /**
   * Creates the part of a speed zone that finishes no other zone.
   *
   * @param speed the speed limit of the zone, in km/h.
   * @param part the part of the zone the box is.
   *
   * @throws DomainException {@link ValidationError#INVALID_WAYPOINT} if the speed is beyond its bounds.
   * @throws NullPointerException if the part is {@code null}.
   */
  public SpeedZone(final int speed, final Part part) {
    this(speed, part, OptionalInt.empty());
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the part of a speed zone of each box of a roadbook.
   *
   * @param boxes the boxes of the roadbook, in their order.
   *
   * @return the part of each box, in the same order; empty for a box outside any zone.
   */
  public static List<Optional<SpeedZone>> along(final List<Box> boxes) {
    final List<Optional<SpeedZone>> zones = new ArrayList<>(boxes.size());
    OptionalInt speed = OptionalInt.empty();
    for (final Box box : boxes) {
      final Optional<WaypointKind> kind = box.waypoint().map(Waypoint::kind);
      if (kind.filter(WaypointKind::hasSpeed).isPresent()) {
        final OptionalInt finished = speed;
        speed = box.waypoint().get().speed();
        zones.add(Optional.of(new SpeedZone(speed.getAsInt(), Part.START, finished)));
      } else if (speed.isEmpty()) {
        zones.add(Optional.empty());
      } else if (kind.filter(WaypointKind.FZ::equals).isPresent()) {
        zones.add(Optional.of(new SpeedZone(speed.getAsInt(), Part.FINISH)));
        speed = OptionalInt.empty();
      } else {
        zones.add(Optional.of(new SpeedZone(speed.getAsInt(), Part.INSIDE)));
      }
    }
    return List.copyOf(zones);
  }

  /**
   * Tells whether the box shows the speed limit in its description: the start and the finish of the zone.
   *
   * @return {@code true} for the start and the finish.
   */
  public boolean isSigned() {
    return part != Part.INSIDE;
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * A part of a speed zone.
   *
   * <p>Immutable, safe for concurrent use.</p>
   */
  public enum Part {

    /** The box of the DZ: the speed limit in its description. */
    START,
    /** A box between the DZ and the FZ. */
    INSIDE,
    /** The box of the FZ: the speed limit struck out in its description. */
    FINISH

  }

}
