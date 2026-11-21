package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.DescriptionRenderer;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.SpeedZone;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A box of the roadbook as the strip of the window shows it (DESIGN.md §14): its number, its distances in kilometres
 * written in the format of the language, its diagram drawn by the catalogue (its tulip, with the symbols and the
 * danger mark of its notes), its heading where the catalogue shows it, and its description (the abbreviations and the
 * free text of its notes), as in the roadbook exported, and its place, shown on the map when it is selected; the part
 * of a speed zone it is (phase 5, increment 5: framed, the sign of the speed limit at the start and at the finish of
 * the zone, drawn by the catalogue); the speed limit of the map where the route leaves it (increment 5b, an
 * information for the user); and the box itself, as corrected by the user and as generated, for its editor
 * (development plan, phase 5).
 *
 * <p>Built off the JavaFX Application Thread ({@link #rows(Roadbook, Roadbook, List, Catalogue, Locale,
 * ResourceBundle)}):
 * the diagrams are drawn there. Immutable, its image loaded once built.</p>
 *
 * @param number the number of the box.
 * @param place the place of the box.
 * @param total the total distance, in kilometres with two decimals.
 * @param partial the partial distance, in kilometres with two decimals.
 * @param heading the heading shown, or empty.
 * @param tulip the image of the diagram, or empty for the boxes without tulip nor notes for their diagram.
 * @param notes the description of the box, its lines separated by line feeds; empty if it has none.
 * @param description the text of the box for the readers of the screen: its number, its distances, its notes, its
 *                    waypoint and its speed limit.
 * @param zone the part of a speed zone the box is, or empty outside any zone.
 * @param sign the image of the sign of the speed limit, at the start and at the finish of a zone; empty otherwise.
 * @param limit the speed limit of the map where the route leaves the box (the road it arrives by at the finish), in
 *              km/h; empty if the map gives none, or without map.
 * @param box the box shown, with the corrections of the user.
 * @param generated the box as generated from the route, without the corrections of the user.
 */
record BoxRow(int number, GeoPoint place, String total, String partial, Optional<String> heading,
              Optional<Image> tulip, String notes, String description, Optional<SpeedZone> zone, Optional<Image> sign,
              OptionalInt limit, Box box, Box generated) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The decimals of the distances: the precision of a trip meter (10 m), as the roadbook exported. */
  static final int DECIMALS = 2;
  /** The metres of a kilometre. */
  static final double METRES_PER_KILOMETRE = 1000;
  /** The key of the text of a box for the readers of the screen. */
  static final String BOX_TEXT = "roadbook.box";
  /** The key of the danger of a box for the readers of the screen, with its level. */
  static final String DANGER_TEXT = "roadbook.danger";
  /** The key of the description of a box for the readers of the screen, with its text. */
  static final String NOTES_TEXT = "roadbook.notes";
  /** The key of the symbols of a box for the readers of the screen, with their names. */
  static final String SYMBOLS_TEXT = "roadbook.symbols";
  /** The key of the waypoint of a box for the readers of the screen, with its kind. */
  static final String WAYPOINT_TEXT = "roadbook.waypoint";
  /** The separator of the lines of the description of a box. */
  static final String LINE = "\n";
  /** The separator of the parts of the text of a box for the readers of the screen, and of the names of symbols. */
  static final String PARTS = ", ";
  /** The key of the speed limit at the start of a speed zone for the readers of the screen, with the speed. */
  static final String SPEED_TEXT = "roadbook.speed";
  /** The key of the speed limit at the finish of a speed zone for the readers of the screen, with the speed. */
  static final String SPEED_END_TEXT = "roadbook.speedEnd";
  /** The key of the heading of a box. */
  static final String HEADING_TEXT = "roadbook.heading";

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Builds the rows of a roadbook, its diagrams drawn by a catalogue.
   *
   * @param generated the roadbook generated from the route, its boxes added and removed by the user.
   * @param roadbook the same roadbook, its boxes corrected by the user: the one shown.
   * @param limits the speed limits of the map where the route leaves each box, in their order, in km/h; empty where
   *               the map gives none.
   * @param catalogue the catalogue, which draws the tulips and tells the headings shown.
   * @param locale the locale of the application, for the distances.
   * @param texts the texts of the user interface.
   *
   * @return the rows, one per box, in their order.
   *
   * @throws IOException if a diagram cannot be drawn.
   */
  static List<BoxRow> rows(final Roadbook generated, final Roadbook roadbook, final List<OptionalInt> limits,
                           final Catalogue catalogue, final Locale locale, final ResourceBundle texts)
      throws IOException {
    return rows(generated.boxes(), roadbook.boxes(), limits, catalogue, locale, texts);
  }

  /**
   * Builds the rows of an extract of a roadbook, its boxes as they are, without speed limit of the map: a situation
   * of the training (development plan, phase 7).
   *
   * @param boxes the boxes of the extract, in their order.
   * @param catalogue the catalogue, which draws the tulips and tells the headings shown.
   * @param locale the locale of the application, for the distances.
   * @param texts the texts of the user interface.
   *
   * @return the rows, one per box, in their order.
   *
   * @throws IOException if a diagram cannot be drawn.
   */
  static List<BoxRow> rows(final List<Box> boxes, final Catalogue catalogue, final Locale locale,
                           final ResourceBundle texts) throws IOException {
    return rows(boxes, boxes, Collections.nCopies(boxes.size(), OptionalInt.empty()), catalogue, locale, texts);
  }

  /**
   * Builds the rows of boxes, their diagrams drawn by a catalogue.
   *
   * @param generated the boxes generated from the route, its boxes added and removed by the user.
   * @param shown the same boxes, corrected by the user: those shown.
   * @param limits the speed limits of the map where the route leaves each box, in their order, in km/h; empty where
   *               the map gives none.
   * @param catalogue the catalogue, which draws the tulips and tells the headings shown.
   * @param locale the locale of the application, for the distances.
   * @param texts the texts of the user interface.
   *
   * @return the rows, one per box, in their order.
   *
   * @throws IOException if a diagram cannot be drawn.
   */
  private static List<BoxRow> rows(final List<Box> generated, final List<Box> shown, final List<OptionalInt> limits,
                                   final Catalogue catalogue, final Locale locale, final ResourceBundle texts)
      throws IOException {
    final NumberFormat kilometres = kilometres(locale);
    final List<Optional<SpeedZone>> zones = SpeedZone.along(shown);
    final List<BoxRow> rows = new ArrayList<>(shown.size());
    for (int i = 0; i < shown.size(); i++) {
      final Box box = shown.get(i);
      final Optional<SpeedZone> zone = zones.get(i);
      final String total = kilometres.format(box.total() / METRES_PER_KILOMETRE);
      final String partial = kilometres.format(box.partial() / METRES_PER_KILOMETRE);
      final Optional<String> heading = catalogue.headings().shows(box)
          ? Optional.of(new MessageFormat(texts.getString(HEADING_TEXT), locale)
                            .format(new Object[] {String.valueOf(box.heading().orElseThrow())}))
          : Optional.empty();
      Optional<Image> tulip = Optional.empty();
      if (box.tulip().isPresent() || box.notes().hasDiagram()) {
        final byte[] png = DiagramRenderer.png(box.tulip(), box.notes(), catalogue);
        tulip = Optional.of(new Image(new ByteArrayInputStream(png)));
      }
      final String notes = String.join(LINE, DescriptionRenderer.paragraphs(box.notes(), catalogue));
      final String description = new MessageFormat(texts.getString(BOX_TEXT), locale)
          .format(new Object[] {box.number(), total, partial}) + notes(box.notes(), notes, locale, texts)
          + box.waypoint().map(waypoint -> PARTS + new MessageFormat(texts.getString(WAYPOINT_TEXT), locale)
              .format(new Object[] {waypoint.kind().name()})).orElse("")
          + zone.filter(SpeedZone::isSigned).map(signed -> PARTS + speed(signed, locale, texts)).orElse("");
      Optional<Image> sign = Optional.empty();
      if (zone.filter(SpeedZone::isSigned).isPresent()) {
        sign = Optional.of(new Image(new ByteArrayInputStream(DescriptionRenderer.sign(zone.get(), catalogue))));
      }
      rows.add(new BoxRow(box.number(), box.position(), total, partial, heading, tulip, notes, description, zone,
                          sign, limits.get(i), box, generated.get(i)));
    }
    return rows;
  }

  /**
   * Gives the notes of a box for the readers of the screen: its danger, its description and the names of its symbols,
   * each after a comma, those absent left out.
   *
   * @param notes the notes of the box.
   * @param written the description of the box, as the strip shows it; empty if it has none.
   * @param locale the locale of the application.
   * @param texts the texts of the user interface.
   *
   * @return the text of the notes, each part after a comma; empty if the box has no notes.
   */
  static String notes(final Notes notes, final String written, final Locale locale, final ResourceBundle texts) {
    final StringBuilder text = new StringBuilder();
    if (notes.danger().isPresent()) {
      text.append(PARTS).append(new MessageFormat(texts.getString(DANGER_TEXT), locale)
                                    .format(new Object[] {notes.danger().get().level()}));
    }
    if (!written.isEmpty()) {
      text.append(PARTS).append(new MessageFormat(texts.getString(NOTES_TEXT), locale)
                                    .format(new Object[] {written.replace(LINE, " ")}));
    }
    if (!notes.symbols().isEmpty()) {
      final String names = notes.symbols().stream()
                                .map(symbol -> texts.getString(NotesEditor.SYMBOL_KEYS + symbol.symbol().name()))
                                .collect(Collectors.joining(PARTS));
      text.append(PARTS).append(new MessageFormat(texts.getString(SYMBOLS_TEXT), locale)
                                    .format(new Object[] {names}));
    }
    return text.toString();
  }

  /**
   * Gives the speed limit of the start or of the finish of a speed zone for the readers of the screen: the end of the
   * zone a start finishes first, if any.
   *
   * @param zone the part of the speed zone, its start or its finish.
   * @param locale the locale of the application.
   * @param texts the texts of the user interface.
   *
   * @return the speed limit, or its end, or both.
   */
  private static String speed(final SpeedZone zone, final Locale locale, final ResourceBundle texts) {
    final String key = zone.part() == SpeedZone.Part.START ? SPEED_TEXT : SPEED_END_TEXT;
    final String speed = new MessageFormat(texts.getString(key), locale).format(new Object[] {zone.speed()});
    if (zone.finished().isEmpty()) {
      return speed;
    }
    return new MessageFormat(texts.getString(SPEED_END_TEXT), locale)
               .format(new Object[] {zone.finished().getAsInt()}) + PARTS + speed;
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
