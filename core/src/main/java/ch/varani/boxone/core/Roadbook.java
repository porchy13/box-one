package ch.varani.boxone.core;

import java.util.List;
import java.util.Objects;

/**
 * A roadbook: the boxes read by the rider along a route, from the box of the start to the box of the finish
 * (development plan, phase 2), and the route itself, whose line is exported with them.
 *
 * <p><b>Preconditions</b>: a route; at least {@value #MIN_BOXES} boxes, numbered from {@value Box#FIRST} in their
 * order. Otherwise the roadbook is rejected. The list of the boxes is copied: later changes of the list given do not
 * change the roadbook.</p>
 *
 * <p>Its route and its boxes are personal data (AGENTS.md §5): a roadbook is never logged.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param route the route of the roadbook.
 * @param boxes the boxes, from the start to the finish; unmodifiable.
 */
public record Roadbook(Route route, List<Box> boxes) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest number of boxes of a roadbook: the box of the start and the box of the finish. */
  public static final int MIN_BOXES = 2;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a roadbook.
   *
   * @param route the route of the roadbook.
   * @param boxes the boxes, from the start to the finish, at least {@value #MIN_BOXES}, numbered from
   *              {@value Box#FIRST} in their order, none {@code null}.
   *
   * @throws NullPointerException if the route, the list or one of its boxes is {@code null}.
   * @throws DomainException {@link ValidationError#TOO_FEW_BOXES} if the roadbook has fewer than
   *                         {@value #MIN_BOXES} boxes, {@link ValidationError#INVALID_BOX_NUMBER} if a box is not
   *                         numbered after its rank.
   */
  public Roadbook {
    Objects.requireNonNull(route);
    boxes = List.copyOf(boxes);
    if (boxes.size() < MIN_BOXES) {
      throw new DomainException(ValidationError.TOO_FEW_BOXES);
    }
    for (int i = 0; i < boxes.size(); i++) {
      if (boxes.get(i).number() != Box.FIRST + i) {
        throw new DomainException(ValidationError.INVALID_BOX_NUMBER);
      }
    }
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the length of the roadbook: the total distance of its last box, the box of the finish.
   *
   * @return the length of the roadbook, in metres.
   */
  public double distance() {
    return boxes.getLast().total();
  }

}
