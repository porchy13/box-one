package ch.varani.boxone.routing;

import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.GeoPoint;

import java.util.List;
import java.util.Objects;

/**
 * A road of the map around a place (development plan, phase 7c; asked by the maintainer on 2026-11-21: the branches of
 * a junction of the simulated run drawn as the map draws them, the tracks in a colour of their own): its line and its
 * kind, a tarmac road or a track, as a tulip draws it ({@link Router#roads(GeoPoint, double)}).
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param line the points of the road, two at least, in the order of the map.
 * @param kind its kind.
 */
public record MapRoad(List<GeoPoint> line, BranchKind kind) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a road, its line copied.
   *
   * @param line its points.
   * @param kind its kind.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws IllegalArgumentException if the line has fewer than two points.
   */
  public MapRoad {
    line = List.copyOf(line);
    Objects.requireNonNull(kind);
    if (line.size() < 2) {
      throw new IllegalArgumentException("two points at least");
    }
  }

}
