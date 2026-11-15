package ch.varani.boxone.export;

import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;

import java.util.Objects;

/**
 * A roadbook read back from a file exported by Box One ({@link OpenRallyReader}): its route, where the route comes
 * from (issue #5 of GitLab) and the changes made by the user in the editor of the roadbook (development plan, phase
 * 5). The boxes are not read: they are generated again from the route, and the changes applied to them again.
 *
 * <p>The route and the places are personal data (AGENTS.md §5): never logged.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param route the route of the roadbook, its points those of the track of the file.
 * @param origin the start and the finish clicked, and the detour the route comes from.
 * @param edits the changes of the user, already applied to the route: the boxes added and removed, the corrections.
 */
public record RoadbookFile(Route route, Origin origin, RoadbookEdits edits) {

  /**
   * Creates a roadbook read back.
   *
   * @param route the route of the roadbook.
   * @param origin where the route comes from.
   * @param edits the changes of the user.
   *
   * @throws NullPointerException if the route, the origin or the changes are {@code null}.
   */
  public RoadbookFile {
    Objects.requireNonNull(route);
    Objects.requireNonNull(origin);
    Objects.requireNonNull(edits);
  }

}
