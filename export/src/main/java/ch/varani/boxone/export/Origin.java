package ch.varani.boxone.export;

import ch.varani.boxone.core.GeoPoint;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * Where the route of a roadbook comes from: the start and the finish clicked by the user, and, for a detour, what it
 * was generated from (issue #5 of GitLab). Written into the roadbook exported, so that it can be opened again and its
 * detour generated again from its seed.
 *
 * <p>The export knows neither the routing engine nor the generator of detours (AGENTS.md §3): the budget and the
 * kinds of road forbidden are kept as the names of their constants ({@code Budget.Unit}, {@code RoadKind}), checked by
 * the user interface when it reads them back.</p>
 *
 * <p>The places are personal data (AGENTS.md §5): an origin is never logged.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public sealed interface Origin {

  /**
   * Returns the start clicked.
   *
   * @return the start, as clicked, before the routing engine places it on a road.
   */
  GeoPoint start();

  /**
   * Returns the finish clicked.
   *
   * @return the finish, as clicked, before the routing engine places it on a road.
   */
  GeoPoint finish();

  /**
   * The fastest route from the start to the finish (development plan, phase 1).
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param start the start clicked.
   * @param finish the finish clicked.
   */
  record Fastest(GeoPoint start, GeoPoint finish) implements Origin {

    /**
     * Creates the origin of a fastest route.
     *
     * @param start the start clicked.
     * @param finish the finish clicked.
     *
     * @throws NullPointerException if a place is {@code null}.
     */
    public Fastest {
      Objects.requireNonNull(start);
      Objects.requireNonNull(finish);
    }

  }

  /**
   * A detour from the start to the finish (development plan, phase 3): the same seed, start, finish, budget, kinds of
   * road forbidden and map give the same detour again.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param start the start clicked.
   * @param finish the finish clicked.
   * @param seed the seed of the random draws of the detour.
   * @param unit the unit of the budget, the name of a constant of {@code Budget.Unit}.
   * @param amount the amount of the budget, in its unit.
   * @param avoided the kinds of road forbidden, the names of constants of {@code RoadKind}; unmodifiable.
   * @param fastest the duration of the fastest route, of which a budget in percent is a share.
   */
  record Detour(GeoPoint start, GeoPoint finish, long seed, String unit, int amount, Set<String> avoided,
                Duration fastest) implements Origin {

    /**
     * Creates the origin of a detour.
     *
     * @param start the start clicked.
     * @param finish the finish clicked.
     * @param seed the seed of the random draws of the detour.
     * @param unit the unit of the budget, the name of a constant of {@code Budget.Unit}.
     * @param amount the amount of the budget, in its unit.
     * @param avoided the kinds of road forbidden, the names of constants of {@code RoadKind}; copied.
     * @param fastest the duration of the fastest route.
     *
     * @throws NullPointerException if a place, the unit, the kinds of road or the duration is {@code null}.
     */
    public Detour {
      Objects.requireNonNull(start);
      Objects.requireNonNull(finish);
      Objects.requireNonNull(unit);
      avoided = Set.copyOf(avoided);
      Objects.requireNonNull(fastest);
    }

  }

}
