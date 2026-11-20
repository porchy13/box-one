package ch.varani.boxone.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Saves the file of an export atomically (AGENTS.md §5): the content is written into a temporary file of the folder of
 * the file chosen, then moved in place, so that a failed export never leaves a damaged file. Shared by the exports of
 * the roadbook: OpenRally ({@link OpenRallyWriter}) and the plain GPX ({@link GpxWriter}).
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class AtomicFile {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The suffix of the temporary file of a save. */
  static final String TEMPORARY_SUFFIX = ".tmp";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private AtomicFile() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Saves a content into a file, replacing it if it exists: written into a temporary file of the same folder, then
   * moved in place; the temporary file is deleted if the content cannot be written.
   *
   * @param file the file.
   * @param content writes the content into a stream, left open.
   *
   * @throws IOException if the file cannot be written; it is then left as it was.
   */
  static void save(final Path file, final Content content) throws IOException {
    final Path target = file.toAbsolutePath();
    final Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(),
                                                TEMPORARY_SUFFIX);
    try {
      try (OutputStream output = Files.newOutputStream(temporary)) {
        content.writeTo(output);
      }
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (final IOException e) {
      Files.deleteIfExists(temporary);
      throw e;
    }
  }

  // *******************************************************************************************************************
  // Inner types
  // *******************************************************************************************************************

  /**
   * The content of a file, written into a stream.
   */
  @FunctionalInterface
  interface Content {

    /**
     * Writes the content into a stream, left open.
     *
     * @param output the stream.
     *
     * @throws IOException if the stream cannot be written.
     */
    void writeTo(OutputStream output) throws IOException;

  }

}
