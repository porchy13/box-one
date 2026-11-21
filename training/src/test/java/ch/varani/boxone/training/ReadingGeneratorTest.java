package ch.varani.boxone.training;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.routing.GraphHopperRouter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link ReadingGenerator}: the properties of the questions drawn with fixed seeds (AGENTS.md §6), on a grid
 * of streets without map ({@link GridRouter}), and on the real roads of the extract of the module routing (the centre
 * of Yverdon-les-Bains, © OpenStreetMap contributors, ODbL), imported once for the class.
 */
final class ReadingGeneratorTest {

  /** The extract of OpenStreetMap of the tests, shared with the module routing. */
  private static final String MAP = "/ch/varani/boxone/routing/yverdon.osm.gz";
  /** The south-west corner of the area of the tests, around the centre of Yverdon-les-Bains. */
  private static final GeoPoint SOUTH_WEST = new GeoPoint(46.7750, 6.6330);
  /** The north-east corner of the area of the tests. */
  private static final GeoPoint NORTH_EAST = new GeoPoint(46.7860, 6.6500);
  /** The north-east corner of an area too small for a route of the shortest trip. */
  private static final GeoPoint TINY = new GeoPoint(46.7751, 6.6331);
  /** The seed of the tests. */
  private static final long SEED = 2026;
  /** Both exercises. */
  private static final Set<Exercise> BOTH = EnumSet.allOf(Exercise.class);

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
    router = GraphHopperRouter.importMap(Path.of(ReadingGeneratorTest.class.getResource(MAP).toURI()), graph);
  }

  /**
   * Releases the graph imported for the class.
   */
  @AfterAll
  static void close() {
    router.close();
  }

  /**
   * On a grid of crossings, twenty junctions at most, all different, each asked by an exercise it suits, the tulip with
   * four choices, the direction to take among them; the same seed gives the same questions, another seed others.
   */
  @Test
  @DisplayName("Phase 7b: twenty junctions at most, each asked by an exercise it suits, the same for the same seed")
  void questionsTest() {
    final GridRouter grid = new GridRouter(GridRouter.Kind.CROSSING, false);
    final List<ReadingQuestion> questions = ReadingGenerator.questions(grid, SOUTH_WEST, NORTH_EAST, BOTH, SEED);
    final Set<GeoPoint> places = questions.stream().map(question -> question.reading().box().position())
                                          .collect(Collectors.toSet());
    assertAll(
        () -> assertEquals(QuizGenerator.MAX_QUESTIONS, questions.size()),
        () -> assertEquals(QuizGenerator.MAX_QUESTIONS, places.size()),
        () -> assertTrue(questions.stream().allMatch(question -> question.reading().suits(question.exercise()))),
        () -> assertTrue(questions.stream().filter(question -> question.exercise() == Exercise.TULIP)
                                  .allMatch(question -> question.choices().size() == QuizGenerator.CHOICES
                                                        && question.choices().contains(question.reading().turn()))),
        () -> assertEquals(BOTH, questions.stream().map(ReadingQuestion::exercise).collect(Collectors.toSet())),
        () -> assertEquals(questions, ReadingGenerator.questions(grid, SOUTH_WEST, NORTH_EAST, BOTH, SEED)),
        () -> assertNotEquals(questions, ReadingGenerator.questions(grid, SOUTH_WEST, NORTH_EAST, BOTH, SEED + 1)));
  }

  /**
   * The map only asks crossings on the map; a roundabout is read from its tulip, its exits as choices; a bend suits no
   * exercise; the same junctions are kept once; no route, or an area too small for a trip, gives no question.
   */
  @Test
  @DisplayName("Phase 7b: the exercises chosen, the junctions kept once, none without route")
  void kindsTest() {
    final List<ReadingQuestion> map = ReadingGenerator.questions(new GridRouter(GridRouter.Kind.CROSSING, false),
                                                                 SOUTH_WEST, NORTH_EAST, Set.of(Exercise.MAP), SEED);
    final List<ReadingQuestion> roundabouts = ReadingGenerator.questions(
        new GridRouter(GridRouter.Kind.ROUNDABOUT, false), SOUTH_WEST, NORTH_EAST, BOTH, SEED);
    assertAll(
        () -> assertTrue(map.stream().allMatch(question -> question.exercise() == Exercise.MAP
                                                           && question.choices().isEmpty())),
        () -> assertFalse(roundabouts.isEmpty()),
        () -> assertTrue(roundabouts.stream().allMatch(
            question -> question.exercise() == Exercise.TULIP
                        && question.choices().stream().allMatch(turn -> turn.kind() == TurnKind.ROUNDABOUT))),
        () -> assertEquals(List.of(), ReadingGenerator.questions(new GridRouter(GridRouter.Kind.BEND, false),
                                                                 SOUTH_WEST, NORTH_EAST, BOTH, SEED)),
        () -> assertEquals(2, ReadingGenerator.questions(new GridRouter(GridRouter.Kind.CROSSING, true), SOUTH_WEST,
                                                         NORTH_EAST, BOTH, SEED).size()),
        () -> assertEquals(List.of(), ReadingGenerator.questions(new GridRouter(GridRouter.Kind.NONE, false),
                                                                 SOUTH_WEST, NORTH_EAST, BOTH, SEED)),
        () -> assertEquals(List.of(), ReadingGenerator.questions(new GridRouter(GridRouter.Kind.CROSSING, false),
                                                                 SOUTH_WEST, TINY, BOTH, SEED)));
  }

  /**
   * On the real roads of Yverdon-les-Bains, junctions are found, each suiting its exercise.
   */
  @Test
  @DisplayName("Phase 7b: on real roads, junctions of random routes are read")
  void realRoadsTest() {
    final List<ReadingQuestion> questions = ReadingGenerator.questions(router, SOUTH_WEST, NORTH_EAST, BOTH, SEED);
    assertAll(
        () -> assertFalse(questions.isEmpty()),
        () -> assertTrue(questions.size() <= QuizGenerator.MAX_QUESTIONS),
        () -> assertTrue(questions.stream().allMatch(question -> question.reading().suits(question.exercise()))));
  }

  /**
   * The route around a junction: back to the point at the length before it, or to the start, on to the point at the
   * length after it, or to the finish.
   */
  @Test
  @DisplayName("Phase 7b: the route kept around a junction")
  void pathTest() {
    final List<GeoPoint> points = List.of(Fixtures.SOUTH_POINT, Fixtures.CENTRE, Fixtures.east(Fixtures.AFTER),
                                          Fixtures.east(Fixtures.AFTER * 2));
    assertAll(
        () -> assertEquals(points, ReadingGenerator.path(points, 1)),
        () -> assertEquals(points.subList(0, points.size() - 1), ReadingGenerator.path(points, 0)),
        () -> assertEquals(points.subList(1, points.size()), ReadingGenerator.path(points, points.size() - 1)));
  }

  /**
   * An interrupted thread cancels the drawing; no exercise, or no router: a programming error.
   */
  @Test
  @DisplayName("Phase 7b: the drawing cancelled by the interruption of its thread")
  void invalidTest() {
    final GridRouter grid = new GridRouter(GridRouter.Kind.CROSSING, false);
    Thread.currentThread().interrupt();
    try {
      assertThrows(CancellationException.class,
                   () -> ReadingGenerator.questions(grid, SOUTH_WEST, NORTH_EAST, BOTH, SEED));
    } finally {
      Thread.interrupted();
    }
    final Set<Exercise> none = Set.of();
    assertAll(
        () -> assertThrows(IllegalArgumentException.class,
                           () -> ReadingGenerator.questions(grid, SOUTH_WEST, NORTH_EAST, none, SEED)),
        () -> assertThrows(NullPointerException.class,
                           () -> ReadingGenerator.questions(null, SOUTH_WEST, NORTH_EAST, BOTH, SEED)));
  }

}
