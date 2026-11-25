package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.SymbolPlace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/**
 * Dresses the roadbook of a simulated run with elements of the scene (development plan, phase 7c; asked by the
 * maintainer on 2026-11-21: trees, houses and castles at the roadside used in the tulips, not all the time, and holes
 * and bumps on the road): some boxes get a symbol beside the road placed in their diagram, some a symbol of an
 * obstacle on their exit, which the run then draws where the roadbook places them ({@link Ride#items()}).
 *
 * <p><b>Algorithm</b>, from a seed ({@code L64X128MixRandom}, the seed of the run): each box with a tulip and without
 * notes, so that the notes of the user are kept, draws one chance in {@value #SCENERY_SHARE} of a tree, a house or a
 * castle, placed at one of the corners of its diagram ({@link #SLOTS}) that no branch passes within {@value #CLEAR}°
 * (the entry, behind, is 45° from the corners), none if all are taken; then, if the next box is more than twice
 * {@value Ride#OBSTACLE} m farther, one chance in {@value #OBSTACLE_SHARE} of a bump or a hole on its exit. The same
 * seed and the same boxes give the same elements.</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
public final class RideScenery {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** One box in so many gets an element beside the road: chosen for Box One, not all the time. */
  public static final int SCENERY_SHARE = 3;
  /** One box in so many with room after it gets an obstacle on its exit: chosen for Box One. */
  public static final int OBSTACLE_SHARE = 4;
  /** The smallest angle between an element and a road of the diagram, in degrees: off the roads. */
  public static final double CLEAR = 30;
  /** The corners of the diagram where an element beside the road is placed: ahead right and left, behind them. */
  public static final List<DiagramPoint> SLOTS = List.of(new DiagramPoint(0.6, -0.6), new DiagramPoint(-0.6, -0.6),
                                                         new DiagramPoint(0.6, 0.6), new DiagramPoint(-0.6, 0.6));
  /** The elements beside the road. */
  private static final List<RideItem.Kind> SCENERY = List.of(RideItem.Kind.TREE, RideItem.Kind.HOUSE,
                                                             RideItem.Kind.CASTLE);
  /** The obstacles on the road. */
  private static final List<RideItem.Kind> OBSTACLES = List.of(RideItem.Kind.BUMP, RideItem.Kind.HOLE);
  /** The degrees of a full turn. */
  private static final double FULL_TURN = 360;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RideScenery() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Dresses the boxes of a roadbook.
   *
   * @param boxes the boxes, in their order.
   * @param seed the seed of the draws.
   *
   * @return the same boxes, some of them with the notes of an element beside the road or of an obstacle.
   */
  public static List<Box> dress(final List<Box> boxes, final long seed) {
    final RandomGenerator random = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
    final List<Box> dressed = new ArrayList<>(boxes.size());
    for (int i = 0; i < boxes.size(); i++) {
      final Box box = boxes.get(i);
      final double room = i + 1 < boxes.size() ? boxes.get(i + 1).total() - box.total() : 0;
      dressed.add(box.tulip().isPresent() && box.notes().equals(Notes.NONE) ? dress(box, room, random) : box);
    }
    return List.copyOf(dressed);
  }

  /**
   * Dresses a box with a tulip and without notes.
   *
   * @param box the box.
   * @param room the distance to the next box, in metres; 0 for the last box.
   * @param random the generator of the draws.
   *
   * @return the box, with the notes of the elements drawn, or as it is if none is.
   */
  private static Box dress(final Box box, final double room, final RandomGenerator random) {
    final List<BoxSymbol> symbols = new ArrayList<>();
    if (random.nextInt(SCENERY_SHARE) == 0) {
      final RideItem.Kind kind = SCENERY.get(random.nextInt(SCENERY.size()));
      slot(box.tulip().orElseThrow().branches(), random)
          .ifPresent(point -> symbols.add(BoxSymbol.free(kind.symbol(), point)));
    }
    if (room > 2 * Ride.OBSTACLE && random.nextInt(OBSTACLE_SHARE) == 0) {
      symbols.add(new BoxSymbol(OBSTACLES.get(random.nextInt(OBSTACLES.size())).symbol(), SymbolPlace.EXIT));
    }
    if (symbols.isEmpty()) {
      return box;
    }
    return new Box(box.number(), box.position(), box.total(), box.partial(), box.heading(), box.tulip(),
                   box.display(), Notes.NONE.withSymbols(symbols), box.waypoint());
  }

  /**
   * Chooses a corner of a diagram off its roads.
   *
   * @param branches the angles of the branches of the tulip but the entry, in degrees from straight on; the entry,
   *                 behind, is 45° from the corners behind, always clear.
   * @param random the generator of the draws.
   *
   * @return a corner farther than {@value #CLEAR}° from every branch, drawn among them; nothing if there is none.
   */
  static Optional<DiagramPoint> slot(final List<Integer> branches, final RandomGenerator random) {
    final List<DiagramPoint> free = SLOTS.stream().filter(point -> {
      final double angle = Math.toDegrees(Math.atan2(point.x(), -point.y()));
      return branches.stream().allMatch(branch -> apart(angle, branch) > CLEAR);
    }).toList();
    return free.isEmpty() ? Optional.empty() : Optional.of(free.get(random.nextInt(free.size())));
  }

  /**
   * Computes the angle between two directions, the shorter way round.
   *
   * @param first a direction, in degrees.
   * @param second another direction, in degrees.
   *
   * @return the angle, from 0 to 180 degrees.
   */
  private static double apart(final double first, final double second) {
    final double difference = Math.abs(first - second) % FULL_TURN;
    return Math.min(difference, FULL_TURN - difference);
  }

}
