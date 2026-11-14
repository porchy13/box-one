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
 * small, both right-aligned; in the middle, the tulip; at the right, the heading where the catalogue shows it. The box
 * has its number and its distances as accessible text.
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
  /** The tulip. */
  private final ImageView tulip = new ImageView();
  /** The heading. */
  private final Label heading = new Label();
  /** The box: the number, the distances, the tulip and the heading in a row. */
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
    tulip.setFitWidth(TULIP_SIDE);
    tulip.setFitHeight(TULIP_SIDE);
    tulip.setPreserveRatio(true);
    tulip.setSmooth(true);
    final VBox distances = new VBox(total, partial);
    distances.getStyleClass().add("box-distances");
    distances.setAlignment(Pos.TOP_RIGHT);
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    row = new HBox(number, distances, tulip, space, heading);
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
    setAccessibleText(box.description());
    setGraphic(row);
  }

}
