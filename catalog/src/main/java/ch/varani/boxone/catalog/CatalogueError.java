package ch.varani.boxone.catalog;

/**
 * The reasons why a catalogue of symbols cannot be read, carried by a {@link CatalogueException}.
 *
 * <p>The catalogues are files of the application: a failure is a defect of the application, caught by the tests and
 * the smoke test, not a message for the user. The code never contains a value of the file.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum CatalogueError {

  /** A file that cannot be read, larger than a catalogue may be, or that is not JSON. */
  UNREADABLE,
  /** A catalogue of a newer format than this version of the application reads. */
  NEWER_VERSION,
  /** A catalogue whose content is missing, of the wrong type, or beyond its bounds. */
  INVALID

}
