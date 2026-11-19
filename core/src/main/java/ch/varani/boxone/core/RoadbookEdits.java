package ch.varani.boxone.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The changes made by the user to a roadbook generated, in its editor (development plan, phase 5): the boxes added at
 * places of the route, the boxes removed, and the corrections of the boxes ({@link BoxEdit}).
 *
 * <p>Every change is attached to a place, not to the number of a box, so that the changes are kept when the route
 * changes and still passes there (decision of the maintainer, 2026-11-14); the roadbook module applies them to a
 * route and to its roadbook, and keeps those that still apply when the route changes. Here, the places are compared
 * exactly: the editor gives the places of the boxes shown.</p>
 *
 * <p><b>Preconditions</b>: three lists, none holding {@code null}, a place corrected at most once. Otherwise the
 * changes are rejected. The lists are copied.</p>
 *
 * <p>Its places are personal data (AGENTS.md §5): the changes are never logged.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param added the places of the route where a box is added, in the order they were added; unmodifiable.
 * @param removed the places of the boxes removed; unmodifiable.
 * @param boxes the corrections of the boxes, at most one per place; unmodifiable.
 */
public record RoadbookEdits(List<GeoPoint> added, List<GeoPoint> removed, List<BoxEdit> boxes) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** No change: the roadbook as generated. */
  public static final RoadbookEdits NONE = new RoadbookEdits(List.of(), List.of(), List.of());

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the changes of a roadbook.
   *
   * @param added the places where a box is added, none {@code null}.
   * @param removed the places of the boxes removed, none {@code null}.
   * @param boxes the corrections of the boxes, none {@code null}, at most one per place.
   *
   * @throws NullPointerException if a list or one of its elements is {@code null}.
   * @throws IllegalArgumentException if two corrections have the same place.
   */
  public RoadbookEdits {
    added = List.copyOf(added);
    removed = List.copyOf(removed);
    boxes = List.copyOf(boxes);
    if (boxes.stream().map(BoxEdit::place).distinct().count() != boxes.size()) {
      throw new IllegalArgumentException("one correction per place");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether there is no change.
   *
   * @return {@code true} if no box is added, removed nor corrected.
   */
  public boolean isEmpty() {
    return added.isEmpty() && removed.isEmpty() && boxes.isEmpty();
  }

  /**
   * Returns the correction of the box at a place.
   *
   * @param place the place of the box.
   *
   * @return its correction, or empty if it has none.
   */
  public Optional<BoxEdit> edit(final GeoPoint place) {
    return boxes.stream().filter(edit -> edit.place().equals(place)).findFirst();
  }

  /**
   * Returns the changes with a box added at a place of the route.
   *
   * @param place the place of the box, on the route.
   *
   * @return the changes with the box added; the same changes if a box is already added there.
   *
   * @throws NullPointerException if the place is {@code null}.
   */
  public RoadbookEdits withAdded(final GeoPoint place) {
    Objects.requireNonNull(place);
    if (added.contains(place)) {
      return this;
    }
    final List<GeoPoint> newAdded = new ArrayList<>(added);
    newAdded.add(place);
    return new RoadbookEdits(newAdded, removed, boxes);
  }

  /**
   * Returns the changes with the box at a place removed, and its correction with it: a box added is no longer added,
   * a box generated is removed.
   *
   * @param place the place of the box.
   *
   * @return the changes with the box removed.
   *
   * @throws NullPointerException if the place is {@code null}.
   */
  public RoadbookEdits withRemoved(final GeoPoint place) {
    Objects.requireNonNull(place);
    final List<BoxEdit> newBoxes = boxes.stream().filter(edit -> !edit.place().equals(place)).toList();
    if (added.contains(place)) {
      return new RoadbookEdits(added.stream().filter(point -> !point.equals(place)).toList(), removed, newBoxes);
    }
    final List<GeoPoint> newRemoved = new ArrayList<>(removed);
    if (!removed.contains(place)) {
      newRemoved.add(place);
    }
    return new RoadbookEdits(added, newRemoved, newBoxes);
  }

  /**
   * Returns the changes with a box moved along the route, dragged on the map: a box added is added at its new place
   * instead, a box generated is removed and added at its new place. Its distances and its heading are those of its
   * new place, computed again: its shift and its heading typed are dropped, measured at the former place, as its tulip
   * corrected; the display of its heading chosen by the user, its notes and its waypoint are kept.
   *
   * @param from the place of the box.
   * @param to its new place, on the route.
   *
   * @return the changes with the box moved.
   *
   * @throws NullPointerException if a place is {@code null}.
   */
  public RoadbookEdits withMoved(final GeoPoint from, final GeoPoint to) {
    final RoadbookEdits moved = withRemoved(from).withAdded(to);
    return edit(from).map(old -> moved.with(new BoxEdit(to).withDisplay(old.display()).withNotes(old.notes())
                                                           .withWaypoint(old.waypoint())))
                     .orElse(moved);
  }

  /**
   * Returns the changes with the correction of a box, replacing its former one; a correction that changes nothing
   * removes the former one.
   *
   * @param edit the correction of the box.
   *
   * @return the changes with the correction.
   *
   * @throws NullPointerException if the correction is {@code null}.
   */
  public RoadbookEdits with(final BoxEdit edit) {
    final List<BoxEdit> newBoxes = new ArrayList<>(boxes.stream()
                                                        .filter(other -> !other.place().equals(edit.place()))
                                                        .toList());
    if (!edit.isEmpty()) {
      newBoxes.add(edit);
    }
    return new RoadbookEdits(added, removed, newBoxes);
  }

}
