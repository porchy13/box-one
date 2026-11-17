package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Notes;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Draws the description of a box with the style of a catalogue, as a PNG image (development plan, phase 5: notes): the
 * abbreviations of its notes, as the catalogue writes them, then its free text — the image of the element
 * {@code notes} of OpenRally, which Terra Pirata shows as PNG only (phase 0).
 *
 * <p><b>Drawing</b>, in a square image of the size of the tulips, on their background, in the colour of the text of
 * the catalogue, in a bold sans-serif font: the abbreviations form a paragraph, the text another one; each paragraph
 * is broken between its words into lines that fit the width of the image less its margins; the font has the size of
 * the catalogue, or a smaller one, a pixel at a time down to its smallest, until the lines fit the image (a word wider
 * than the image at the smallest size is cut at its edges); the lines are centred, the block in the middle of the
 * image.</p>
 *
 * <p>The font is the logical font {@code SansSerif} of Java 2D: the font of each system, so that the image may differ
 * slightly from one system to another.</p>
 *
 * <p>Utility class without state, safe for concurrent use: each call draws its own image, in memory, without any
 * window.</p>
 */
public final class DescriptionRenderer {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The separator of the words of a line. */
  private static final String SPACE = " ";
  /** How many margins the width and the height of the image lose: one on each side. */
  private static final int SIDES = 2;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private DescriptionRenderer() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws the description of a box as a PNG image.
   *
   * @param notes the notes of the box.
   * @param catalogue the catalogue.
   *
   * @return the bytes of the PNG image, of the size of the tulips of the catalogue; the background alone if the notes
   *         have no abbreviation and no text.
   *
   * @throws IOException if the image cannot be encoded.
   */
  public static byte[] png(final Notes notes, final Catalogue catalogue) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image(notes, catalogue), TulipRenderer.FORMAT, bytes);
    return bytes.toByteArray();
  }

  /**
   * Gives the paragraphs of the description: the abbreviations as the catalogue writes them, separated by spaces, then
   * the free text; an empty paragraph is left out.
   *
   * @param notes the notes of the box.
   * @param catalogue the catalogue.
   *
   * @return the paragraphs, none empty.
   */
  public static List<String> paragraphs(final Notes notes, final Catalogue catalogue) {
    final List<String> paragraphs = new ArrayList<>();
    final String abbreviations = notes.abbreviations().stream()
                                      .map(abbreviation -> text(abbreviation, catalogue))
                                      .collect(Collectors.joining(SPACE));
    if (!abbreviations.isEmpty()) {
      paragraphs.add(abbreviations);
    }
    if (!notes.text().isEmpty()) {
      paragraphs.add(notes.text());
    }
    return paragraphs;
  }

  /**
   * Draws the description of a box.
   *
   * @param notes the notes of the box.
   * @param catalogue the catalogue.
   *
   * @return the image, of the size of the tulips of the catalogue.
   */
  static BufferedImage image(final Notes notes, final Catalogue catalogue) {
    final BufferedImage image = TulipRenderer.blank(catalogue.tulip());
    final Graphics2D graphics = TulipRenderer.graphics(image);
    try {
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      graphics.setColor(catalogue.notes().text());
      write(graphics, paragraphs(notes, catalogue), catalogue);
    } finally {
      graphics.dispose();
    }
    return image;
  }

  /**
   * Writes the paragraphs, broken into lines, in the largest font that fits, centred.
   *
   * @param graphics the graphics of the image.
   * @param paragraphs the paragraphs.
   * @param catalogue the catalogue.
   */
  private static void write(final Graphics2D graphics, final List<String> paragraphs, final Catalogue catalogue) {
    final NoteStyle style = catalogue.notes();
    final int size = catalogue.tulip().size();
    final double room = size - SIDES * style.margin();
    double font = style.font();
    List<String> lines = lines(graphics, paragraphs, font, room);
    while (font - 1 >= style.minFont() && !fits(graphics, lines, font, room)) {
      font -= 1;
      lines = lines(graphics, paragraphs, font, room);
    }
    graphics.setFont(font(font));
    final FontMetrics metrics = graphics.getFontMetrics();
    double baseline = (size - lines.size() * metrics.getHeight()) / 2.0 + metrics.getAscent();
    for (final String line : lines) {
      graphics.drawString(line, (float) ((size - metrics.stringWidth(line)) / 2.0), (float) baseline);
      baseline += metrics.getHeight();
    }
  }

  /**
   * Breaks the paragraphs into lines that fit a width, between their words: a word goes on the line before if the
   * line with it still fits, on a new line otherwise.
   *
   * @param graphics the graphics of the image.
   * @param paragraphs the paragraphs.
   * @param font the size of the font, in pixels.
   * @param room the width of a line, in pixels.
   *
   * @return the lines, each paragraph beginning a new one.
   */
  private static List<String> lines(final Graphics2D graphics, final List<String> paragraphs, final double font,
                                    final double room) {
    final FontMetrics metrics = graphics.getFontMetrics(font(font));
    final List<String> lines = new ArrayList<>();
    for (final String paragraph : paragraphs) {
      final List<String> words = Arrays.stream(paragraph.split(SPACE)).filter(word -> !word.isEmpty()).toList();
      String line = words.getFirst();
      for (final String word : words.subList(1, words.size())) {
        final String longer = line + SPACE + word;
        if (metrics.stringWidth(longer) <= room) {
          line = longer;
        } else {
          lines.add(line);
          line = word;
        }
      }
      lines.add(line);
    }
    return lines;
  }

  /**
   * Tells whether lines fit a square: each as wide as it at most, all together as high.
   *
   * @param graphics the graphics of the image.
   * @param lines the lines.
   * @param font the size of the font, in pixels.
   * @param room the side of the square, in pixels.
   *
   * @return {@code true} if the lines fit.
   */
  private static boolean fits(final Graphics2D graphics, final List<String> lines, final double font,
                              final double room) {
    final FontMetrics metrics = graphics.getFontMetrics(font(font));
    return (double) lines.size() * metrics.getHeight() <= room
           && lines.stream().allMatch(line -> metrics.stringWidth(line) <= room);
  }

  /**
   * Gives the font of the description at a size.
   *
   * @param size the size, in pixels.
   *
   * @return the bold sans-serif font of that size.
   */
  private static Font font(final double size) {
    return new Font(Font.SANS_SERIF, Font.BOLD, 1).deriveFont((float) size);
  }

  /**
   * Gives how the catalogue writes an abbreviation.
   *
   * @param abbreviation the abbreviation.
   * @param catalogue the catalogue.
   *
   * @return its text.
   */
  private static String text(final Abbreviation abbreviation, final Catalogue catalogue) {
    return catalogue.abbreviations().get(abbreviation);
  }

}
