package ch.varani.boxone.export;

import ch.varani.boxone.core.Route;

import java.util.Objects;

/**
 * A roadbook read back from a file exported by Box One ({@link OpenRallyReader}): its route and where the route comes
 * from (issue #5 of GitLab). The boxes are not read: they are generated again from the route.
 *
 * <p>The route and the places are personal data (AGENTS.md §5): never logged.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param route the route of the roadbook, its points those of the track of the file.
 * @param origin the start and the finish clicked, and the detour the route comes from.
 */
public record RoadbookFile(Route route, Origin origin) {

  /**
   * Creates a roadbook read back.
   *
   * @param route the route of the roadbook.
   * @param origin where the route comes from.
   *
   * @throws NullPointerException if the route or the origin is {@code null}.
   */
  public RoadbookFile {
    Objects.requireNonNull(route);
    Objects.requireNonNull(origin);
  }

}
