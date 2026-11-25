package ch.varani.boxone.training;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Junction;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.roadbook.Geodesy;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

/**
 * Draws the questions of a session of the reading of boxes (development plan, phase 7b; decisions of the maintainer,
 * 2026-11-20: the junctions of random routes on the map, the tulip read and the direction to take found among answers
 * in words, or the box read and the branch to take clicked on the map; as many questions as the junctions found,
 * {@value QuizGenerator#MAX_QUESTIONS} at most).
 *
 * <p><b>Inputs</b>: the router of the map imported; the area of the map, its south-west and north-east corners (the
 * area seen by the user); the exercises chosen, at least one; the seed of the session.</p>
 *
 * <p><b>Algorithm</b>, every draw from a generator {@value QuizGenerator#ALGORITHM} built from the seed (AGENTS.md §2:
 * the same seed, map and inputs give the same questions):</p>
 * <ol>
 *   <li>up to {@value #MAX_ROUTES} times, until {@value QuizGenerator#MAX_QUESTIONS} junctions are found: a start and a
 *       finish drawn uniformly in the area; closer than {@value #MIN_TRIP} m, they are drawn again; otherwise the
 *       shortest route between them on every road, the tracks included ({@link Router#shortest(GeoPoint, GeoPoint)};
 *       asked by the maintainer, 2026-11-20), a route that cannot be found skipped (a point far from the roads);</li>
 *   <li>the roadbook of the route generated with its junctions ({@link RoadbookGenerator}); each junction of a
 *       maneuver kept if it suits an exercise chosen ({@link Reading#suits(Exercise)}) and is farther than
 *       {@value #SAME_PLACE} m from the junctions kept before, with the route {@value #PATH_LENGTH} m either side;</li>
 *   <li>the junctions shuffled, the first {@value QuizGenerator#MAX_QUESTIONS} kept; each asked by an exercise drawn
 *       among those chosen that it suits; for the tulip, the other choices are the directions of the other branches of
 *       the tulip, then other directions of its kind (the turns of a crossing, the exits of a roundabout), drawn at
 *       random, {@value QuizGenerator#CHOICES} choices in all, shuffled.</li>
 * </ol>
 *
 * <p><b>Cancellation</b>: the drawing stops between two routes once its thread is interrupted.</p>
 *
 * <p>Utility class without state, safe for concurrent use if the router is.</p>
 */
public final class ReadingGenerator {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The largest number of routes drawn for a session (chosen for Box One: a few hundredths of a second each). */
  public static final int MAX_ROUTES = 40;
  /** The shortest distance between the start and the finish of a route drawn, in metres (chosen for Box One). */
  public static final double MIN_TRIP = 300;
  /** The distance within which two junctions are the same, in metres (chosen for Box One). */
  public static final double SAME_PLACE = 15;
  /** The length of the route kept either side of a junction, shown once it is answered, in metres. */
  public static final double PATH_LENGTH = 60;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private ReadingGenerator() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws the questions of a session.
   *
   * @param router the router of the map.
   * @param southWest the south-west corner of the area of the map.
   * @param northEast the north-east corner of the area of the map.
   * @param exercises the exercises chosen.
   * @param seed the seed of the draws.
   *
   * @return one question per junction found, {@value QuizGenerator#MAX_QUESTIONS} at most, in the order they are asked;
   *         none if no junction is found; unmodifiable.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws IllegalArgumentException if no exercise is chosen.
   * @throws CancellationException if the thread is interrupted during the drawing.
   */
  public static List<ReadingQuestion> questions(final Router router, final GeoPoint southWest,
                                                final GeoPoint northEast, final Set<Exercise> exercises,
                                                final long seed) {
    Objects.requireNonNull(router);
    if (exercises.isEmpty()) {
      throw new IllegalArgumentException("no exercise");
    }
    final RandomGenerator random = RandomGeneratorFactory.of(QuizGenerator.ALGORITHM).create(seed);
    final List<Reading> readings = new ArrayList<>();
    for (int attempt = 0; attempt < MAX_ROUTES && readings.size() < QuizGenerator.MAX_QUESTIONS; attempt++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      final GeoPoint start = point(southWest, northEast, random);
      final GeoPoint finish = point(southWest, northEast, random);
      if (Geodesy.distance(start, finish) >= MIN_TRIP) {
        collect(router, start, finish, exercises, readings);
      }
    }
    Collections.shuffle(readings, random);
    return readings.stream().limit(QuizGenerator.MAX_QUESTIONS).map(reading -> question(reading, exercises, random))
                   .toList();
  }

  /**
   * Draws a point in an area, uniformly in latitude and in longitude.
   *
   * @param southWest the south-west corner of the area.
   * @param northEast the north-east corner of the area.
   * @param random the generator of the draws.
   *
   * @return the point.
   */
  static GeoPoint point(final GeoPoint southWest, final GeoPoint northEast, final RandomGenerator random) {
    return new GeoPoint(southWest.latitude() + random.nextDouble() * (northEast.latitude() - southWest.latitude()),
                        southWest.longitude() + random.nextDouble() * (northEast.longitude() - southWest.longitude()));
  }

  /**
   * Routes from a start to a finish, and keeps the junctions of the route that suit an exercise and are not kept yet.
   *
   * @param router the router of the map.
   * @param start the start.
   * @param finish the finish.
   * @param exercises the exercises chosen.
   * @param readings the junctions kept, completed.
   */
  private static void collect(final Router router, final GeoPoint start, final GeoPoint finish,
                              final Set<Exercise> exercises, final List<Reading> readings) {
    final Route route;
    try {
      route = router.shortest(start, finish);
    } catch (RoutingException _) {
      return;
    }
    final List<Junction> junctions = router.junctions(route);
    final Roadbook roadbook = RoadbookGenerator.generate(route, junctions);
    for (int i = 0; i < junctions.size(); i++) {
      final Reading reading = new Reading(roadbook.boxes().get(i + 1), junctions.get(i),
                                          path(route.points(), route.maneuvers().get(i)));
      if (exercises.stream().anyMatch(reading::suits) && readings.stream().noneMatch(
          kept -> Geodesy.distance(kept.box().position(), reading.box().position()) < SAME_PLACE)) {
        readings.add(reading);
      }
    }
  }

  /**
   * Gives the points of a route around one of its points.
   *
   * @param points the points of the route.
   * @param index the index of the point.
   *
   * @return the points from {@value #PATH_LENGTH} m before the point, or the start, to {@value #PATH_LENGTH} m after
   *         it, or the finish.
   */
  static List<GeoPoint> path(final List<GeoPoint> points, final int index) {
    int first = index;
    double before = 0;
    while (first > 0 && before < PATH_LENGTH) {
      before += Geodesy.distance(points.get(first - 1), points.get(first));
      first--;
    }
    int last = index;
    double after = 0;
    while (last < points.size() - 1 && after < PATH_LENGTH) {
      after += Geodesy.distance(points.get(last), points.get(last + 1));
      last++;
    }
    return points.subList(first, last + 1);
  }

  /**
   * Asks a junction: an exercise drawn among those chosen that it suits, and, for the tulip, its choices.
   *
   * @param reading the junction.
   * @param exercises the exercises chosen.
   * @param random the generator of the draws.
   *
   * @return the question.
   */
  private static ReadingQuestion question(final Reading reading, final Set<Exercise> exercises,
                                          final RandomGenerator random) {
    final List<Exercise> suited = Arrays.stream(Exercise.values())
                                        .filter(exercise -> exercises.contains(exercise) && reading.suits(exercise))
                                        .toList();
    final Exercise exercise = suited.get(random.nextInt(suited.size()));
    return new ReadingQuestion(reading, exercise, exercise == Exercise.TULIP ? choices(reading, random) : List.of());
  }

  /**
   * Draws the choices of a question on a tulip: the direction to take, the directions of the other branches, then
   * other directions of the kind of the junction.
   *
   * @param reading the junction.
   * @param random the generator of the draws.
   *
   * @return {@value QuizGenerator#CHOICES} different directions, the direction to take among them, shuffled.
   */
  private static List<Turn> choices(final Reading reading, final RandomGenerator random) {
    final Turn right = reading.turn();
    final List<Turn> others = new ArrayList<>(Turn.of(reading.tulip()).stream().distinct()
                                                  .filter(turn -> !turn.equals(right)).toList());
    Collections.shuffle(others, random);
    final List<Turn> spare = new ArrayList<>(spare(reading.tulip()).stream()
                                                 .filter(turn -> !turn.equals(right) && !others.contains(turn))
                                                 .toList());
    Collections.shuffle(spare, random);
    others.addAll(spare);
    final List<Turn> choices = new ArrayList<>(others.subList(0, QuizGenerator.CHOICES - 1));
    choices.add(right);
    Collections.shuffle(choices, random);
    return choices;
  }

  /**
   * Gives the directions of the kind of a junction: the turns of a crossing, or the exits of a roundabout, one more
   * than its branches, {@value QuizGenerator#CHOICES} at least.
   *
   * @param tulip the tulip of the junction.
   *
   * @return the directions.
   */
  private static List<Turn> spare(final Tulip tulip) {
    if (tulip.shape() == JunctionShape.CROSSING) {
      return Arrays.stream(TurnKind.values()).filter(kind -> kind != TurnKind.ROUNDABOUT)
                   .map(kind -> new Turn(kind, 0)).toList();
    }
    final int exits = Math.max(tulip.branches().size() + 1, QuizGenerator.CHOICES);
    return IntStream.rangeClosed(1, exits).mapToObj(exit -> new Turn(TurnKind.ROUNDABOUT, exit)).toList();
  }

}
