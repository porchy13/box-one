package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.TulipRenderer;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Tulip;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * A box of the roadbook as the strip of the window shows it (DESIGN.md §14): its number, its distances in kilometres
 * written in the format of the language, its tulip drawn by the catalogue, its heading where the catalogue shows it,
 * as in the roadbook exported, and its place, shown on the map when it is selected; and the box itself, as corrected
 * by the user and as generated, for its editor (development plan, phase 5).
 *
 * <p>Built off the JavaFX Application Thread ({@link #rows(Roadbook, Roadbook, Catalogue, Locale, ResourceBundle)}):
 * the tulips are drawn there. Immutable, its image loaded once built.</p>
 *
 * @param number the number of the box.
 * @param place the place of the box.
 * @param total the total distance, in kilometres with two decimals.
 * @param partial the partial distance, in kilometres with two decimals.
 * @param heading the heading shown, or empty.
 * @param tulip the image of the tulip, or empty for the boxes of the start and of the finish.
 * @param description the text of the box for the readers of the screen: its number and its distances.
 * @param box the box shown, with the corrections of the user.
 * @param generated the box as generated from the route, without the corrections of the user.
 */
record BoxRow(int number, GeoPoint place, String total, String partial, Optional<String> heading,
              Optional<Image> tulip, String description, Box box, Box generated) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The decimals of the distances: the precision of a trip meter (10 m), as the roadbook exported. */
  static final int DECIMALS = 2;
  /** The metres of a kilometre. */
  static final double METRES_PER_KILOMETRE = 1000;
  /** The key of the text of a box for the readers of the screen. */
  static final String BOX_TEXT = "roadbook.box";
  /** The key of the heading of a box. */
  static final String HEADING_TEXT = "roadbook.heading";

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Builds the rows of a roadbook, its tulips drawn by a catalogue.
   *
   * @param generated the roadbook generated from the route, its boxes added and removed by the user.
   * @param roadbook the same roadbook, its boxes corrected by the user: the one shown.
   * @param catalogue the catalogue, which draws the tulips and tells the headings shown.
   * @param locale the locale of the application, for the distances.
   * @param texts the texts of the user interface.
   *
   * @return the rows, one per box, in their order.
   *
   * @throws IOException if a tulip cannot be drawn.
   */
  static List<BoxRow> rows(final Roadbook generated, final Roadbook roadbook, final Catalogue catalogue,
                           final Locale locale, final ResourceBundle texts) throws IOException {
    final NumberFormat kilometres = kilometres(locale);
    final List<BoxRow> rows = new ArrayList<>(roadbook.boxes().size());
    for (int i = 0; i < roadbook.boxes().size(); i++) {
      final Box box = roadbook.boxes().get(i);
      final String total = kilometres.format(box.total() / METRES_PER_KILOMETRE);
      final String partial = kilometres.format(box.partial() / METRES_PER_KILOMETRE);
      final Optional<String> heading = catalogue.headings().shows(box)
          ? Optional.of(new MessageFormat(texts.getString(HEADING_TEXT), locale)
                            .format(new Object[] {String.valueOf(box.heading().orElseThrow())}))
          : Optional.empty();
      Optional<Image> tulip = Optional.empty();
      final Optional<Tulip> schema = box.tulip();
      if (schema.isPresent()) {
        final byte[] png = TulipRenderer.png(schema.get(), catalogue.tulip());
        tulip = Optional.of(new Image(new ByteArrayInputStream(png)));
      }
      final String description = new MessageFormat(texts.getString(BOX_TEXT), locale)
          .format(new Object[] {box.number(), total, partial});
      rows.add(new BoxRow(box.number(), box.position(), total, partial, heading, tulip, description, box,
                          generated.boxes().get(i)));
    }
    return rows;
  }

  /**
   * Returns the format of the distances of the strip: kilometres with {@value #DECIMALS} decimals, in the format of
   * the language.
   *
   * @param locale the locale of the application.
   *
   * @return a new format, not thread-safe.
   */
  static NumberFormat kilometres(final Locale locale) {
    final NumberFormat kilometres = NumberFormat.getNumberInstance(locale);
    kilometres.setMinimumFractionDigits(DECIMALS);
    kilometres.setMaximumFractionDigits(DECIMALS);
    return kilometres;
  }

}
