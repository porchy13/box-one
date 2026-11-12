package ch.varani.boxone.ui;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The choice of the file of the roadbook to export (File › Export the roadbook…), or of the roadbook to open
 * ({@link #opening(ResourceBundle, BiFunction)}, File › Open a roadbook…, issue #5 of GitLab): the save or the open
 * dialog of the system, limited to the GPX files, the format of OpenRally read by Terra Pirata. The dialog is shown by
 * a function ({@code FileChooser::showSaveDialog}, {@code FileChooser::showOpenDialog}), replaced in the tests, which
 * run without the dialogs of the system.
 *
 * <p>The file chosen for an export always ends with {@value #EXTENSION}, added if the user typed a name without it;
 * the name of the file, without its extension, is the name of the roadbook ({@link #name(Path)}). The file chosen to
 * be opened is returned as it is.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class RoadbookFiles implements Function<Window, Optional<Path>> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the dialog of the export. */
  static final String TITLE = "export.title";
  /** The key of the title of the dialog of the opening. */
  static final String OPEN_TITLE = "open.title";
  /** The key of the name of the filter of the GPX files. */
  static final String FILTER = "export.filter";
  /** The extension of the files of the roadbooks. */
  static final String EXTENSION = ".gpx";
  /** The pattern of the GPX files. */
  static final String GPX = "*" + EXTENSION;
  /** The name proposed for the file. */
  static final String INITIAL_NAME = "roadbook" + EXTENSION;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** Shows the dialog to the user, and returns the file chosen, or {@code null} if cancelled. */
  private final BiFunction<FileChooser, Window, File> dialog;
  /** Whether the file is chosen for an export: a name proposed, the extension added. */
  private final boolean export;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the choice of the file.
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled ({@code FileChooser::showSaveDialog}).
   */
  RoadbookFiles(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog) {
    this(texts, dialog, true);
  }

  /**
   * Creates the choice of the file, for an export or an opening.
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled.
   * @param export whether the file is chosen for an export, otherwise to be opened.
   */
  private RoadbookFiles(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog,
                        final boolean export) {
    this.texts = texts;
    this.dialog = dialog;
    this.export = export;
  }

  /**
   * Creates the choice of the roadbook to open.
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled ({@code FileChooser::showOpenDialog}).
   *
   * @return the choice of the roadbook to open.
   */
  static RoadbookFiles opening(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog) {
    return new RoadbookFiles(texts, dialog, false);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Asks the user for the file of the roadbook.
   *
   * @param owner the main window.
   *
   * @return the file chosen, ending with {@value #EXTENSION} for an export, or nothing if the user cancelled.
   */
  @Override
  public Optional<Path> apply(final Window owner) {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle(texts.getString(export ? TITLE : OPEN_TITLE));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(texts.getString(FILTER), GPX));
    if (export) {
      chooser.setInitialFileName(INITIAL_NAME);
    }
    final Optional<Path> chosen = Optional.ofNullable(dialog.apply(chooser, owner)).map(File::toPath);
    return export ? chosen.map(RoadbookFiles::withExtension) : chosen;
  }

  /**
   * Adds the extension {@value #EXTENSION} to a file that has not got it, whatever its case.
   *
   * @param file the file chosen.
   *
   * @return the file, ending with {@value #EXTENSION}.
   */
  static Path withExtension(final Path file) {
    final String name = file.getFileName().toString();
    if (name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
      return file;
    }
    return file.resolveSibling(name + EXTENSION);
  }

  /**
   * Returns the name of a roadbook: the name of its file, without its extension.
   *
   * @param file the file of the roadbook, ending with {@value #EXTENSION}.
   *
   * @return the name of the roadbook.
   */
  static String name(final Path file) {
    final String name = file.getFileName().toString();
    return name.substring(0, name.length() - EXTENSION.length());
  }

}
