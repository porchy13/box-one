package ch.varani.boxone.catalog;

import java.io.Serial;

/**
 * The failure to read a catalogue of symbols, with its reason.
 *
 * <p>Its message is the code of its reason, without any value of the file.</p>
 *
 * <p>Immutable once thrown, safe for concurrent use.</p>
 */
public final class CatalogueException extends RuntimeException {

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
  private final CatalogueError error;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the failure to read a catalogue.
   *
   * @param error the reason of the failure.
   */
  public CatalogueException(final CatalogueError error) {
    super(error.name());
    this.error = error;
  }

  /**
   * Creates the failure to read a catalogue, caused by another exception.
   *
   * @param error the reason of the failure.
   * @param cause the exception that caused it.
   */
  public CatalogueException(final CatalogueError error, final Throwable cause) {
    super(error.name(), cause);
    this.error = error;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the reason of the failure.
   *
   * @return the reason.
   */
  public CatalogueError getError() {
    return error;
  }

}
