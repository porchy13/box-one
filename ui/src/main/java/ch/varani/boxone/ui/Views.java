package ch.varani.boxone.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * The loading of the views of the user interface: an FXML file of this package, its texts taken from the bundle of
 * the language of the application, and the controller given by the caller.
 *
 * <p>Utility class without state; its methods are called on the JavaFX Application Thread.</p>
 */
final class Views {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Views() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Loads a view.
   *
   * @param view the address of the FXML file of the view.
   * @param texts the texts of the user interface, referenced as {@code %key} by the view.
   * @param controller the controller of the view, whose {@code @FXML} fields and methods it connects.
   *
   * @return the root of the view.
   *
   * @throws UncheckedIOException if the view cannot be read: a defect of the package, never of the user.
   */
  static Parent load(final URL view, final ResourceBundle texts, final Object controller) {
    final FXMLLoader loader = new FXMLLoader(view, texts);
    loader.setController(controller);
    try {
      return loader.load();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
