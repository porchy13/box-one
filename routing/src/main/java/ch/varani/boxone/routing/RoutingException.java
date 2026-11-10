package ch.varani.boxone.routing;

import java.io.Serial;

/**
 * Thrown when the routing fails: the map cannot be imported, or no route joins the start and the finish.
 *
 * <p>The exception carries a {@link RoutingError}, translated by the user interface. Its message is the name of the
 * error only: it never contains a position (AGENTS.md §5). For the same reason, a failed routing never keeps the
 * exception of the engine, whose message holds the positions; a failed import keeps it, as its message only names
 * the files of the map.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public final class RoutingException extends RuntimeException {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The serial version of the class. */
  @Serial
  private static final long serialVersionUID = 1L;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The reason of the failure. */
  private final RoutingError error;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates an exception without cause, for a failed routing.
   *
   * @param error the reason of the failure.
   */
  public RoutingException(final RoutingError error) {
    super(error.name());
    this.error = error;
  }

  /**
   * Creates an exception with its cause, for a failed import or load of the map.
   *
   * @param error the reason of the failure.
   * @param cause the exception of the engine; its message must not hold a position.
   */
  public RoutingException(final RoutingError error, final Throwable cause) {
    super(error.name(), cause);
    this.error = error;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the reason of the failure.
   *
   * @return the reason of the failure.
   */
  public RoutingError getError() {
    return error;
  }

}
