package ch.varani.boxone.ui;

import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The colours of the system turned into the colour tokens of the interface (DESIGN.md §3): the accent, the background
 * and the text of the operating system, and the neutral shades and the tints of the accent derived from them.
 *
 * <p><b>Algorithm</b>: the background of the system is the surface of the content ({@code -c-surface}), its text the
 * main text ({@code -c-text}), its accent the accent ({@code -c-accent}). The other neutral tokens mix the text into
 * the background, in the sRGB space, at a fixed share each (from {@value #WINDOW} for the background of the window to
 * {@value #TEXT_2} for the secondary text), so that they follow the system in light and in dark alike; the selection
 * and the halo of the focus mix the accent into the background. The text on the accent is the lighter of the
 * background and the text of the system, as the systems draw it, unless its contrast with the accent is below
 * {@value #ON_ACCENT_CONTRAST}:1; then the darker one. The opacity of the colours of the system is ignored.</p>
 *
 * <p>The tokens that do not come from the system (errors, warnings, success, shadow) stay in the style sheets of the
 * tokens ({@link Theme}).</p>
 *
 * <p>Stateless; thread-safe.</p>
 */
final class Palette {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The share of the text in the background of the window ({@code -c-bg}), DESIGN.md §3. */
  static final double WINDOW = 0.04;
  /** The share of the text in the side bar ({@code -c-sidebar}), DESIGN.md §3. */
  static final double SIDEBAR = 0.07;
  /** The share of the text in the light borders and the separators ({@code -c-border}), DESIGN.md §3. */
  static final double BORDER = 0.14;
  /** The share of the text in the borders of the fields and the buttons ({@code -c-border-strong}), DESIGN.md §3. */
  static final double BORDER_STRONG = 0.24;
  /** The share of the text in the secondary text ({@code -c-text-2}): at least 4.5:1 on the surface, DESIGN.md §3. */
  static final double TEXT_2 = 0.65;
  /** The share of the text in the tertiary text ({@code -c-text-3}), for non-essential information, DESIGN.md §3. */
  static final double TEXT_3 = 0.50;
  /** The share of the accent in the halo of the focus ({@code -c-accent-subtle}), DESIGN.md §3. */
  static final double ACCENT_SUBTLE = 0.12;
  /** The share of the accent in the selected row or element ({@code -c-selection}), DESIGN.md §3. */
  static final double SELECTION = 0.20;
  /**
   * The minimum contrast of the light text on the accent before the dark one is taken: 3:1, the contrast of the
   * large text and of the components in WCAG 2.2 (1.4.3, 1.4.11), DESIGN.md §3.
   */
  static final double ON_ACCENT_CONTRAST = 3.0;
  /** The largest value of a channel of a colour written as {@code #RRGGBB}. */
  private static final int CHANNEL = 255;
  /** The format of a colour in the style sheets: {@code #RRGGBB}. */
  private static final String HEX = "#%02X%02X%02X";
  /** The weight of the red in the relative luminance (WCAG 2.2, definition of the relative luminance). */
  private static final double RED = 0.2126;
  /** The weight of the green in the relative luminance (WCAG 2.2). */
  private static final double GREEN = 0.7152;
  /** The weight of the blue in the relative luminance (WCAG 2.2). */
  private static final double BLUE = 0.0722;
  /** The value of a channel up to which the sRGB curve is linear (IEC 61966-2-1, quoted by WCAG 2.2). */
  private static final double LINEAR_LIMIT = 0.04045;
  /** The slope of the linear part of the sRGB curve (IEC 61966-2-1). */
  private static final double LINEAR_SLOPE = 12.92;
  /** The offset of the curved part of the sRGB curve (IEC 61966-2-1). */
  private static final double CURVE_OFFSET = 0.055;
  /** The scale of the curved part of the sRGB curve (IEC 61966-2-1). */
  private static final double CURVE_SCALE = 1.055;
  /** The exponent of the curved part of the sRGB curve (IEC 61966-2-1). */
  private static final double CURVE_EXPONENT = 2.4;
  /** The flare added to both luminances of a contrast ratio (WCAG 2.2, definition of the contrast ratio). */
  private static final double FLARE = 0.05;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Palette() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Returns the colour tokens taken from the colours of the system.
   *
   * @param accent the accent of the system.
   * @param background the background of the system, the surface of the content.
   * @param text the text of the system, on that background.
   *
   * @return the names of the tokens ({@code -c-…}) and their values ({@code #RRGGBB}), in the order of DESIGN.md §3.
   */
  static Map<String, String> tokens(final Color accent, final Color background, final Color text) {
    final Map<String, String> tokens = new LinkedHashMap<>();
    tokens.put("-c-bg", mix(background, text, WINDOW));
    tokens.put("-c-surface", hex(background));
    tokens.put("-c-sidebar", mix(background, text, SIDEBAR));
    tokens.put("-c-border", mix(background, text, BORDER));
    tokens.put("-c-border-strong", mix(background, text, BORDER_STRONG));
    tokens.put("-c-text", hex(text));
    tokens.put("-c-text-2", mix(background, text, TEXT_2));
    tokens.put("-c-text-3", mix(background, text, TEXT_3));
    tokens.put("-c-accent", hex(accent));
    tokens.put("-c-accent-subtle", mix(background, accent, ACCENT_SUBTLE));
    tokens.put("-c-selection", mix(background, accent, SELECTION));
    tokens.put("-c-on-accent", hex(onAccent(accent, background, text)));
    return tokens;
  }

  /**
   * Returns the contrast ratio of two colours (WCAG 2.2), from 1 (the same luminance) to 21 (black and white).
   *
   * @param first a colour.
   * @param second another colour.
   *
   * @return the luminance of the lighter one plus the flare, divided by the luminance of the darker one plus the flare.
   */
  static double contrast(final Color first, final Color second) {
    final double one = luminance(first) + FLARE;
    final double other = luminance(second) + FLARE;
    return Math.max(one, other) / Math.min(one, other);
  }

  /**
   * Chooses the text on the accent: the lighter of the background and the text of the system, unless it is too close
   * to the accent.
   *
   * @param accent the accent of the system.
   * @param background the background of the system.
   * @param text the text of the system.
   *
   * @return the lighter colour when its contrast with the accent reaches {@value #ON_ACCENT_CONTRAST}:1, the darker one
   *     otherwise.
   */
  private static Color onAccent(final Color accent, final Color background, final Color text) {
    final boolean lightBackground = luminance(background) >= luminance(text);
    final Color light = lightBackground ? background : text;
    final Color dark = lightBackground ? text : background;
    return contrast(light, accent) >= ON_ACCENT_CONTRAST ? light : dark;
  }

  /**
   * Mixes a colour into another one, in the sRGB space.
   *
   * @param base the colour mixed into.
   * @param added the colour added.
   * @param share the share of the added colour, from 0 (the base) to 1 (the added colour).
   *
   * @return the mix, as {@code #RRGGBB}.
   */
  private static String mix(final Color base, final Color added, final double share) {
    return hex(base.interpolate(added, share));
  }

  /**
   * Writes a colour as in the style sheets, without its opacity.
   *
   * @param color the colour.
   *
   * @return the colour as {@code #RRGGBB}, in capitals.
   */
  private static String hex(final Color color) {
    return String.format(Locale.ROOT, HEX, channel(color.getRed()), channel(color.getGreen()),
                         channel(color.getBlue()));
  }

  /**
   * Turns a channel of a colour into its value in {@code #RRGGBB}.
   *
   * @param value the channel, from 0 to 1.
   *
   * @return the channel, from 0 to {@value #CHANNEL}, rounded.
   */
  private static int channel(final double value) {
    return (int) Math.round(value * CHANNEL);
  }

  /**
   * Returns the relative luminance of a colour (WCAG 2.2), its opacity ignored.
   *
   * @param color the colour.
   *
   * @return the luminance, from 0 (black) to 1 (white).
   */
  private static double luminance(final Color color) {
    return RED * linear(color.getRed()) + GREEN * linear(color.getGreen()) + BLUE * linear(color.getBlue());
  }

  /**
   * Turns a channel of the sRGB space into its linear value (IEC 61966-2-1).
   *
   * @param value the channel, from 0 to 1.
   *
   * @return the linear value, from 0 to 1.
   */
  private static double linear(final double value) {
    if (value <= LINEAR_LIMIT) {
      return value / LINEAR_SLOPE;
    }
    return Math.pow((value + CURVE_OFFSET) / CURVE_SCALE, CURVE_EXPONENT);
  }

}
