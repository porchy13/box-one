package ch.varani.boxone.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;
import java.util.ResourceBundle;

/**
 * The terms of use, shown at each start, as on the GPS (issue #7 of GitLab): the routes are generated from
 * OpenStreetMap, whose data are incomplete or wrong in places, and may take roads that are forbidden, closed or
 * dangerous; only the signs, the rules of the road and the local laws are binding; the user alone is responsible for
 * the use of the routes and of the roadbooks, shared ones included, and the development team accepts no liability
 * for a misuse of the software, the roadbooks generated or the data used (decision of the maintainer).
 *
 * <p><b>Look</b> (DESIGN.md §4, §14): a title, then three short paragraphs — the data, the rules, the responsibility
 * — larger than the body (1.15em) and spaced (line spacing 1.5), so that the terms are read rather than closed.</p>
 *
 * <p><b>Rules</b> (DESIGN.md §7): a modal dialog, since the user must decide before using the map, at each start,
 * without "do not show again", with two buttons of explicit labels: "I accept" keeps the application open, "Refuse
 * and quit" quits it. Closing the dialog otherwise — the close button of its window, the key Escape — is the refusal,
 * the button of cancellation of the dialog.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class StartupNotice {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the title of the dialog. */
  static final String TITLE = "notice.title";
  /** The keys of the paragraphs of the terms, in their order: the data, the rules, the responsibility. */
  static final List<String> PARAGRAPHS = List.of("notice.data", "notice.rules", "notice.responsibility");
  /** The style class of the content of the warning (components.css). */
  private static final String STYLE = "notice";
  /** The style class of a paragraph of the warning (components.css): larger than the body, spaced. */
  private static final String PARAGRAPH_STYLE = "notice-text";
  /** The style class of the title of the warning (base.css): the title of a view. */
  private static final String TITLE_STYLE = "title-view";
  /** The key of the button that accepts the terms. */
  static final String ACCEPT = "notice.accept";
  /** The key of the button that refuses the terms and quits. */
  static final String REFUSE = "notice.refuse";
  /**
   * The width of the text of the terms, in pixels: lines of about 65 characters at its size (DESIGN.md §4: 80 at
   * most); without it, the dialog would take the width of each paragraph on one line.
   */
  static final double TEXT_WIDTH = 480;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The dialog. */
  private final Dialog<ButtonType> dialog = new Dialog<>();

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the terms of use; they are shown by {@code getDialog().show()}.
   *
   * @param owner the main window.
   * @param texts the texts of the user interface.
   * @param stylesheets the style sheets of the theme of the main window.
   * @param quit quits the application, if the terms are refused.
   */
  StartupNotice(final Window owner, final ResourceBundle texts, final List<String> stylesheets, final Runnable quit) {
    dialog.initOwner(owner);
    dialog.setTitle(texts.getString(TITLE));
    final DialogPane pane = dialog.getDialogPane();
    pane.getStylesheets().setAll(stylesheets);
    final ButtonType accept = new ButtonType(texts.getString(ACCEPT), ButtonBar.ButtonData.OK_DONE);
    pane.getButtonTypes().setAll(accept,
                                 new ButtonType(texts.getString(REFUSE), ButtonBar.ButtonData.CANCEL_CLOSE));
    final VBox content = new VBox(paragraph(texts.getString(TITLE), TITLE_STYLE));
    PARAGRAPHS.forEach(key -> content.getChildren().add(paragraph(texts.getString(key), PARAGRAPH_STYLE)));
    content.getStyleClass().add(STYLE);
    pane.setContent(content);
    dialog.setOnHidden(event -> {
      if (dialog.getResult() != accept) {
        quit.run();
      }
    });
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Creates a paragraph of the terms, wrapped at {@value #TEXT_WIDTH} pixels.
   *
   * @param text the text of the paragraph.
   * @param style the style class of the paragraph: the title or the text.
   *
   * @return the paragraph.
   */
  private static Label paragraph(final String text, final String style) {
    final Label paragraph = new Label(text);
    paragraph.getStyleClass().add(style);
    paragraph.setWrapText(true);
    paragraph.setPrefWidth(TEXT_WIDTH);
    paragraph.setMinHeight(Region.USE_PREF_SIZE);
    return paragraph;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Returns the dialog.
   *
   * @return the dialog.
   */
  Dialog<ButtonType> getDialog() {
    return dialog;
  }

}
