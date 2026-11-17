package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link BoxRow} (development plan, phase 5): the notes of a box in the strip, its description written as
 * the catalogue writes it, its diagram drawn even without tulip, its notes read by the readers of the screen.
 */
@ExtendWith(ApplicationExtension.class)
final class BoxRowTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The locale of the tests: French, in the formats of Switzerland. */
  private static final Locale LOCALE = Languages.FRENCH;
  /** The start: the station of Yverdon-les-Bains. */
  private static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** The finish: the castle of Yverdon-les-Bains. */
  private static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);
  /** The length of the route, in metres. */
  private static final double LENGTH = 345.4;
  /** The heading of the start, in degrees: south. */
  private static final int SOUTH = 180;
  /** The notes of the start: a danger of level 2, follow the road, a text, a bump after the start. */
  private static final Notes NOTES = new Notes(Optional.of(Danger.LEVEL_2),
                                               List.of(Abbreviation.FOLLOW_ROAD, Abbreviation.AND_NEXT), "BIG ROCKS",
                                               List.of(new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT),
                                                       new BoxSymbol(Symbol.DITCH, SymbolPlace.EXIT)));

  /**
   * A box with notes has its description, its paragraphs on lines, and a diagram even without tulip; a box without
   * notes nor tulip has neither; the readers of the screen hear the danger, the description and the symbols.
   *
   * @throws IOException if a diagram cannot be drawn.
   */
  @Test
  @DisplayName("Phase 5: the strip shows the notes of a box, its diagram even without tulip, read by screen readers")
  void notesTest() throws IOException {
    final Roadbook roadbook = new Roadbook(
        new Route(List.of(STATION, CASTLE), List.of(), LENGTH, Duration.ofMinutes(1)),
        List.of(new Box(1, STATION, 0, 0, OptionalInt.of(SOUTH), Optional.empty(), HeadingDisplay.RULE, NOTES),
                new Box(2, CASTLE, LENGTH, LENGTH, OptionalInt.empty(), Optional.empty())));
    final List<BoxRow> rows = BoxRow.rows(roadbook, roadbook, Catalogue.fia(), LOCALE, TEXTS);
    assertAll(
        () -> assertEquals("FRO ET\nBIG ROCKS", rows.getFirst().notes()),
        () -> assertTrue(rows.getFirst().tulip().isPresent()),
        () -> assertTrue(rows.getFirst().description().endsWith(
            ", danger de niveau 2, notes : FRO ET BIG ROCKS, symboles : Bosse, Fossé")),
        () -> assertEquals("", rows.getLast().notes()),
        () -> assertTrue(rows.getLast().tulip().isEmpty()),
        () -> assertEquals("Case 2 : 0,35 km, 0,35 km depuis la précédente", rows.getLast().description()));
  }

  /**
   * Notes without danger, description nor symbols add nothing for the readers of the screen; each part present is
   * added after a comma.
   */
  @Test
  @DisplayName("Phase 5: the readers of the screen hear only the parts of the notes a box has")
  void partsTest() {
    assertAll(
        () -> assertEquals("", BoxRow.notes(Notes.NONE, "", LOCALE, TEXTS)),
        () -> assertEquals(", danger de niveau 1", BoxRow.notes(Notes.NONE.withDanger(Optional.of(Danger.LEVEL_1)),
                                                                "", LOCALE, TEXTS)),
        () -> assertEquals(", notes : kpL", BoxRow.notes(Notes.NONE.withAbbreviations(List.of(Abbreviation.KEEP_LEFT)),
                                                         "kpL", LOCALE, TEXTS)));
  }

}
