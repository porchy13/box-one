package ch.varani.boxone.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * A line of a list of the elements of a box (its abbreviations, its symbols) that removes its element by a cross at its
 * right (decision of the maintainer, 2026-11-17: the button "Remove" was too far from the list): its icon, if any, its
 * name, then the cross, a button whose accessible text names the element removed.
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class RemovableCell extends ListCell<String> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the accessible text of the cross, with the name of the element removed. */
  static final String REMOVE_TEXT = "notes.removeItem";
  /** The cross of the button, a sign rather than a text: the multiplication sign. */
  static final String CROSS = "×";
  /** The style class of the cross. */
  static final String CROSS_STYLE = "remove-item";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application. */
  private final Locale locale;
  /** The icon of an element by its index, if the elements have icons. */
  private final Optional<IntFunction<Image>> icons;
  /** The name of the element. */
  private final Label name = new Label();
  /** The cross removing the element. */
  private final Button remove = new Button(CROSS);
  /** The line: the icon, the name, the cross. */
  private final HBox line;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a line of a list.
   *
   * @param texts the texts of the user interface.
   * @param locale the locale of the application.
   * @param icons the icon of an element by its index in the list, if the elements have icons.
   * @param removes receives the index of the element removed.
   */
  RemovableCell(final ResourceBundle texts, final Locale locale, final Optional<IntFunction<Image>> icons,
                final IntConsumer removes) {
    this.texts = texts;
    this.locale = locale;
    this.icons = icons;
    remove.getStyleClass().add(CROSS_STYLE);
    remove.setOnAction(event -> removes.accept(getIndex()));
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    line = new HBox(name, space, remove);
    line.getStyleClass().add("removable-line");
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows an element: its icon, its name and its cross; nothing for an empty line.
   *
   * @param item the name of the element, or {@code null}.
   * @param empty whether the line is empty.
   */
  @Override
  protected void updateItem(final String item, final boolean empty) {
    super.updateItem(item, empty);
    if (empty) {
      setGraphic(null);
      return;
    }
    name.setText(item);
    name.setGraphic(icons.map(icon -> new ImageView(icon.apply(getIndex()))).orElse(null));
    remove.setAccessibleText(new MessageFormat(texts.getString(REMOVE_TEXT), locale).format(new Object[] {item}));
    setGraphic(line);
  }

}
