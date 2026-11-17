package ch.varani.boxone.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A box of the roadbook strip, drawn as on paper (DESIGN.md §14): at the left, the number of the box, small, to find
 * it again (asked by the maintainer, 2026-11-14), then the total distance, large, and the partial distance under it,
 * small, both right-aligned; in the middle, the diagram (the tulip, the symbols and the danger mark of the notes); at
 * the right, the heading where the catalogue shows it, and the description under it (the abbreviations and the free
 * text of the notes, phase 5). The box has its number, its distances and its notes as accessible text.
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class BoxCell extends ListCell<BoxRow> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The side of the tulip in the strip, in pixels: half the image of the catalogue, as on a printed strip. */
  static final double TULIP_SIDE = 100;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The number of the box. */
  private final Label number = new Label();
  /** The total distance. */
  private final Label total = new Label();
  /** The partial distance. */
  private final Label partial = new Label();
  /** The diagram: the tulip and the notes drawn on it. */
  private final ImageView tulip = new ImageView();
  /** The heading. */
  private final Label heading = new Label();
  /** The description: the abbreviations and the free text. */
  private final Label notes = new Label();
  /** The box: the number, the distances, the diagram, the heading and the description in a row. */
  private final HBox row;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a cell of the strip.
   */
  BoxCell() {
    number.getStyleClass().add("box-number");
    total.getStyleClass().add("box-total");
    partial.getStyleClass().add("box-partial");
    heading.getStyleClass().add("box-heading");
    notes.getStyleClass().add("box-notes");
    notes.setWrapText(true);
    tulip.setFitWidth(TULIP_SIDE);
    tulip.setFitHeight(TULIP_SIDE);
    tulip.setPreserveRatio(true);
    tulip.setSmooth(true);
    final VBox distances = new VBox(total, partial);
    distances.getStyleClass().add("box-distances");
    distances.setAlignment(Pos.TOP_RIGHT);
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    final VBox description = new VBox(heading, notes);
    description.getStyleClass().add("box-description");
    description.setAlignment(Pos.TOP_RIGHT);
    row = new HBox(number, distances, tulip, space, description);
    row.getStyleClass().add("box-paper");
    getStyleClass().add("box-cell");
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows a box, or nothing for an empty cell.
   *
   * @param box the box, or {@code null}.
   * @param empty whether the cell is empty.
   */
  @Override
  protected void updateItem(final BoxRow box, final boolean empty) {
    super.updateItem(box, empty);
    if (empty) {
      setGraphic(null);
      setAccessibleText(null);
      return;
    }
    number.setText(String.valueOf(box.number()));
    total.setText(box.total());
    partial.setText(box.partial());
    tulip.setImage(box.tulip().orElse(null));
    heading.setText(box.heading().orElse(""));
    notes.setText(box.notes());
    setAccessibleText(box.description());
    setGraphic(row);
  }

}
