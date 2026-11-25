package ch.varani.boxone.export;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.catalog.StripStyle;
import ch.varani.boxone.catalog.DescriptionRenderer;
import ch.varani.boxone.catalog.DiagramRenderer;
import ch.varani.boxone.catalog.WaypointMark;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.SpeedZone;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Writes a roadbook as a PDF strip, to print and to read in a roadbook holder (development plan, phase 6; decisions of
 * the maintainer, 2026-11-20: as a real roadbook, A5, the binding margin of 15 mm, the boxes close to each other and
 * the dangers of level 3 marked as the FIA asks), after the rulebook of the FIA (2026 Cross-Country Rally Sporting
 * Regulations, Appendix III, §5).
 *
 * <p><b>Pages</b>: A5 portrait (§5.3.1), printed on one side, bound on the upper side (§5.3.2): {@value #BINDING} mm
 * left at the top. Each page has one header line (§5.5.1): the number of the page without the word "Page" (§5.1.6),
 * the name of the roadbook and its length, and the kilometres from the first box of the page to the finish; then at
 * most {@value #ROWS} lines of boxes; then, at the bottom, the reminder that the route is indicative (issue #7 of
 * GitLab) and the attribution of OpenStreetMap (AGENTS.md §2: every export carries it).</p>
 *
 * <p><b>Boxes</b> (§5.5, Diagram 1), three columns as the strip of the window:</p>
 * <ul>
 *   <li>the distances: the total distance in large digits, the partial distance in its own frame under it, and the
 *       mark of the waypoint in its disc ({@link WaypointMark}, the FZ of a zone finished at the box above the DZ);
 *       a box less than {@value #CLOSE} m before the next one has a vertical arrow, and the next one its distance box
 *       filled with the colour of the catalogue (§5.6.11, Diagram 4);</li>
 *   <li>the diagram: the tulip, the symbols and the danger marks of the notes ({@link DiagramRenderer});</li>
 *   <li>the description: the heading (CAP) where the catalogue shows it, the description of the notes and the sign of
 *       a speed limit ({@link DescriptionRenderer}), and the number of the waypoint in a circle, counted as in the
 *       OpenRally export.</li>
 * </ul>
 * <p>The boxes of a speed zone are framed in the colour of the zones (§5.9), a box of a danger of level 3 in the colour
 * of the dangers (§5.8.2), outside the frame of a zone. The drawings are the PNG images of the catalogue; the texts are
 * written in Helvetica (one of the standard fonts of PDF, not embedded): a character it cannot write is replaced by
 * {@value #REPLACEMENT}. The distances are written in the format of the language of the application.</p>
 *
 * <p><b>Saving</b>: {@link #save(Roadbook, String, String, String, Locale, Catalogue, Path)} writes a temporary file
 * next to the file chosen, then moves it in place ({@link AtomicFile}), so that a failed export never leaves a damaged
 * file (AGENTS.md §5).</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class PdfWriter {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The points of a millimetre. */
  static final float POINTS_PER_MM = 72f / 25.4f;
  /** The margin at the top of a page, for the binding on the upper side, in millimetres (decision, 2026-11-20). */
  static final float BINDING = 15;
  /** The margin at the left and at the right of a page, in millimetres. */
  static final float SIDE = 5;
  /** The margin at the bottom of a page, in millimetres. */
  static final float BOTTOM = 5;
  /** The height of the header line, in millimetres. */
  static final float HEADER = 9;
  /** The height of the reminder and of the attribution at the bottom of a page, in millimetres. */
  static final float FOOTER = 9;
  /** The lines of boxes of a page: at most five (§5.5.1). */
  static final int ROWS = 5;
  /** The width of the distance box, in millimetres. */
  static final float DISTANCE_WIDTH = 44;
  /** The width of the diagram box, in millimetres. */
  static final float DIAGRAM_WIDTH = 50;
  /** The width of the number of the page in the header, in millimetres. */
  static final float PAGE_NUMBER_WIDTH = 12;
  /** The width of the kilometres to the finish in the header, in millimetres. */
  static final float REMAINING_WIDTH = 30;
  /** The distance under which the next box is close, in metres (§5.6.11: "less than 300 metres"). */
  static final double CLOSE = StripStyle.CLOSE_DISTANCE;
  /** The space between the edges of a box and what it holds, in millimetres. */
  static final float INSET = 1.5f;
  /** The width of the frame of the partial distance, in millimetres. */
  static final float PARTIAL_WIDTH = 22;
  /** The height of the frame of the partial distance, in millimetres. */
  static final float PARTIAL_HEIGHT = 10;
  /** The diameter of the disc of a waypoint, and of the circle of its number, in millimetres. */
  static final float MARK = 8;
  /** The space between the frame of the partial distance and the arrow of a close box, in millimetres. */
  static final float ARROW_GAP = 3;
  /** The half width of the head of the arrow of a close box, in millimetres. */
  static final float ARROW_HEAD = 1.5f;
  /** The height of the head of the arrow of a close box, in millimetres. */
  static final float ARROW_LENGTH = 3;
  /** The height left for the heading at the top of the description box, in millimetres. */
  static final float HEADING_HEIGHT = 7;
  /** The width of the lines of the boxes, in points. */
  static final float GRID = 0.8f;
  /** The width of the frames of a zone and of a danger, and of the arrow, in points. */
  static final float FRAME = 2.5f;
  /** The size of the total distance, in points. */
  static final float TOTAL_SIZE = 24;
  /** The size of the partial distance, in points. */
  static final float PARTIAL_SIZE = 13;
  /** The size of the texts of the header, in points. */
  static final float HEADER_SIZE = 11;
  /** The size of the heading, in points. */
  static final float HEADING_SIZE = 12;
  /** The size of the mark of a waypoint of one letter and three, in points. */
  static final float MARK_SIZE = 9;
  /** The size of the mark of a waypoint of two letters (DZ, FZ), in points. */
  static final float SMALL_MARK_SIZE = 7;
  /** The size of the number of a waypoint, in points. */
  static final float NUMBER_SIZE = 8;
  /** The size of the reminder and of the attribution, in points. */
  static final float FOOTER_SIZE = 6;
  /** The space between the lines of the reminder, in points. */
  static final float FOOTER_LEADING = 7;
  /** The height of the capital letters of Helvetica, in fractions of the size of the font. */
  static final float CAP_HEIGHT = 0.72f;
  /** The thousandths of the size of a font in which its widths are given. */
  static final float FONT_UNITS = 1000;
  /** The control distance of a quarter of circle drawn as a Bézier curve, in fractions of its radius. */
  static final float CIRCLE = 0.552_284_7f;
  /** The decimals of the distances: the precision of a trip meter (10 m), as the strip of the window. */
  static final int DECIMALS = 2;
  /** The metres of a kilometre. */
  static final double METRES_PER_KILOMETRE = 1000;
  /** The character written for one Helvetica cannot write. */
  static final String REPLACEMENT = "?";
  /** The end of a text cut to fit its place. */
  static final String ELLIPSIS = "…";
  /** The separator of the name and of the length of the roadbook in the header. */
  static final String NAME_SEPARATOR = " – ";
  /** The unit of the distances of the header. */
  static final String KILOMETRES = " km";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private PdfWriter() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Saves a roadbook into a PDF file, replacing it if it exists: written into a temporary file of the same folder,
   * then moved in place.
   *
   * @param roadbook the roadbook: its boxes, with the changes of the user.
   * @param name the name of the roadbook, in the header of each page and as the title of the document.
   * @param notice the reminder that the route is indicative, at the bottom of each page.
   * @param heading the pattern of the heading of a box ({@link MessageFormat}, the heading as its argument).
   * @param locale the locale of the application, for the distances.
   * @param catalogue the catalogue of symbols that draws the boxes and tells the headings shown.
   * @param file the file.
   *
   * @throws IOException if the file cannot be written, or a box drawn; it is then left as it was.
   */
  public static void save(final Roadbook roadbook, final String name, final String notice, final String heading,
                          final Locale locale, final Catalogue catalogue, final Path file) throws IOException {
    AtomicFile.save(file, output -> write(roadbook, name, notice, heading, locale, catalogue, output));
  }

  /**
   * Writes a roadbook as a PDF strip into a stream; the stream is left open.
   *
   * @param roadbook the roadbook: its boxes, with the changes of the user.
   * @param name the name of the roadbook, in the header of each page and as the title of the document.
   * @param notice the reminder that the route is indicative, at the bottom of each page.
   * @param heading the pattern of the heading of a box ({@link MessageFormat}, the heading as its argument).
   * @param locale the locale of the application, for the distances.
   * @param catalogue the catalogue of symbols that draws the boxes and tells the headings shown.
   * @param output the stream.
   *
   * @throws IOException if the stream cannot be written, or a box drawn.
   */
  public static void write(final Roadbook roadbook, final String name, final String notice, final String heading,
                           final Locale locale, final Catalogue catalogue, final OutputStream output)
      throws IOException {
    try (PDDocument document = new PDDocument()) {
      document.getDocumentInformation().setTitle(name);
      document.getDocumentInformation().setCreator(OpenRallyWriter.CREATOR);
      new Printer(document, roadbook, new Texts(name, notice, heading, locale), catalogue).print();
      document.save(output);
    }
    output.flush();
  }

  /**
   * Counts the pages of the PDF strip of a roadbook.
   *
   * @param roadbook the roadbook.
   *
   * @return the number of pages: {@value #ROWS} boxes on each, the last one with the boxes left.
   */
  public static int pages(final Roadbook roadbook) {
    return (roadbook.boxes().size() + ROWS - 1) / ROWS;
  }

  /**
   * Gives the number of each waypoint among those of the roadbook, as the OpenRally export counts them: one for each
   * waypoint placed, and one more before it for the FZ of a zone a DZ finishes.
   *
   * @param boxes the boxes of the roadbook.
   * @param zones the part of a speed zone each box is.
   *
   * @return the number of the waypoint of each box, from 1; 0 for a box without waypoint.
   */
  static int[] numbers(final List<Box> boxes, final List<Optional<SpeedZone>> zones) {
    final int[] numbers = new int[boxes.size()];
    int count = 0;
    for (int i = 0; i < boxes.size(); i++) {
      if (zones.get(i).filter(zone -> zone.finished().isPresent()).isPresent()) {
        count++;
      }
      if (boxes.get(i).waypoint().isPresent()) {
        count++;
        numbers[i] = count;
      }
    }
    return numbers;
  }

  /**
   * Converts millimetres into points.
   *
   * @param millimetres the length, in millimetres.
   *
   * @return the length, in points.
   */
  static float mm(final float millimetres) {
    return millimetres * POINTS_PER_MM;
  }

  /**
   * Replaces the characters a font cannot write.
   *
   * @param text the text.
   * @param font the font.
   *
   * @return the text, each character the font cannot write replaced by {@value #REPLACEMENT}.
   *
   * @throws IOException if the font cannot be read.
   */
  static String printable(final String text, final PDFont font) throws IOException {
    final StringBuilder printable = new StringBuilder(text.length());
    for (final int point : text.codePoints().toArray()) {
      final String character = Character.toString(point);
      printable.append(writes(font, character) ? character : REPLACEMENT);
    }
    return printable.toString();
  }

  /**
   * Tells whether a font can write a character.
   *
   * @param font the font.
   * @param character the character.
   *
   * @return {@code true} if the encoding of the font has it.
   *
   * @throws IOException if the font cannot be read.
   */
  private static boolean writes(final PDFont font, final String character) throws IOException {
    try {
      font.encode(character);
      return true;
    } catch (IllegalArgumentException _) {
      return false;
    }
  }

  /**
   * Measures a text.
   *
   * @param text the text, written by the font.
   * @param font the font.
   * @param size the size of the font, in points.
   *
   * @return the width of the text, in points.
   *
   * @throws IOException if the font cannot be read.
   */
  static float width(final String text, final PDFont font, final float size) throws IOException {
    return font.getStringWidth(text) / FONT_UNITS * size;
  }

  /**
   * Cuts a text to a width, its end replaced by {@value #ELLIPSIS}.
   *
   * @param text the text, written by the font.
   * @param font the font.
   * @param size the size of the font, in points.
   * @param room the width available, in points.
   *
   * @return the text, if it fits; otherwise its beginning and {@value #ELLIPSIS}, or {@value #ELLIPSIS} alone.
   *
   * @throws IOException if the font cannot be read.
   */
  static String fit(final String text, final PDFont font, final float size, final float room) throws IOException {
    if (width(text, font, size) <= room) {
      return text;
    }
    String cut = text;
    while (!cut.isEmpty() && width(cut + ELLIPSIS, font, size) > room) {
      cut = cut.substring(0, cut.length() - 1);
    }
    return cut + ELLIPSIS;
  }

  /**
   * Breaks a text into the lines that fit a width, at its spaces; a word longer than the width stands on its own line.
   *
   * @param text the text, written by the font.
   * @param font the font.
   * @param size the size of the font, in points.
   * @param room the width of a line, in points.
   *
   * @return the lines, in their order.
   *
   * @throws IOException if the font cannot be read.
   */
  static List<String> lines(final String text, final PDFont font, final float size, final float room)
      throws IOException {
    final List<String> lines = new ArrayList<>();
    String line = "";
    for (final String word : text.strip().split("\\s+")) {
      final String longer = line.isEmpty() ? word : line + " " + word;
      if (line.isEmpty() || width(longer, font, size) <= room) {
        line = longer;
      } else {
        lines.add(line);
        line = word;
      }
    }
    lines.add(line);
    return lines;
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The texts written around the boxes, in the language of the application.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param name the name of the roadbook.
   * @param notice the reminder that the route is indicative.
   * @param heading the pattern of the heading of a box.
   * @param locale the locale of the application, for the distances.
   */
  private record Texts(String name, String notice, String heading, Locale locale) {
  }

  /**
   * Prints the pages of one roadbook into one document.
   *
   * <p>Not thread-safe: used by one export.</p>
   */
  private static final class Printer {

    /** The document. */
    private final PDDocument document;
    /** The roadbook. */
    private final Roadbook roadbook;
    /** The texts around the boxes. */
    private final Texts texts;
    /** The catalogue of symbols. */
    private final Catalogue catalogue;
    /** The font of the texts: Helvetica bold. */
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    /** The font of the reminder: Helvetica. */
    private final PDFont plain = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    /** The format of the distances. */
    private final NumberFormat kilometres;
    /** The part of a speed zone each box is. */
    private final List<Optional<SpeedZone>> zones;
    /** The number of the waypoint of each box; 0 without waypoint. */
    private final int[] numbers;
    /** The height of a line of boxes, in millimetres. */
    private final float rowHeight;
    /** The width of the description box, in millimetres. */
    private final float descriptionWidth;

    /**
     * Prepares the printing of a roadbook.
     *
     * @param document the document.
     * @param roadbook the roadbook.
     * @param texts the texts around the boxes.
     * @param catalogue the catalogue of symbols.
     */
    private Printer(final PDDocument document, final Roadbook roadbook, final Texts texts, final Catalogue catalogue) {
      this.document = document;
      this.roadbook = roadbook;
      this.texts = texts;
      this.catalogue = catalogue;
      this.kilometres = NumberFormat.getNumberInstance(texts.locale());
      kilometres.setMinimumFractionDigits(DECIMALS);
      kilometres.setMaximumFractionDigits(DECIMALS);
      this.zones = SpeedZone.along(roadbook.boxes());
      this.numbers = numbers(roadbook.boxes(), zones);
      final float height = PDRectangle.A5.getHeight() / POINTS_PER_MM;
      this.rowHeight = (height - BINDING - HEADER - FOOTER - BOTTOM) / ROWS;
      this.descriptionWidth = PDRectangle.A5.getWidth() / POINTS_PER_MM - 2 * SIDE - DISTANCE_WIDTH - DIAGRAM_WIDTH;
    }

    /**
     * Prints the pages: {@value #ROWS} boxes on each, the last one with the boxes left.
     *
     * @throws IOException if a page cannot be written, or a box drawn.
     */
    private void print() throws IOException {
      final List<Box> boxes = roadbook.boxes();
      for (int first = 0; first < boxes.size(); first += ROWS) {
        final PDPage page = new PDPage(PDRectangle.A5);
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
          final float top = PDRectangle.A5.getHeight() / POINTS_PER_MM - BINDING;
          header(content, first, top);
          for (int i = first; i < Math.min(first + ROWS, boxes.size()); i++) {
            box(content, i, top - HEADER - (i - first) * rowHeight);
          }
          footer(content);
        }
      }
    }

    /**
     * Prints the header line of a page: its number, the name and the length of the roadbook, and the kilometres from
     * its first box to the finish.
     *
     * @param content the content of the page.
     * @param first the index of the first box of the page.
     * @param top the top of the header, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void header(final PDPageContentStream content, final int first, final float top) throws IOException {
      final float width = DISTANCE_WIDTH + DIAGRAM_WIDTH + descriptionWidth;
      final float bottom = top - HEADER;
      final float middle = width - PAGE_NUMBER_WIDTH - REMAINING_WIDTH;
      grid(content, SIDE, bottom, PAGE_NUMBER_WIDTH, HEADER);
      grid(content, SIDE + PAGE_NUMBER_WIDTH, bottom, middle, HEADER);
      grid(content, SIDE + PAGE_NUMBER_WIDTH + middle, bottom, REMAINING_WIDTH, HEADER);
      final float baseline = bottom + (HEADER - HEADER_SIZE * CAP_HEIGHT / POINTS_PER_MM) / 2;
      final String length = NAME_SEPARATOR + km(roadbook.distance()) + KILOMETRES;
      final float room = mm(middle - 2 * INSET) - width(length, bold, HEADER_SIZE);
      text(content, Integer.toString(first / ROWS + 1), HEADER_SIZE, SIDE + INSET, baseline);
      text(content, fit(printable(texts.name(), bold), bold, HEADER_SIZE, room) + length, HEADER_SIZE,
           SIDE + PAGE_NUMBER_WIDTH + INSET, baseline);
      final String remaining = km(roadbook.distance() - roadbook.boxes().get(first).total());
      text(content, remaining, HEADER_SIZE,
           SIDE + width - INSET - width(remaining, bold, HEADER_SIZE) / POINTS_PER_MM, baseline);
    }

    /**
     * Prints a line of boxes: the distances, the diagram and the description of a box, then the frames of its zone
     * and of its danger.
     *
     * @param content the content of the page.
     * @param index the index of the box.
     * @param top the top of the line, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written, or the box drawn.
     */
    private void box(final PDPageContentStream content, final int index, final float top) throws IOException {
      final Box box = roadbook.boxes().get(index);
      final float bottom = top - rowHeight;
      distances(content, index, bottom);
      final float diagram = SIDE + DISTANCE_WIDTH;
      grid(content, diagram, bottom, DIAGRAM_WIDTH, rowHeight);
      if (box.tulip().isPresent() || box.notes().hasDiagram()) {
        image(content, DiagramRenderer.png(box.tulip(), box.notes(), catalogue), diagram, bottom, DIAGRAM_WIDTH,
              rowHeight);
      }
      description(content, index, bottom);
      final float width = DISTANCE_WIDTH + DIAGRAM_WIDTH + descriptionWidth;
      final boolean danger = box.notes().danger().filter(Danger.LEVEL_3::equals).isPresent();
      if (zones.get(index).isPresent()) {
        final float inset = danger ? FRAME / POINTS_PER_MM : 0;
        frame(content, catalogue.notes().zone(), SIDE + inset, bottom + inset, width - 2 * inset,
              rowHeight - 2 * inset);
      }
      if (danger) {
        frame(content, catalogue.notes().danger(), SIDE, bottom, width, rowHeight);
      }
    }

    /**
     * Prints the distance box of a box: filled if the box is close to the one before, its total and partial distances,
     * the arrow if the next box is close, and the mark of its waypoint.
     *
     * @param content the content of the page.
     * @param index the index of the box.
     * @param bottom the bottom of the line, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void distances(final PDPageContentStream content, final int index, final float bottom)
        throws IOException {
      final List<Box> boxes = roadbook.boxes();
      final Box box = boxes.get(index);
      if (index > 0 && box.total() - boxes.get(index - 1).total() < CLOSE) {
        content.setNonStrokingColor(catalogue.strip().close());
        content.addRect(mm(SIDE), mm(bottom), mm(DISTANCE_WIDTH), mm(rowHeight));
        content.fill();
      }
      grid(content, SIDE, bottom, DISTANCE_WIDTH, rowHeight);
      text(content, km(box.total()), TOTAL_SIZE, SIDE + INSET,
           bottom + rowHeight - INSET - TOTAL_SIZE * CAP_HEIGHT / POINTS_PER_MM);
      grid(content, SIDE + INSET, bottom + INSET, PARTIAL_WIDTH, PARTIAL_HEIGHT);
      text(content, km(box.partial()), PARTIAL_SIZE, SIDE + 2 * INSET,
           bottom + INSET + (PARTIAL_HEIGHT - PARTIAL_SIZE * CAP_HEIGHT / POINTS_PER_MM) / 2);
      if (index + 1 < boxes.size() && boxes.get(index + 1).total() - box.total() < CLOSE) {
        arrow(content, SIDE + INSET + PARTIAL_WIDTH + ARROW_GAP, bottom + INSET,
              bottom + INSET + PARTIAL_HEIGHT);
      }
      final float x = SIDE + DISTANCE_WIDTH - INSET - MARK / 2;
      final float y = bottom + INSET + MARK / 2;
      final Optional<WaypointKind> marked = box.waypoint().map(Waypoint::kind).filter(WaypointKind::isMarked);
      if (marked.isPresent()) {
        mark(content, marked.get(), x, y);
      }
      if (zones.get(index).filter(zone -> zone.finished().isPresent()).isPresent()) {
        mark(content, WaypointKind.FZ, x, y + MARK + INSET);
      }
    }

    /**
     * Prints the description box of a box: its heading, its description and the number of its waypoint.
     *
     * @param content the content of the page.
     * @param index the index of the box.
     * @param bottom the bottom of the line, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written, or the description drawn.
     */
    private void description(final PDPageContentStream content, final int index, final float bottom)
        throws IOException {
      final Box box = roadbook.boxes().get(index);
      final float left = SIDE + DISTANCE_WIDTH + DIAGRAM_WIDTH;
      grid(content, left, bottom, descriptionWidth, rowHeight);
      final float right = left + descriptionWidth - INSET;
      if (catalogue.headings().shows(box)) {
        final String heading = printable(new MessageFormat(texts.heading(), texts.locale())
                                             .format(new Object[] {String.valueOf(box.heading().orElseThrow())}),
                                         bold);
        text(content, heading, HEADING_SIZE, right - width(heading, bold, HEADING_SIZE) / POINTS_PER_MM,
             bottom + rowHeight - INSET - HEADING_SIZE * CAP_HEIGHT / POINTS_PER_MM);
      }
      if (DescriptionRenderer.has(box.notes(), zones.get(index))) {
        image(content, DescriptionRenderer.png(box.notes(), zones.get(index), catalogue), left, bottom,
              descriptionWidth, rowHeight - HEADING_HEIGHT);
      }
      if (box.waypoint().filter(waypoint -> waypoint.kind().isMarked()).isPresent()) {
        final String number = Integer.toString(numbers[index]);
        final float x = right - MARK / 2;
        final float y = bottom + INSET + MARK / 2;
        circle(content, x, y, Color.WHITE, catalogue.notes().text());
        centred(content, number, NUMBER_SIZE, catalogue.notes().text(), x, y);
      }
    }

    /**
     * Prints the mark of a waypoint: its letters in a disc of the colours of the catalogue.
     *
     * @param content the content of the page.
     * @param kind the kind of the waypoint, marked in the roadbook.
     * @param x the centre of the disc, in millimetres from the left of the page.
     * @param y the centre of the disc, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void mark(final PDPageContentStream content, final WaypointKind kind, final float x, final float y)
        throws IOException {
      final WaypointMark mark = catalogue.waypoints().get(kind);
      circle(content, x, y, mark.fill(), mark.ink());
      centred(content, kind.mark(), kind.mark().length() == 2 ? SMALL_MARK_SIZE : MARK_SIZE, mark.ink(), x, y);
    }

    /**
     * Prints the reminder that the route is indicative and the attribution of OpenStreetMap at the bottom of a page,
     * in as many lines as fit.
     *
     * @param content the content of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void footer(final PDPageContentStream content) throws IOException {
      final float room = mm(DISTANCE_WIDTH + DIAGRAM_WIDTH + descriptionWidth);
      final String reminder = printable(texts.notice() + " © " + OpenRallyWriter.OSM_AUTHOR + ", ODbL.", plain);
      final List<String> lines = lines(reminder, plain, FOOTER_SIZE, room);
      final int fitting = (int) (mm(FOOTER) / FOOTER_LEADING);
      content.setNonStrokingColor(catalogue.notes().text());
      for (int i = 0; i < Math.min(lines.size(), fitting); i++) {
        content.beginText();
        content.setFont(plain, FOOTER_SIZE);
        content.newLineAtOffset(mm(SIDE), mm(BOTTOM + FOOTER) - (i + 1) * FOOTER_LEADING);
        content.showText(lines.get(i));
        content.endText();
      }
    }

    /**
     * Writes a text in bold, in the colour of the text of the catalogue.
     *
     * @param content the content of the page.
     * @param text the text, written by the font.
     * @param size the size of the font, in points.
     * @param x the left of the text, in millimetres from the left of the page.
     * @param y the baseline of the text, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void text(final PDPageContentStream content, final String text, final float size, final float x,
                      final float y) throws IOException {
      content.setNonStrokingColor(catalogue.notes().text());
      content.beginText();
      content.setFont(bold, size);
      content.newLineAtOffset(mm(x), mm(y));
      content.showText(text);
      content.endText();
    }

    /**
     * Writes a text in bold, centred on a point.
     *
     * @param content the content of the page.
     * @param text the text, written by the font.
     * @param size the size of the font, in points.
     * @param colour the colour of the text.
     * @param x the centre, in millimetres from the left of the page.
     * @param y the centre, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void centred(final PDPageContentStream content, final String text, final float size, final Color colour,
                         final float x, final float y) throws IOException {
      content.setNonStrokingColor(colour);
      content.beginText();
      content.setFont(bold, size);
      content.newLineAtOffset(mm(x) - width(text, bold, size) / 2, mm(y) - size * CAP_HEIGHT / 2);
      content.showText(text);
      content.endText();
    }

    /**
     * Draws the frame of a box, in the colour of the text of the catalogue.
     *
     * @param content the content of the page.
     * @param x the left of the box, in millimetres from the left of the page.
     * @param y the bottom of the box, in millimetres from the bottom of the page.
     * @param width the width of the box, in millimetres.
     * @param height the height of the box, in millimetres.
     *
     * @throws IOException if the page cannot be written.
     */
    private void grid(final PDPageContentStream content, final float x, final float y, final float width,
                      final float height) throws IOException {
      content.setStrokingColor(catalogue.notes().text());
      content.setLineWidth(GRID);
      content.addRect(mm(x), mm(y), mm(width), mm(height));
      content.stroke();
    }

    /**
     * Draws a frame around a line of boxes, inside it.
     *
     * @param content the content of the page.
     * @param colour the colour of the frame.
     * @param x the left of the line, in millimetres from the left of the page.
     * @param y the bottom of the line, in millimetres from the bottom of the page.
     * @param width the width of the line, in millimetres.
     * @param height the height of the line, in millimetres.
     *
     * @throws IOException if the page cannot be written.
     */
    private static void frame(final PDPageContentStream content, final Color colour, final float x, final float y,
                              final float width, final float height) throws IOException {
      content.setStrokingColor(colour);
      content.setLineWidth(FRAME);
      content.addRect(mm(x) + FRAME / 2, mm(y) + FRAME / 2, mm(width) - FRAME, mm(height) - FRAME);
      content.stroke();
    }

    /**
     * Draws the vertical arrow of a box close to the next one, pointing down (§5.6.11).
     *
     * @param content the content of the page.
     * @param x the line of the arrow, in millimetres from the left of the page.
     * @param tip the tip of the arrow, in millimetres from the bottom of the page.
     * @param tail the tail of the arrow, in millimetres from the bottom of the page.
     *
     * @throws IOException if the page cannot be written.
     */
    private void arrow(final PDPageContentStream content, final float x, final float tip, final float tail)
        throws IOException {
      content.setStrokingColor(catalogue.notes().text());
      content.setNonStrokingColor(catalogue.notes().text());
      content.setLineWidth(FRAME);
      content.moveTo(mm(x), mm(tail));
      content.lineTo(mm(x), mm(tip + ARROW_LENGTH));
      content.stroke();
      content.moveTo(mm(x - ARROW_HEAD), mm(tip + ARROW_LENGTH));
      content.lineTo(mm(x + ARROW_HEAD), mm(tip + ARROW_LENGTH));
      content.lineTo(mm(x), mm(tip));
      content.closePath();
      content.fill();
    }

    /**
     * Draws a disc of {@value #MARK} mm, filled and ringed.
     *
     * @param content the content of the page.
     * @param x the centre, in millimetres from the left of the page.
     * @param y the centre, in millimetres from the bottom of the page.
     * @param fill the colour of the disc.
     * @param ring the colour of its ring.
     *
     * @throws IOException if the page cannot be written.
     */
    private static void circle(final PDPageContentStream content, final float x, final float y, final Color fill,
                               final Color ring) throws IOException {
      final float cx = mm(x);
      final float cy = mm(y);
      final float r = mm(MARK / 2);
      final float k = r * CIRCLE;
      content.setNonStrokingColor(fill);
      content.setStrokingColor(ring);
      content.setLineWidth(GRID);
      content.moveTo(cx + r, cy);
      content.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
      content.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
      content.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
      content.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
      content.closePath();
      content.fillAndStroke();
    }

    /**
     * Draws a square PNG image of the catalogue in the middle of a box, as large as the box allows.
     *
     * @param content the content of the page.
     * @param png the bytes of the image.
     * @param x the left of the box, in millimetres from the left of the page.
     * @param y the bottom of the box, in millimetres from the bottom of the page.
     * @param width the width of the box, in millimetres.
     * @param height the height of the room of the image, from the bottom of the box, in millimetres.
     *
     * @throws IOException if the image cannot be read, or the page written.
     */
    private void image(final PDPageContentStream content, final byte[] png, final float x, final float y,
                       final float width, final float height) throws IOException {
      final PDImageXObject image = PDImageXObject.createFromByteArray(document, png, null);
      final float side = Math.min(width, height) - 2 * INSET;
      content.drawImage(image, mm(x + (width - side) / 2), mm(y + (height - side) / 2), mm(side), mm(side));
    }

    /**
     * Formats a distance in kilometres, with {@value #DECIMALS} decimals, in the format of the language.
     *
     * @param metres the distance, in metres.
     *
     * @return the distance, in kilometres.
     */
    private String km(final double metres) {
      return kilometres.format(metres / METRES_PER_KILOMETRE);
    }

  }

}
