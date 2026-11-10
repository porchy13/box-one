package ch.varani.boxone.ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The choice of the extract of OpenStreetMap to import (File › Open a map…): the file dialog of the system, limited to
 * the PBF files, the format of the extracts of Geofabrik. The dialog is shown by a function
 * ({@code FileChooser::showOpenDialog}), replaced in the tests, which run without the dialogs of the system.
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class MapFiles implements Function<Window, Optional<Path>> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the dialog. */
  static final String TITLE = "chooser.title";
  /** The key of the name of the filter of the PBF files. */
  static final String FILTER = "chooser.filter";
  /** The pattern of the PBF files. */
  static final String PBF = "*.pbf";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** Shows the dialog to the user, and returns the file chosen, or {@code null} if cancelled. */
  private final BiFunction<FileChooser, Window, File> dialog;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the choice of the extract.
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled ({@code FileChooser::showOpenDialog}).
   */
  MapFiles(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog) {
    this.texts = texts;
    this.dialog = dialog;
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Asks the user for an extract of OpenStreetMap.
   *
   * @param owner the main window.
   *
   * @return the file chosen, or nothing if the user cancelled.
   */
  @Override
  public Optional<Path> apply(final Window owner) {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle(texts.getString(TITLE));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(texts.getString(FILTER), PBF));
    return Optional.ofNullable(dialog.apply(chooser, owner)).map(File::toPath);
  }

}
