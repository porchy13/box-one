package ch.varani.boxone.catalog;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the SVG paths of the catalogues (development plan, phase 4: "a catalogue = a file, JSON and SVG paths") into
 * the shapes of Java 2D.
 *
 * <p><b>Grammar</b>, a subset of the path data of SVG 1.1 (§8.3), enough for the symbols drawn for Box One: the
 * absolute commands {@code M} (move to: x y), {@code L} (line to: x y), {@code C} (cubic curve: x1 y1 x2 y2 x y) and
 * {@code Z} (close), each followed by its numbers, separated by spaces or commas; the path starts with {@code M}. A
 * path of more than {@value #MAX_LENGTH} characters, another command, a missing number or anything else is refused.
 * </p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class SvgPath {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The longest path read, in characters: a symbol of a roadbook, not a map. */
  static final int MAX_LENGTH = 10_000;
  /** The commands of the grammar, one character each. */
  private static final String COMMANDS = "MLCZ";
  /** The separators of the tokens: the white spaces of SVG and of the class {@code \s} of Java, and the comma. */
  private static final String SEPARATORS = " \t\n\u000B\f\r,";
  /** The digits of a number. */
  private static final String DIGITS = "0123456789";
  /** The numbers of a move or a line: the point reached. */
  private static final int POINT = 2;
  /** The numbers of a cubic curve: two control points and the point reached. */
  private static final int CURVE = 6;
  /** The index of the ordinate of the second control point among the numbers of a cubic curve. */
  private static final int CONTROL_Y = 3;
  /** The index of the abscissa of the point reached among the numbers of a cubic curve. */
  private static final int END_X = 4;
  /** The index of the ordinate of the point reached among the numbers of a cubic curve. */
  private static final int END_Y = 5;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private SvgPath() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Reads a path.
   *
   * @param data the path data, as in the attribute {@code d} of SVG.
   *
   * @return the shape of the path.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the path does not follow the grammar.
   */
  static Path2D.Double parse(final String data) {
    if (data.length() > MAX_LENGTH) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final List<String> tokens = tokens(data);
    if (tokens.isEmpty() || !"M".equals(tokens.getFirst())) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    final Path2D.Double path = new Path2D.Double();
    int index = 0;
    while (index < tokens.size()) {
      final String command = tokens.get(index++);
      final double[] numbers = numbers(tokens, index, count(command));
      index += numbers.length;
      switch (command) {
        case "M" -> path.moveTo(numbers[0], numbers[1]);
        case "L" -> path.lineTo(numbers[0], numbers[1]);
        case "C" -> path.curveTo(numbers[0], numbers[1], numbers[2], numbers[CONTROL_Y], numbers[END_X],
                                  numbers[END_Y]);
        default -> path.closePath();
      }
    }
    return path;
  }

  /**
   * Splits a path into its commands and its numbers, read character by character, in a time linear in the length of
   * the path (a regular expression searched at each position would take a time quadratic in it, Sonar java:S8786).
   *
   * @param data the path data.
   *
   * @return the tokens, in their order.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the path holds anything else.
   */
  private static List<String> tokens(final String data) {
    final List<String> tokens = new ArrayList<>();
    int index = separators(data, 0);
    while (index < data.length()) {
      final int end = token(data, index);
      tokens.add(data.substring(index, end));
      index = separators(data, end);
    }
    return tokens;
  }

  /**
   * Skips the separators of the tokens.
   *
   * @param data the path data.
   * @param from the index of the first character to look at.
   *
   * @return the index of the first character after the separators, or the length of the path.
   */
  private static int separators(final String data, final int from) {
    int index = from;
    while (index < data.length() && SEPARATORS.indexOf(data.charAt(index)) >= 0) {
      index++;
    }
    return index;
  }

  /**
   * Finds the end of the token that starts at a character: a command, or a number, an optional minus sign, digits,
   * and optionally a point and digits.
   *
   * @param data the path data.
   * @param from the index of the first character of the token, before the end of the path.
   *
   * @return the index of the first character after the token.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if no command or number starts there.
   */
  private static int token(final String data, final int from) {
    if (COMMANDS.indexOf(data.charAt(from)) >= 0) {
      return from + 1;
    }
    final int start = data.charAt(from) == '-' ? from + 1 : from;
    final int end = digits(data, start);
    if (end < data.length() && data.charAt(end) == '.') {
      return digits(data, end + 1);
    }
    return end;
  }

  /**
   * Finds the end of a run of digits, at least one.
   *
   * @param data the path data.
   * @param from the index of the first digit.
   *
   * @return the index of the first character after the digits.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if no digit starts there.
   */
  private static int digits(final String data, final int from) {
    int index = from;
    while (index < data.length() && DIGITS.indexOf(data.charAt(index)) >= 0) {
      index++;
    }
    if (index == from) {
      throw new CatalogueException(CatalogueError.INVALID);
    }
    return index;
  }

  /**
   * Gives the count of numbers that follow a command.
   *
   * @param command the token of the command.
   *
   * @return 2 for {@code M} and {@code L}, 6 for {@code C}, 0 for {@code Z}.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if the token is a number, not a command.
   */
  private static int count(final String command) {
    return switch (command) {
      case "M", "L" -> POINT;
      case "C" -> CURVE;
      case "Z" -> 0;
      default -> throw new CatalogueException(CatalogueError.INVALID);
    };
  }

  /**
   * Reads the numbers that follow a command.
   *
   * @param tokens the tokens of the path.
   * @param from the index of the first number.
   * @param count the count of numbers of the command.
   *
   * @return the numbers.
   *
   * @throws CatalogueException {@link CatalogueError#INVALID} if a number is missing.
   */
  private static double[] numbers(final List<String> tokens, final int from, final int count) {
    final double[] numbers = new double[count];
    for (int i = 0; i < count; i++) {
      if (from + i >= tokens.size() || Character.isLetter(tokens.get(from + i).charAt(0))) {
        throw new CatalogueException(CatalogueError.INVALID);
      }
      numbers[i] = Double.parseDouble(tokens.get(from + i));
    }
    return numbers;
  }

}
