package ch.varani.boxone.training;

/**
 * A speed zone of a simulated run (development plan, phase 7c; asked by the maintainer on 2026-11-21: the speed zones
 * respected with a key): from its DZ to its FZ, or to the DZ of the next zone, or to the finish.
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param box the number of the box of its DZ.
 * @param speed its speed limit, in km/h.
 * @param start the distance of its DZ along the road, in metres.
 * @param finish the distance where it finishes, in metres, beyond its start.
 */
public record RideZone(int box, int speed, double start, double finish) {

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether a distance is in the zone.
   *
   * @param distance the distance along the road, in metres.
   *
   * @return {@code true} from its start included to its finish excluded.
   */
  public boolean contains(final double distance) {
    return distance >= start && distance < finish;
  }

}
