package ch.varani.boxone.training;

import ch.varani.boxone.core.Waypoint;

import java.util.Objects;

/**
 * A waypoint of a simulated run (development plan, phase 7c; asked by the maintainer on 2026-11-21: the waypoints
 * validated with their radii, drawn in the scene): the waypoint of a box, where the box is on the road.
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param box the number of its box.
 * @param number its number among the waypoints of the roadbook, from 1, as the NAV-GPS counts them.
 * @param waypoint the waypoint: its kind and its radii.
 * @param distance the distance of its box along the road, in metres.
 * @param east the metres east of the start of the run.
 * @param north the metres north of the start of the run.
 */
public record RideWaypoint(int box, int number, Waypoint waypoint, double distance, double east, double north) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a waypoint of the run.
   *
   * @param box the number of its box.
   * @param number its number among the waypoints.
   * @param waypoint the waypoint.
   * @param distance its distance along the road.
   * @param east its metres east.
   * @param north its metres north.
   *
   * @throws NullPointerException if the waypoint is {@code null}.
   */
  public RideWaypoint {
    Objects.requireNonNull(waypoint);
  }

}
