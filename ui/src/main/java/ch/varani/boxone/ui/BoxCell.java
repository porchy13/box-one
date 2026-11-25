package ch.varani.boxone.ui;

import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.Optional;

/**
 * A box of the roadbook strip, drawn as on paper (DESIGN.md §14): at the left, the number of the box, small, to find
 * it again (asked by the maintainer, 2026-11-14), the mark of its waypoint in a circle under it (phase 5, increment
 * 4; FIA, Appendix III, §5.12.2: all but the WPP; increment 5, §5.9.2 and §5.9.4: DZ and FZ), then the total
 * distance, large, and the partial distance under it, small, both right-aligned; in the middle, the diagram (the
 * tulip, the symbols and the danger mark of the notes); at the right, the heading where the catalogue shows it, and
 * the description under it (the sign of the speed limit at the start and at the finish of a speed zone, §5.9.5, then
 * the abbreviations and the free text of the notes, phase 5). A box of a speed zone is framed (§5.9.1); a DZ that
 * finishes another zone has the circle of a FZ above its own (§37.2.3). A box less than 300 m after the box before it
 * has its distances highlighted in green, and the box before it a vertical arrow beside its partial distance
 * (§5.6.11, as the strip on paper). The box has
 * its number, its distances, its notes, its waypoint and its speed limit as accessible text.
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class BoxCell extends ListCell<BoxRow> {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The side of the tulip in the strip, in pixels: half the image of the catalogue, as on a printed strip. */
  static final double TULIP_SIDE = 100;
  /** The style class of the waypoint of a box. */
  static final String WAYPOINT_STYLE = "box-waypoint";
  /** The beginning of the style class of the kind of the waypoint of a box ({@code waypoint-wpm}…). */
  static final String WAYPOINT_KIND_STYLE = "waypoint-";
  /** The style class of a box of a speed zone, framed. */
  static final String ZONE_STYLE = "speed-zone";
  /** The side of the sign of a speed limit in the strip, in pixels: as large as the heading. */
  static final double SIGN_SIDE = 36;
  /** The style class of the distances of a box close to the box before it, highlighted in green. */
  static final String CLOSE_STYLE = "close";
  /** The vertical arrow of a box close to the next one, pointing down (FIA, Appendix III, §5.6.11). */
  static final String ARROW = "\u2193";
  /** The style class of the paper of a box. */
  private static final String PAPER_STYLE = "box-paper";
  /** The style class of the distances of a box. */
  private static final String DISTANCES_STYLE = "box-distances";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The number of the box. */
  private final Label number = new Label();
  /** The mark of the waypoint of the box in its circle, under the number; hidden without waypoint marked. */
  private final Label waypoint = new Label();
  /** The circle of the FZ of a zone finished by the DZ of the box, above its circle; hidden otherwise. */
  private final Label finish = new Label(WaypointKind.FZ.mark());
  /** The total distance. */
  private final Label total = new Label();
  /** The partial distance. */
  private final Label partial = new Label();
  /** The vertical arrow beside the partial distance, when the next box is close; hidden otherwise. */
  private final Label arrowLabel = new Label(ARROW);
  /** The distances: the total, the partial and the arrow; highlighted when the box is close to the one before. */
  private final VBox distances;
  /** The diagram: the tulip and the notes drawn on it. */
  private final ImageView tulip = new ImageView();
  /** The heading. */
  private final Label heading = new Label();
  /** The sign of the speed limit, at the start and at the finish of a speed zone; hidden otherwise. */
  private final ImageView sign = new ImageView();
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
    waypoint.getStyleClass().add(WAYPOINT_STYLE);
    waypoint.managedProperty().bind(waypoint.visibleProperty());
    finish.getStyleClass().addAll(WAYPOINT_STYLE,
                                  WAYPOINT_KIND_STYLE + WaypointKind.FZ.name().toLowerCase(Locale.ROOT));
    finish.managedProperty().bind(finish.visibleProperty());
    total.getStyleClass().add("box-total");
    partial.getStyleClass().add("box-partial");
    heading.getStyleClass().add("box-heading");
    notes.getStyleClass().add("box-notes");
    notes.setWrapText(true);
    tulip.setFitWidth(TULIP_SIDE);
    tulip.setFitHeight(TULIP_SIDE);
    tulip.setPreserveRatio(true);
    tulip.setSmooth(true);
    sign.setFitWidth(SIGN_SIDE);
    sign.setFitHeight(SIGN_SIDE);
    sign.setPreserveRatio(true);
    sign.setSmooth(true);
    sign.managedProperty().bind(sign.visibleProperty());
    arrowLabel.getStyleClass().add("box-arrow");
    final HBox partials = new HBox(arrowLabel, partial);
    partials.setAlignment(Pos.CENTER_RIGHT);
    distances = new VBox(total, partials);
    distances.getStyleClass().add(DISTANCES_STYLE);
    distances.setAlignment(Pos.TOP_RIGHT);
    final Region space = new Region();
    HBox.setHgrow(space, Priority.ALWAYS);
    final VBox description = new VBox(heading, sign, notes);
    description.getStyleClass().add("box-description");
    description.setAlignment(Pos.TOP_RIGHT);
    final VBox numbered = new VBox(number, finish, waypoint);
    numbered.setAlignment(Pos.TOP_RIGHT);
    row = new HBox(numbered, distances, tulip, space, description);
    row.getStyleClass().add(PAPER_STYLE);
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
    final Optional<WaypointKind> marked = box.box().waypoint().map(Waypoint::kind).filter(WaypointKind::isMarked);
    waypoint.getStyleClass().setAll(WAYPOINT_STYLE);
    marked.ifPresent(kind -> {
      waypoint.setText(kind.mark());
      waypoint.getStyleClass().add(WAYPOINT_KIND_STYLE + kind.name().toLowerCase(Locale.ROOT));
    });
    waypoint.setVisible(marked.isPresent());
    finish.setVisible(box.zone().filter(zone -> zone.finished().isPresent()).isPresent());
    total.setText(box.total());
    partial.setText(box.partial());
    arrowLabel.setVisible(box.closeNext());
    distances.getStyleClass().setAll(DISTANCES_STYLE);
    if (box.close()) {
      distances.getStyleClass().add(CLOSE_STYLE);
    }
    tulip.setImage(box.tulip().orElse(null));
    heading.setText(box.heading().orElse(""));
    sign.setImage(box.sign().orElse(null));
    sign.setVisible(box.sign().isPresent());
    notes.setText(box.notes());
    row.getStyleClass().setAll(PAPER_STYLE);
    if (box.zone().isPresent()) {
      row.getStyleClass().add(ZONE_STYLE);
    }
    setAccessibleText(box.description());
    setGraphic(row);
  }

}
