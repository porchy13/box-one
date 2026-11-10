package ch.varani.boxone.detour;

import java.io.Serial;

/**
 * Thrown when no detour can be generated within the budget.
 *
 * <p>The exception carries a {@link DetourError}, translated by the user interface. Its message is the name of the
 * error only: it never contains a position (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public final class DetourException extends RuntimeException {

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
  private final DetourError error;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates an exception.
   *
   * @param error the reason of the failure.
   */
  public DetourException(final DetourError error) {
    super(error.name());
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
  public DetourError getError() {
    return error;
  }

}
