package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Notes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link DescriptionRenderer} with the catalogue of the FIA: the abbreviations as the catalogue writes them,
 * then the free text, written in the middle of the image, smaller when they would not fit.
 */
final class DescriptionRendererTest {

  /** The catalogue of the FIA. */
  private static final Catalogue FIA = Catalogue.fia();
  /** The side of the image, in pixels. */
  private static final int SIZE = FIA.tulip().size();
  /** The height of the bands of rows looked at, in pixels: an eighth of the image. */
  private static final int BAND = SIZE / 8;
  /** Keep to the left, and next, follow the road. */
  private static final List<Abbreviation> ABBREVIATIONS = List.of(Abbreviation.KEEP_LEFT, Abbreviation.AND_NEXT,
                                                                  Abbreviation.FOLLOW_ROAD);
  /** A free text, in English. */
  private static final String TEXT = "BIG ROCKS";
  /** A text of the largest length, of many short words. */
  private static final String LONG_TEXT = "WW WW WW WW WW WW WW WW WW WW WW";
  /** A text of the largest length, one word wider than the image at the smallest font. */
  private static final String ONE_WORD = "W".repeat(Notes.MAX_TEXT);

  /**
   * The abbreviations are written as the catalogue writes them, separated by spaces, then the text; without them,
   * nothing.
   */
  @Test
  @DisplayName("Phase 5: the description writes the abbreviations of the catalogue, then the free text")
  void paragraphsTest() {
    final Notes notes = Notes.NONE.withAbbreviations(ABBREVIATIONS).withText(TEXT);
    assertAll(
        () -> assertEquals(List.of("kpL ET FRO", TEXT), DescriptionRenderer.paragraphs(notes, FIA)),
        () -> assertEquals(List.of(TEXT), DescriptionRenderer.paragraphs(Notes.NONE.withText(TEXT), FIA)),
        () -> assertEquals(List.of(), DescriptionRenderer.paragraphs(Notes.NONE, FIA)));
  }

  /**
   * Notes without description give the background alone; a description writes in the colour of the text, around the
   * middle of the image.
   *
   * @throws IOException if the image cannot be encoded or decoded.
   */
  @Test
  @DisplayName("Phase 5: the description is written in black in the middle of a PNG image, nothing without notes")
  void imageTest() throws IOException {
    final BufferedImage written = ImageIO.read(new ByteArrayInputStream(DescriptionRenderer.png(
        Notes.NONE.withAbbreviations(ABBREVIATIONS), FIA)));
    final BufferedImage empty = DescriptionRenderer.image(Notes.NONE, FIA);
    assertAll(
        () -> assertEquals(List.of(SIZE, SIZE), List.of(written.getWidth(), written.getHeight())),
        () -> assertTrue(count(written, SIZE / 2 - BAND, SIZE / 2 + BAND) > 0),
        () -> assertEquals(0, count(written, 0, BAND)),
        () -> assertEquals(0, count(empty, 0, SIZE)));
  }

  /**
   * A long description is written smaller, over several lines, within the margins; a word too wide is cut at the
   * edges, at the smallest font; words separated by two spaces are written as by one.
   */
  @Test
  @DisplayName("Phase 5: a long description is written smaller, over several lines, within the image")
  void longTest() {
    final Notes many = Notes.NONE.withAbbreviations(Collections.nCopies(Notes.MAX_ABBREVIATIONS,
                                                                        Abbreviation.MORE_OR_LESS_VISIBLE))
                                 .withText(LONG_TEXT);
    final int margin = (int) FIA.notes().margin();
    final BufferedImage image = DescriptionRenderer.image(many, FIA);
    final BufferedImage cut = DescriptionRenderer.image(Notes.NONE.withText(ONE_WORD), FIA);
    final BufferedImage spaced = DescriptionRenderer.image(Notes.NONE.withText("BIG  ROCKS"), FIA);
    assertAll(
        () -> assertTrue(count(spaced, SIZE / 2 - margin, SIZE / 2 + margin) > 0),
        () -> assertTrue(count(image, margin, SIZE - margin) > 0),
        () -> assertEquals(0, count(image, 0, margin / 2)),
        () -> assertTrue(count(cut, SIZE / 2 - margin, SIZE / 2 + margin) > 0));
  }

  /**
   * Counts the pixels of the colour of the text in rows of an image.
   *
   * @param image the image.
   * @param from the first row, included.
   * @param to the last row, excluded.
   *
   * @return how many pixels of those rows have the colour of the text.
   */
  private static int count(final BufferedImage image, final int from, final int to) {
    int count = 0;
    for (int y = from; y < to; y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (image.getRGB(x, y) == FIA.notes().text().getRGB()) {
          count++;
        }
      }
    }
    return count;
  }

}
