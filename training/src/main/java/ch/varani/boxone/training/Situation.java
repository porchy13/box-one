package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A situation of a roadbook, written by hand for Box One (development plan, phase 7; decisions of the maintainer,
 * 2026-11-20: an extract of two or three boxes drawn as on paper, the question asked on one of them, how to behave
 * found among proposals): its name, its theme, its boxes and the box asked. The question, the {@value #BEHAVIOURS}
 * behaviours proposed — the first one right — and the explanation shown once answered are texts of the user
 * interface, named after the situation.
 *
 * <p><b>Preconditions</b>: the name is made of letters, a lowercase one first, so that it can name the texts; from
 * {@value #MIN_BOXES} to {@value #MAX_BOXES} boxes, numbered one after the other; the box asked is one of them. The
 * boxes are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param name the name of the situation, which names its texts.
 * @param theme the theme of the situation.
 * @param boxes the boxes of the extract, in their order; unmodifiable.
 * @param asked the index of the box the question is asked on, among the boxes.
 */
public record Situation(String name, Theme theme, List<Box> boxes, int asked) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The fewest boxes of an extract: the box asked and its neighbour (decision of the maintainer, 2026-11-20). */
  public static final int MIN_BOXES = 2;
  /** The most boxes of an extract (decision of the maintainer, 2026-11-20). */
  public static final int MAX_BOXES = 3;
  /** The number of the behaviours proposed for a situation, the first one right: as many as the choices of the quiz. */
  public static final int BEHAVIOURS = QuizGenerator.CHOICES;
  /** The form of a name: letters, a lowercase one first. */
  private static final Pattern NAME_PATTERN = Pattern.compile("[a-z][A-Za-z]*");

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a situation.
   *
   * @param name the name of the situation, which names its texts.
   * @param theme the theme of the situation.
   * @param boxes the boxes of the extract, in their order.
   * @param asked the index of the box the question is asked on.
   *
   * @throws NullPointerException if a value or a box is {@code null}.
   * @throws IllegalArgumentException if the name is not of its form, the boxes too few, too many or not numbered one
   *                                  after the other, or the box asked not one of them.
   */
  public Situation {
    Objects.requireNonNull(theme);
    boxes = List.copyOf(boxes);
    if (!NAME_PATTERN.matcher(name).matches() || boxes.size() < MIN_BOXES || boxes.size() > MAX_BOXES || asked < 0
        || asked >= boxes.size() || !numbered(boxes)) {
      throw new IllegalArgumentException("a situation of its form");
    }
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the box the question is asked on.
   *
   * @return the box asked.
   */
  public Box box() {
    return boxes.get(asked);
  }

  /**
   * Tells whether boxes are numbered one after the other.
   *
   * @param boxes the boxes, in their order.
   *
   * @return {@code true} if each box has the number of the one before it plus one.
   */
  private static boolean numbered(final List<Box> boxes) {
    for (int i = 1; i < boxes.size(); i++) {
      if (boxes.get(i).number() != boxes.get(i - 1).number() + 1) {
        return false;
      }
    }
    return true;
  }

}
