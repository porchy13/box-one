package ch.varani.boxone.training;

import ch.varani.boxone.core.WaypointKind;

import java.util.Objects;
import java.util.Optional;

/**
 * A penalty of a simulated run (development plan, phase 7c; asked by the maintainer on 2026-11-21: penalties instead
 * of errors, after the regulations of the FIM): its reason, the box it is given at, its time.
 *
 * <p>The times: a tulip not followed, {@value #WRONG_TULIP} s (a flat penalty asked by the maintainer); a waypoint
 * missed, the penalty of its kind (FIM, 2026 W2RC Sporting Regulations, art. 11.2: {@value #MISSED_WPN} s for a WPN,
 * {@value #MISSED_WPP} s for a WPP, {@value #MISSED} s for the others, DZ and FZ included); an impulse of speeding in a
 * speed zone, after its excess (art. 16: {@value #EXCESS_SMALL} s from 1 to {@value #SMALL} km/h,
 * {@value #EXCESS_MEDIUM} s to {@value #MEDIUM} km/h, {@value #EXCESS_LARGE} s to {@value #LARGE} km/h,
 * {@value #EXCESS_HUGE} s beyond); a fall on an obstacle of the road, {@value #FALL} s (chosen for Box One).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param reason why it is given.
 * @param box the number of the box it is given at: the junction, the waypoint, the start of the zone, the box before
 *            the obstacle.
 * @param seconds its time, in seconds, positive.
 * @param waypoint the kind of the waypoint missed; empty for another reason.
 */
public record Penalty(Reason reason, int box, int seconds, Optional<WaypointKind> waypoint) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The penalty of a tulip not followed, in seconds: 15 minutes, flat (asked by the maintainer on 2026-11-21). */
  public static final int WRONG_TULIP = 15 * 60;
  /** The penalty of a waypoint missed, in seconds: 15 minutes (FIM art. 11.2, WPE, WPM, WPS, WPC, WPV, DZ, FZ). */
  public static final int MISSED = 15 * 60;
  /** The penalty of a WPN missed, in seconds: 60 minutes (FIM art. 11.2). */
  public static final int MISSED_WPN = 60 * 60;
  /** The penalty of a WPP missed, in seconds: 2 minutes (FIM art. 11.2). */
  public static final int MISSED_WPP = 2 * 60;
  /** The largest small excess of speed, in km/h (FIM art. 16: "de 1 à 5 km/h"). */
  public static final int SMALL = 5;
  /** The largest medium excess of speed, in km/h (FIM art. 16: "de 6 à 15 km/h"). */
  public static final int MEDIUM = 15;
  /** The largest large excess of speed, in km/h (FIM art. 16: "de 16 à 40 km/h"). */
  public static final int LARGE = 40;
  /** The penalty of an impulse of a small excess, in seconds (FIM art. 16: "IMP x 10 secondes"). */
  public static final int EXCESS_SMALL = 10;
  /** The penalty of an impulse of a medium excess, in seconds (FIM art. 16: "IMP x 1 minute"). */
  public static final int EXCESS_MEDIUM = 60;
  /** The penalty of an impulse of a large excess, in seconds (FIM art. 16: "IMP X 2'"). */
  public static final int EXCESS_LARGE = 2 * 60;
  /** The penalty of an impulse of an excess beyond {@value #LARGE} km/h, in seconds (FIM art. 16: "IMP X 6'"). */
  public static final int EXCESS_HUGE = 6 * 60;
  /** The penalty of a fall on an obstacle of the road, in seconds: 5 minutes (chosen for Box One). */
  public static final int FALL = 5 * 60;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a penalty.
   *
   * @param reason why it is given.
   * @param box the number of its box.
   * @param seconds its time, positive.
   * @param waypoint the kind of the waypoint missed, or empty.
   *
   * @throws NullPointerException if the reason or the waypoint is {@code null}.
   * @throws IllegalArgumentException if the time is not positive, or a waypoint is given to another reason than a
   *                                  waypoint missed, or none to a waypoint missed.
   */
  public Penalty {
    Objects.requireNonNull(reason);
    Objects.requireNonNull(waypoint);
    if (seconds <= 0 || waypoint.isPresent() != (reason == Reason.MISSED_WAYPOINT)) {
      throw new IllegalArgumentException("invalid penalty");
    }
  }

  /**
   * Gives the penalty of a tulip not followed.
   *
   * @param box the number of the box of the junction.
   *
   * @return the penalty, {@value #WRONG_TULIP} s.
   */
  public static Penalty wrongTulip(final int box) {
    return new Penalty(Reason.WRONG_TULIP, box, WRONG_TULIP, Optional.empty());
  }

  /**
   * Gives the penalty of a waypoint missed.
   *
   * @param box the number of the box of the waypoint.
   * @param kind the kind of the waypoint.
   *
   * @return the penalty of its kind.
   */
  public static Penalty missedWaypoint(final int box, final WaypointKind kind) {
    final int seconds = switch (kind) {
      case WPN -> MISSED_WPN;
      case WPP -> MISSED_WPP;
      default -> MISSED;
    };
    return new Penalty(Reason.MISSED_WAYPOINT, box, seconds, Optional.of(kind));
  }

  /**
   * Gives the penalty of an impulse of speeding.
   *
   * @param box the number of the box of the start of the zone.
   * @param excess the excess of speed, in whole km/h, positive.
   *
   * @return the penalty of the excess.
   *
   * @throws IllegalArgumentException if the excess is not positive.
   */
  public static Penalty speeding(final int box, final int excess) {
    if (excess <= 0) {
      throw new IllegalArgumentException("no excess");
    }
    final int seconds;
    if (excess <= SMALL) {
      seconds = EXCESS_SMALL;
    } else if (excess <= MEDIUM) {
      seconds = EXCESS_MEDIUM;
    } else if (excess <= LARGE) {
      seconds = EXCESS_LARGE;
    } else {
      seconds = EXCESS_HUGE;
    }
    return new Penalty(Reason.SPEEDING, box, seconds, Optional.empty());
  }

  /**
   * Gives the penalty of a fall on an obstacle.
   *
   * @param box the number of the box before the obstacle.
   *
   * @return the penalty, {@value #FALL} s.
   */
  public static Penalty fallOnObstacle(final int box) {
    return new Penalty(Reason.FALL, box, FALL, Optional.empty());
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * Why a penalty is given.
   */
  public enum Reason {

    /** A branch of a junction other than the one of its tulip taken. */
    WRONG_TULIP,
    /** A waypoint not passed within its radius of validation. */
    MISSED_WAYPOINT,
    /** An impulse of speeding in a speed zone. */
    SPEEDING,
    /** A fall on an obstacle of the road passed too fast. */
    FALL

  }

}
