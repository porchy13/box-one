package ch.varani.boxone.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;

/**
 * The parts of the pages of the training mode (development plan, phase 7; DESIGN.md §14, Training), shared by its
 * exercises: the quiz of symbols ({@link TrainingView}) first.
 *
 * <p>Utility class without state; the nodes it makes are confined to the JavaFX Application Thread.</p>
 */
final class Pages {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The style class of the view of the training. */
  static final String STYLE = "training";
  /** The style class of a page of the training. */
  static final String PAGE_STYLE = "quiz-page";
  /** The style class of the row of the buttons of a page. */
  static final String BUTTONS_STYLE = "quiz-buttons";
  /** The style class of a view title (DESIGN.md §4, base.css). */
  static final String VIEW_TITLE = "title-view";
  /** The style class of a panel title (DESIGN.md §4, base.css). */
  static final String PANEL_TITLE = "title-panel";
  /** The style class of a section header (DESIGN.md §4, base.css). */
  static final String SECTION_HEAD = "section-head";
  /** The style class of a caption (DESIGN.md §4, base.css). */
  static final String CAPTION = "caption";
  /** The style class of a text telling an error, in the colour of danger (base.css). */
  static final String ERROR = "error";
  /** The style class of a text telling a success, in the colour of success (base.css). */
  static final String SUCCESS = "success";
  /** The style class of the primary button of a page (DESIGN.md §7, base.css). */
  static final String PRIMARY = "primary";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Pages() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Makes an image of pixels.
   *
   * @param pixels the colours of the pixels, row by row from the top left, in ARGB.
   * @param side the side of the image, in pixels.
   *
   * @return the image.
   */
  static ImageView image(final int[] pixels, final int side) {
    final WritableImage image = new WritableImage(side, side);
    image.getPixelWriter().setPixels(0, 0, side, side, PixelFormat.getIntArgbInstance(), pixels, 0, side);
    return new ImageView(image);
  }

  /**
   * Lays buttons side by side.
   *
   * @param buttons the buttons, the primary one first.
   *
   * @return their row.
   */
  static HBox buttons(final Button... buttons) {
    final HBox row = new HBox(buttons);
    row.getStyleClass().add(BUTTONS_STYLE);
    return row;
  }

  /**
   * Makes a label whose text wraps.
   *
   * @param text the text.
   *
   * @return the label.
   */
  static Label wrapped(final String text) {
    final Label label = new Label(text);
    label.setWrapText(true);
    label.setMnemonicParsing(false);
    return label;
  }

}
