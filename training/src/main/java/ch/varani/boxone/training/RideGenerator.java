package ch.varani.boxone.training;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.roadbook.Geodesy;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Draws a route at random for a simulated run (development plan, phase 7c; decision of the maintainer, 2026-11-21:
 * the roadbook open, or a route drawn at random): its roadbook generated as the editor generates it, without notes.
 *
 * <p><b>Algorithm</b>, every draw from a generator {@value QuizGenerator#ALGORITHM} built from the seed (AGENTS.md §2):
 * up to {@value #MAX_ROUTES} pairs of points drawn uniformly in the area of the map seen, the start and the finish
 * {@value #MIN_TRIP} m apart at least; the shortest route between them on every road, the tracks included
 * ({@link Router#shortest}, as for the reading of boxes), is kept if it is {@value #MIN_LENGTH} m long at least;
 * cancelled by the interruption of its thread.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class RideGenerator {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The number of routes tried at most: chosen for Box One. */
  public static final int MAX_ROUTES = 20;
  /** The distance between the start and the finish, at least, in metres: chosen for Box One. */
  public static final double MIN_TRIP = 1000;
  /** The length of a route kept, at least, in metres: a few minutes of riding, chosen for Box One. */
  public static final double MIN_LENGTH = 2000;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RideGenerator() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Draws a route at random in an area and generates its roadbook.
   *
   * @param router the router of the map.
   * @param southWest the south-west corner of the area.
   * @param northEast the north-east corner of the area.
   * @param seed the seed of the draws.
   *
   * @return the roadbook of the first route long enough, or nothing if none is found.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws CancellationException if the thread is interrupted during the drawing.
   */
  public static Optional<Roadbook> random(final Router router, final GeoPoint southWest, final GeoPoint northEast,
                                          final long seed) {
    Objects.requireNonNull(router);
    final RandomGenerator random = RandomGeneratorFactory.of(QuizGenerator.ALGORITHM).create(seed);
    for (int attempt = 0; attempt < MAX_ROUTES; attempt++) {
      if (Thread.currentThread().isInterrupted()) {
        throw new CancellationException();
      }
      final GeoPoint start = ReadingGenerator.point(southWest, northEast, random);
      final GeoPoint finish = ReadingGenerator.point(southWest, northEast, random);
      final Optional<Route> route = Geodesy.distance(start, finish) < MIN_TRIP ? Optional.empty()
                                                                                : route(router, start, finish);
      if (route.isPresent()) {
        return Optional.of(RoadbookGenerator.generate(route.get(), router.junctions(route.get())));
      }
    }
    return Optional.empty();
  }

  /**
   * Routes from a start to a finish, the route kept if it is long enough.
   *
   * @param router the router of the map.
   * @param start the start.
   * @param finish the finish.
   *
   * @return the shortest route, or nothing if there is none or if it is shorter than {@value #MIN_LENGTH} m.
   */
  private static Optional<Route> route(final Router router, final GeoPoint start, final GeoPoint finish) {
    try {
      final Route route = router.shortest(start, finish);
      return Ride.of(route.points(), List.of()).length() < MIN_LENGTH ? Optional.empty()
                                                                                 : Optional.of(route);
    } catch (RoutingException _) {
      return Optional.empty();
    }
  }

}
