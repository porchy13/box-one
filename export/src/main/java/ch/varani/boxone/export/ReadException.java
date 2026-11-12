package ch.varani.boxone.export;

import java.io.Serial;

/**
 * The failure to read back a roadbook ({@link OpenRallyReader}), with its reason.
 *
 * <p>Its message is the name of its code, without any value of the file (AGENTS.md §5). For the same reason, it never
 * keeps the exception of the parser or of the conversion of a number, whose message may quote the file (a place of
 * the route).</p>
 *
 * <p>Immutable once created, safe for concurrent use.</p>
 */
public final class ReadException extends Exception {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The version of the serialized form. */
  @Serial
  private static final long serialVersionUID = 1L;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The reason of the failure. */
  private final ReadError error;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the failure.
   *
   * @param error the reason of the failure.
   */
  public ReadException(final ReadError error) {
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
  public ReadError getError() {
    return error;
  }

}
