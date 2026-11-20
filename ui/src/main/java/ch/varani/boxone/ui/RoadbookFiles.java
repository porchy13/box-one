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
 * The choice of the file of the roadbook to export (File › Export the roadbook…), of the roadbook to open
 * ({@link #opening(ResourceBundle, BiFunction)}, File › Open a roadbook…, issue #5 of GitLab), or of the plain GPX to
 * export ({@link #gpx(ResourceBundle, BiFunction)}, File › Export as GPX…, phase 6), or of the PDF strip to export
 * ({@link #pdf(ResourceBundle, BiFunction)}, File › Export as PDF…, phase 6): the save or the open dialog of the
 * system, limited to the GPX files, the format of OpenRally read by Terra Pirata and of the plain GPX, or to the PDF
 * files. The dialog is
 * shown by a function ({@code FileChooser::showSaveDialog}, {@code FileChooser::showOpenDialog}), replaced in the
 * tests, which run without the dialogs of the system.
 *
 * <p>The file chosen for an export always ends with the extension of its format, {@value #GPX_EXTENSION} or
 * {@value #PDF_EXTENSION}, added if the user typed a name without it;
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
  static final String GPX_EXTENSION = ".gpx";
  /** The beginning of the pattern of the files of an extension. */
  static final String ANY = "*";
  /** The pattern of the GPX files. */
  static final String PATTERN = ANY + GPX_EXTENSION;
  /** The name proposed for the file. */
  static final String INITIAL_NAME = "roadbook" + GPX_EXTENSION;
  /** The key of the title of the dialog of the export of the plain GPX. */
  static final String GPX_TITLE = "gpx.title";
  /** The key of the name of the filter of the plain GPX files. */
  static final String GPX_FILTER = "gpx.filter";
  /** The name proposed for the file of the plain GPX. */
  static final String GPX_NAME = "route" + GPX_EXTENSION;
  /** The key of the title of the dialog of the export of the PDF strip. */
  static final String PDF_TITLE = "pdf.title";
  /** The key of the name of the filter of the PDF files. */
  static final String PDF_FILTER = "pdf.filter";
  /** The extension of the PDF files. */
  static final String PDF_EXTENSION = ".pdf";
  /** The name proposed for the file of the PDF strip. */
  static final String PDF_NAME = "roadbook" + PDF_EXTENSION;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** Shows the dialog to the user, and returns the file chosen, or {@code null} if cancelled. */
  private final BiFunction<FileChooser, Window, File> dialog;
  /** The key of the title of the dialog. */
  private final String titleKey;
  /** The key of the name of the filter of the files. */
  private final String filterKey;
  /** The name proposed for the file of an export, or {@code null} for an opening: then no extension is added. */
  private final String proposed;
  /** The extension of the files: {@value #GPX_EXTENSION} or {@value #PDF_EXTENSION}. */
  private final String extension;

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
    this(texts, dialog, TITLE, FILTER, INITIAL_NAME, GPX_EXTENSION);
  }

  /**
   * Creates the choice of the file, for an export or an opening.
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled.
   * @param titleKey the key of the title of the dialog.
   * @param filterKey the key of the name of the filter of the files.
   * @param proposed the name proposed for the file of an export, or {@code null} for a file to open.
   * @param extension the extension of the files.
   */
  private RoadbookFiles(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog,
                        final String titleKey, final String filterKey, final String proposed,
                        final String extension) {
    this.texts = texts;
    this.dialog = dialog;
    this.titleKey = titleKey;
    this.filterKey = filterKey;
    this.proposed = proposed;
    this.extension = extension;
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
    return new RoadbookFiles(texts, dialog, OPEN_TITLE, FILTER, null, GPX_EXTENSION);
  }

  /**
   * Creates the choice of the file of the plain GPX to export (development plan, phase 6).
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled ({@code FileChooser::showSaveDialog}).
   *
   * @return the choice of the file of the plain GPX.
   */
  static RoadbookFiles gpx(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog) {
    return new RoadbookFiles(texts, dialog, GPX_TITLE, GPX_FILTER, GPX_NAME, GPX_EXTENSION);
  }

  /**
   * Creates the choice of the file of the PDF strip to export (development plan, phase 6).
   *
   * @param texts the texts of the user interface.
   * @param dialog shows the dialog to the user, owned by the main window, and returns the file chosen, or
   *               {@code null} if cancelled ({@code FileChooser::showSaveDialog}).
   *
   * @return the choice of the file of the PDF strip.
   */
  static RoadbookFiles pdf(final ResourceBundle texts, final BiFunction<FileChooser, Window, File> dialog) {
    return new RoadbookFiles(texts, dialog, PDF_TITLE, PDF_FILTER, PDF_NAME, PDF_EXTENSION);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Asks the user for the file of the roadbook.
   *
   * @param owner the main window.
   *
   * @return the file chosen, ending with the extension of its format for an export, or nothing if the user
   *         cancelled.
   */
  @Override
  public Optional<Path> apply(final Window owner) {
    final FileChooser chooser = new FileChooser();
    chooser.setTitle(texts.getString(titleKey));
    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(texts.getString(filterKey), ANY + extension));
    if (proposed == null) {
      return Optional.ofNullable(dialog.apply(chooser, owner)).map(File::toPath);
    }
    chooser.setInitialFileName(proposed);
    return Optional.ofNullable(dialog.apply(chooser, owner)).map(File::toPath)
                   .map(chosen -> withExtension(chosen, extension));
  }

  /**
   * Adds an extension to a file that has not got it, whatever its case.
   *
   * @param file the file chosen.
   * @param extension the extension, in lower case, with its dot.
   *
   * @return the file, ending with the extension.
   */
  static Path withExtension(final Path file, final String extension) {
    final String name = file.getFileName().toString();
    if (name.toLowerCase(Locale.ROOT).endsWith(extension)) {
      return file;
    }
    return file.resolveSibling(name + extension);
  }

  /**
   * Returns the name of a roadbook: the name of its file, without its extension.
   *
   * @param file the file of the roadbook, of an export of it, or opened.
   *
   * @return the name of the file, without what follows its last dot, if any but at its start.
   */
  static String name(final Path file) {
    final String name = file.getFileName().toString();
    final int dot = name.lastIndexOf('.');
    return dot > 0 ? name.substring(0, dot) : name;
  }

}
