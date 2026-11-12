package ch.varani.boxone.export;

/**
 * The reasons why a roadbook cannot be read back, carried by a {@link ReadException}.
 *
 * <p>The user interface translates each code (key {@code error.read.<code>} of its bundles of texts); the code never
 * contains a value of the file, so that it can be logged safely (AGENTS.md §5).</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum ReadError {

  /** The file cannot be read: missing, or not readable by the user. */
  UNREADABLE,
  /** The file is larger than {@value OpenRallyReader#MAX_SIZE} bytes. */
  TOO_LARGE,
  /** The file is not well-formed XML in UTF-8, or holds a document type declaration (AGENTS.md §5). */
  MALFORMED,
  /** The file is not a roadbook exported by Box One: not GPX, or without the route of Box One. */
  NOT_BOX_ONE,
  /** The file was written by a newer version of Box One, in a version of the format this one does not know. */
  NEWER_FORMAT,
  /** The route of Box One in the file is incomplete, or holds a value out of its bounds. */
  INVALID

}
