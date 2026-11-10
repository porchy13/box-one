package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Views}: a view that cannot be read is reported as a defect of the package.
 */
final class ViewsTest {

  /**
   * A missing view throws an {@link UncheckedIOException}.
   *
   * @param folder a temporary folder, without the view.
   *
   * @throws MalformedURLException never: the address of a path is valid.
   */
  @Test
  @DisplayName("A view that cannot be read is reported")
  void missingTest(@TempDir final Path folder) throws MalformedURLException {
    final URL missing = folder.resolve("missing.fxml").toUri().toURL();
    final ResourceBundle texts = Languages.bundle(Languages.ENGLISH);
    final Object controller = new Object();
    assertThrows(UncheckedIOException.class, () -> Views.load(missing, texts, controller));
  }

}
