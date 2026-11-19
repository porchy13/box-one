package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.SpeedZone;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Draws the description of a box with the style of a catalogue, as a PNG image (development plan, phase 5: notes): the
 * sign of the speed limit at the start or the finish of a speed zone (increment 5; FIA, Appendix III, §5.9.5), the
 * abbreviations of its notes, as the catalogue writes them, then its free text — the image of the element
 * {@code notes} of OpenRally, which Terra Pirata shows as PNG only (phase 0).
 *
 * <p><b>Drawing</b>, in a square image of the size of the tulips, on their background, in the colour of the text of
 * the catalogue, in a bold sans-serif font: the abbreviations form a paragraph, the text another one; each paragraph
 * is broken between its words into lines that fit the width of the image less its margins; the font has the size of
 * the catalogue, or a smaller one, a pixel at a time down to its smallest, until the lines fit the image (a word wider
 * than the image at the smallest size is cut at its edges); the lines are centred, the block in the middle of the
 * image. The sign of a speed limit ({@link NoteStyle}) is drawn at the top of the image, in its middle, the text in
 * the space left below it, or in the middle of the image without text: a disc ringed in the colour of the zones, the
 * speed in it in the bold font, as large as half the inside of the ring, smaller if the speed is wider than
 * {@value #TEXT_SHARE} of it; at the finish of a zone, struck out from its top right to its bottom left by a
 * line half as wide as the ring. A start that finishes another zone at the same box draws the sign of the zone
 * finished, struck out, then the sign of the new zone, side by side, a margin apart.</p>
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
  /** The share of the inside of the ring of a sign the speed may take across. */
  private static final double TEXT_SHARE = 0.8;
  /** The size of the font of a speed, as a share of the inside of the ring of a sign, when it fits. */
  private static final double DIGIT_SHARE = 0.5;
  /** The size of the font at which a speed is measured, in pixels. */
  private static final float MEASURED = 100;
  /** The share of the radius of the inside of a sign that its stroke reaches along each axis: the cosine of 45°. */
  private static final double DIAGONAL = Math.sqrt(0.5);

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
   * @param zone the part of a speed zone the box is, or empty: the start and the finish show the sign of its speed.
   * @param catalogue the catalogue.
   *
   * @return the bytes of the PNG image, of the size of the tulips of the catalogue; the background alone if the notes
   *         have no abbreviation and no text and the box shows no speed.
   *
   * @throws IOException if the image cannot be encoded.
   */
  public static byte[] png(final Notes notes, final Optional<SpeedZone> zone, final Catalogue catalogue)
      throws IOException {
    return encode(image(notes, zone, catalogue));
  }

  /**
   * Draws the signs of the speed limits of a zone alone, for the strip of the window.
   *
   * @param zone the part of the speed zone, its start or its finish.
   * @param catalogue the catalogue.
   *
   * @return the bytes of the PNG image, as high as a sign and as wide as its signs, on the background of the tulips.
   *
   * @throws IOException if the image cannot be encoded.
   */
  public static byte[] sign(final SpeedZone zone, final Catalogue catalogue) throws IOException {
    final int height = (int) Math.ceil(catalogue.notes().sign());
    final int width = (int) Math.ceil(width(zone, catalogue.notes()));
    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    final Graphics2D graphics = TulipRenderer.graphics(image);
    try {
      graphics.setColor(catalogue.tulip().background());
      graphics.fillRect(0, 0, width, height);
      signs(graphics, zone, 0, 0, catalogue);
    } finally {
      graphics.dispose();
    }
    return encode(image);
  }

  /**
   * Tells whether a box has a description: abbreviations or a text in its notes, or the sign of a speed limit.
   *
   * @param notes the notes of the box.
   * @param zone the part of a speed zone the box is, or empty.
   *
   * @return {@code true} if its description is not empty.
   */
  public static boolean has(final Notes notes, final Optional<SpeedZone> zone) {
    return !notes.abbreviations().isEmpty() || !notes.text().isEmpty() || zone.filter(SpeedZone::isSigned).isPresent();
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
   * Encodes an image as PNG.
   *
   * @param image the image.
   *
   * @return the bytes of the PNG image.
   *
   * @throws IOException if the image cannot be encoded.
   */
  private static byte[] encode(final BufferedImage image) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image, TulipRenderer.FORMAT, bytes);
    return bytes.toByteArray();
  }

  /**
   * Draws the description of a box: the sign of its speed limit, at the top, or in the middle without text, then its
   * text in the space left.
   *
   * @param notes the notes of the box.
   * @param zone the part of a speed zone the box is, or empty.
   * @param catalogue the catalogue.
   *
   * @return the image, of the size of the tulips of the catalogue.
   */
  static BufferedImage image(final Notes notes, final Optional<SpeedZone> zone, final Catalogue catalogue) {
    final BufferedImage image = TulipRenderer.blank(catalogue.tulip());
    final Graphics2D graphics = TulipRenderer.graphics(image);
    try {
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      final NoteStyle style = catalogue.notes();
      final List<String> paragraphs = paragraphs(notes, catalogue);
      double top = style.margin();
      final Optional<SpeedZone> signed = zone.filter(SpeedZone::isSigned);
      if (signed.isPresent()) {
        final double size = catalogue.tulip().size();
        final double centred = (size - style.sign()) / 2;
        signs(graphics, signed.get(), (size - width(signed.get(), style)) / 2, paragraphs.isEmpty() ? centred : top,
              catalogue);
        top += style.sign() + style.margin();
      }
      graphics.setColor(style.text());
      write(graphics, paragraphs, top, catalogue);
    } finally {
      graphics.dispose();
    }
    return image;
  }

  /**
   * Gives the width of the signs of a part of a speed zone: one sign, or two a margin apart for a start that finishes
   * another zone.
   *
   * @param zone the part of the speed zone, its start or its finish.
   * @param style the style of the notes.
   *
   * @return the width, in pixels.
   */
  private static double width(final SpeedZone zone, final NoteStyle style) {
    return zone.finished().isPresent() ? 2 * style.sign() + style.margin() : style.sign();
  }

  /**
   * Draws the signs of a part of a speed zone: the sign of the zone finished, struck out, if the start finishes one,
   * then the sign of the part.
   *
   * @param graphics the graphics of the image.
   * @param zone the part of the speed zone, its start or its finish.
   * @param left the left of the first sign, in pixels.
   * @param top the top of the signs, in pixels.
   * @param catalogue the catalogue.
   */
  private static void signs(final Graphics2D graphics, final SpeedZone zone, final double left, final double top,
                            final Catalogue catalogue) {
    double x = left;
    if (zone.finished().isPresent()) {
      sign(graphics, new SpeedZone(zone.finished().getAsInt(), SpeedZone.Part.FINISH), x, top, catalogue);
      x += catalogue.notes().sign() + catalogue.notes().margin();
    }
    sign(graphics, zone, x, top, catalogue);
  }

  /**
   * Draws the sign of a speed limit: a disc ringed in the colour of the zones, the speed in it, struck out at the
   * finish of the zone.
   *
   * @param graphics the graphics of the image.
   * @param zone the part of the speed zone, its start or its finish.
   * @param left the left of the sign, in pixels.
   * @param top the top of the sign, in pixels.
   * @param catalogue the catalogue.
   */
  private static void sign(final Graphics2D graphics, final SpeedZone zone, final double left, final double top,
                           final Catalogue catalogue) {
    final NoteStyle style = catalogue.notes();
    final double diameter = style.sign();
    final double ring = style.ring();
    final Ellipse2D disc = new Ellipse2D.Double(left + ring / 2, top + ring / 2, diameter - ring, diameter - ring);
    graphics.setColor(catalogue.tulip().background());
    graphics.fill(disc);
    graphics.setColor(style.zone());
    graphics.setStroke(new BasicStroke((float) ring));
    graphics.draw(disc);
    final double inside = diameter - 2 * ring;
    final String speed = Integer.toString(zone.speed());
    final double measured = graphics.getFontMetrics(font(MEASURED)).stringWidth(speed);
    graphics.setFont(font(Math.min(inside * DIGIT_SHARE, inside * TEXT_SHARE * MEASURED / measured)));
    final FontMetrics metrics = graphics.getFontMetrics();
    final double x = left + diameter / 2;
    final double y = top + diameter / 2;
    graphics.setColor(style.text());
    graphics.drawString(speed, (float) (x - metrics.stringWidth(speed) / 2.0),
                        (float) (y + (metrics.getAscent() - metrics.getDescent()) / 2.0));
    if (zone.part() == SpeedZone.Part.FINISH) {
      final double reach = inside / 2 * DIAGONAL;
      graphics.setStroke(new BasicStroke((float) (ring / 2)));
      graphics.draw(new Line2D.Double(x + reach, y - reach, x - reach, y + reach));
    }
  }

  /**
   * Writes the paragraphs, broken into lines, in the largest font that fits the space from a top to the bottom margin
   * of the image, centred in it.
   *
   * @param graphics the graphics of the image.
   * @param paragraphs the paragraphs.
   * @param top the top of the space of the text, in pixels.
   * @param catalogue the catalogue.
   */
  private static void write(final Graphics2D graphics, final List<String> paragraphs, final double top,
                            final Catalogue catalogue) {
    final NoteStyle style = catalogue.notes();
    final int size = catalogue.tulip().size();
    final double room = size - SIDES * style.margin();
    final double height = size - top - style.margin();
    double font = style.font();
    List<String> lines = lines(graphics, paragraphs, font, room);
    while (font - 1 >= style.minFont() && !fits(graphics, lines, font, room, height)) {
      font -= 1;
      lines = lines(graphics, paragraphs, font, room);
    }
    graphics.setFont(font(font));
    final FontMetrics metrics = graphics.getFontMetrics();
    double baseline = top + (height - lines.size() * metrics.getHeight()) / 2.0 + metrics.getAscent();
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
   * Tells whether lines fit a space: each as wide as it at most, all together as high.
   *
   * @param graphics the graphics of the image.
   * @param lines the lines.
   * @param font the size of the font, in pixels.
   * @param room the width of the space, in pixels.
   * @param height the height of the space, in pixels.
   *
   * @return {@code true} if the lines fit.
   */
  private static boolean fits(final Graphics2D graphics, final List<String> lines, final double font,
                              final double room, final double height) {
    final FontMetrics metrics = graphics.getFontMetrics(font(font));
    return (double) lines.size() * metrics.getHeight() <= height
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
