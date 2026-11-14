package ch.varani.boxone.roadbook;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.routing.GraphHopperRouter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The collection of reference routes (development plan, §5; AGENTS.md §2): the non-regression suite of the roadbook
 * and of the tulips (phase 4). Each route crosses one kind of junction of the plan — a crossroads, a
 * roundabout, a fork, a slip road — on the small extract of OpenStreetMap of the tests of the routing (the centre of
 * Yverdon-les-Bains, © OpenStreetMap contributors, ODbL), routed by GraphHopper, imported once for the class.
 *
 * <p>A route is a text file of the test resources, {@code reference/<name>.txt}, in UTF-8: lines starting with
 * {@code #} are comments (the junction, the roads); {@code start = latitude, longitude} and
 * {@code finish = latitude, longitude} are the points clicked; each {@code box = } line is an expected box, in order:
 * its number, its place (5 decimals, about a metre), its total and partial distances rounded to the metre, and its
 * CAP in degrees ({@code -} at the finish); each {@code tulip = } line is the tulip of a box, in order: the number
 * of its box, its shape, the kind of its entry, its branches from the left to the right (angle and kind), and the
 * index of its exit among them.
 * The boxes and the tulips recorded are the ones generated when they were added: a change of the generator of boxes
 * or of tulips, of the routing or of the extract that changes one of them fails the test, to be checked and recorded
 * again on purpose.</p>
 */
final class ReferenceRoutesTest {

  /** The extract of OpenStreetMap of the tests, shared with the module routing. */
  private static final String MAP = "/ch/varani/boxone/routing/yverdon.osm.gz";
  /** The folder of the reference routes in the test resources, relative to this class. */
  private static final String FOLDER = "reference/";
  /** The extension of the files of the reference routes. */
  private static final String EXTENSION = ".txt";
  /** The start of a comment line. */
  private static final String COMMENT = "#";
  /** The separator of a key and its value. */
  private static final String KEY = "=";
  /** The separator of a latitude and a longitude. */
  private static final String COORDINATES = ",";
  /** The key of the start. */
  private static final String START = "start";
  /** The key of the finish. */
  private static final String FINISH = "finish";
  /** The key of a box. */
  private static final String BOX = "box";
  /** The key of a tulip. */
  private static final String TULIP = "tulip";
  /** The CAP written for a box without heading, the box of the finish. */
  private static final String NO_HEADING = "-";
  /** The format of a box: number, place, total, partial, CAP, as in the files. */
  private static final String BOX_FORMAT = "%d; %.5f, %.5f; %d; %d; %s";
  /** The format of a tulip: number of its box, shape, kind of the entry, branches, exit, as in the files. */
  private static final String TULIP_FORMAT = "%d; %s; %s; %s; %d";
  /** The format of a branch of a tulip: its angle and its kind. */
  private static final String BRANCH_FORMAT = "%d %s";
  /** The separator of the angles of a tulip. */
  private static final String ANGLES = ", ";

  /** The folder of the graph imported for the class. */
  @TempDir
  private static Path graph;
  /** The router of the graph imported for the class. */
  private static GraphHopperRouter router;

  /**
   * Imports the extract of the tests.
   *
   * @throws URISyntaxException never: the extract is a file of the test resources.
   */
  @BeforeAll
  static void importMap() throws URISyntaxException {
    router = GraphHopperRouter.importMap(Path.of(ReferenceRoutesTest.class.getResource(MAP).toURI()), graph);
  }

  /**
   * Releases the graph imported for the class.
   */
  @AfterAll
  static void close() {
    router.close();
  }

  /**
   * The roadbook of each reference route has the boxes recorded: a box at the start, one at the junction of the route,
   * one at the finish, at their places, with their distances and their CAP; and the tulip recorded at the junction.
   *
   * @param name the name of the reference route, its file without the extension.
   *
   * @throws IOException if the file of the route cannot be read.
   */
  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"crossroads", "roundabout", "fork", "slip-road"})
  @DisplayName("Reference routes: the roadbook of each junction of the collection has the boxes and the tulips "
               + "recorded")
  void boxesTest(final String name) throws IOException {
    final List<String> lines = read(name);
    final Route route = router.route(point(lines, START), point(lines, FINISH));
    final Roadbook roadbook = RoadbookGenerator.generate(route, router.junctions(route));
    assertAll(
        () -> assertEquals(values(lines, BOX), roadbook.boxes().stream().map(ReferenceRoutesTest::format).toList()),
        () -> assertEquals(values(lines, TULIP), roadbook.boxes().stream().filter(box -> box.tulip().isPresent())
            .map(box -> format(box.number(), box.tulip().orElseThrow())).toList()));
  }

  /**
   * Reads the lines of a reference route, without its comments and its blank lines.
   *
   * @param name the name of the route, its file without the extension.
   *
   * @return the lines of keys and values, in the order of the file.
   *
   * @throws IOException if the file cannot be read.
   */
  private static List<String> read(final String name) throws IOException {
    try (InputStream input = Objects.requireNonNull(ReferenceRoutesTest.class.getResourceAsStream(
        FOLDER + name + EXTENSION), name)) {
      final List<String> lines = new ArrayList<>();
      for (final String line : new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList()) {
        if (!line.isBlank() && !line.startsWith(COMMENT)) {
          lines.add(line);
        }
      }
      return lines;
    }
  }

  /**
   * Gives the values of a key of a reference route, trimmed.
   *
   * @param lines the lines of keys and values of the route.
   * @param key the key.
   *
   * @return the values of the key, in the order of the file; empty if the key is absent.
   */
  private static List<String> values(final List<String> lines, final String key) {
    final List<String> values = new ArrayList<>();
    for (final String line : lines) {
      final int separator = line.indexOf(KEY);
      if (line.substring(0, separator).trim().equals(key)) {
        values.add(line.substring(separator + 1).trim());
      }
    }
    return values;
  }

  /**
   * Gives the point of a key of a reference route, written {@code latitude, longitude}.
   *
   * @param lines the lines of keys and values of the route.
   * @param key the key of the point, present once.
   *
   * @return the point.
   */
  private static GeoPoint point(final List<String> lines, final String key) {
    final String[] coordinates = values(lines, key).getFirst().split(COORDINATES);
    return new GeoPoint(Double.parseDouble(coordinates[0].trim()), Double.parseDouble(coordinates[1].trim()));
  }

  /**
   * Writes a box as in the files of the reference routes.
   *
   * @param box the box.
   *
   * @return its number, its place to 5 decimals, its total and partial distances rounded to the metre, its CAP or
   *         {@value #NO_HEADING}.
   */
  private static String format(final Box box) {
    final String heading = box.heading().isPresent() ? String.valueOf(box.heading().getAsInt()) : NO_HEADING;
    return String.format(Locale.ROOT, BOX_FORMAT, box.number(), box.position().latitude(),
                         box.position().longitude(), Math.round(box.total()), Math.round(box.partial()), heading);
  }

  /**
   * Writes a tulip as in the files of the reference routes.
   *
   * @param number the number of its box.
   * @param tulip the tulip.
   *
   * @return the number of its box, its shape, the kind of its entry, its branches from the left to the right (angle and
   *         kind), the index of its exit.
   */
  private static String format(final int number, final Tulip tulip) {
    final String branches = IntStream.range(0, tulip.branches().size())
        .mapToObj(i -> String.format(Locale.ROOT, BRANCH_FORMAT, tulip.branches().get(i), tulip.kinds().get(i)))
        .collect(Collectors.joining(ANGLES));
    return String.format(Locale.ROOT, TULIP_FORMAT, number, tulip.shape(), tulip.entry(), branches, tulip.exit());
  }

}
