package ch.varani.boxone.ui;

import javafx.application.ColorScheme;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The theme of the window (DESIGN.md §2, §3, §12): the style sheets of the light or dark tokens, then the base and the
 * components, following the light or dark mode of the system.
 *
 * <p><b>Algorithm</b>: the scene gets {@value #DARK_TOKENS} when the system is in dark mode, {@value #LIGHT_TOKENS}
 * otherwise, then {@value #BASE} and {@value #COMPONENTS}; the style sheets are applied again when the mode of the
 * system changes. The page of the map, which cannot read the style sheets of JavaFX, gets the values of the tokens
 * ({@link #tokens()}), read from the style sheet of the tokens of the mode. The manual choice of the theme and the
 * accent of the system come with the settings (DESIGN.md §14).</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class Theme {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The tokens of the light theme. */
  static final String LIGHT_TOKENS = "tokens-light.css";
  /** The tokens of the dark theme. */
  static final String DARK_TOKENS = "tokens-dark.css";
  /** The base styles. */
  static final String BASE = "base.css";
  /** The styles of the components. */
  static final String COMPONENTS = "components.css";
  /** A token of a style sheet of tokens: its name ({@code -c-…}) and its value, up to the semicolon. */
  private static final Pattern TOKEN = Pattern.compile("(-c-[a-z0-9-]++)\\s*+:\\s*+([^;]++);");

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The scene of the window. */
  private final Scene scene;
  /** The light or dark mode of the system. */
  private final ObservableValue<ColorScheme> scheme;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Applies the theme following the system to a scene, and follows the changes of the system.
   *
   * @param scene the scene of the window.
   * @param scheme the light or dark mode of the system ({@code Platform.getPreferences().colorSchemeProperty()}).
   */
  Theme(final Scene scene, final ObservableValue<ColorScheme> scheme) {
    this.scene = scene;
    this.scheme = scheme;
    scheme.addListener((property, before, after) -> apply());
    apply();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether the dark theme is shown.
   *
   * @return {@code true} for the dark theme.
   */
  boolean isDark() {
    return scheme.getValue() == ColorScheme.DARK;
  }

  /**
   * Returns the tokens of the theme shown, for the page of the map.
   *
   * @return the names of the tokens ({@code -c-…}) and their values, in the order of the style sheet.
   */
  Map<String, String> tokens() {
    return tokens(Theme.class.getResource(isDark() ? DARK_TOKENS : LIGHT_TOKENS));
  }

  /**
   * Follows the changes of the mode of the system.
   *
   * @param listener called after the style sheets of the new mode have been applied.
   */
  void addListener(final Runnable listener) {
    scheme.addListener((property, before, after) -> listener.run());
  }

  /**
   * Reads the tokens of a style sheet of tokens.
   *
   * @param css the address of the style sheet.
   *
   * @return the names of the tokens ({@code -c-…}) and their values, in the order of the style sheet.
   *
   * @throws UncheckedIOException if the style sheet cannot be read.
   */
  static Map<String, String> tokens(final URL css) {
    final String content;
    try (InputStream input = css.openStream()) {
      content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    final Map<String, String> tokens = new LinkedHashMap<>();
    final Matcher matcher = TOKEN.matcher(content);
    while (matcher.find()) {
      tokens.put(matcher.group(1), matcher.group(2).trim());
    }
    return tokens;
  }

  /**
   * Applies the style sheets of the mode of the system to the scene.
   */
  private void apply() {
    scene.getStylesheets().setAll(stylesheets(isDark()));
  }

  /**
   * Returns the style sheets of a theme, in the order they apply.
   *
   * @param dark {@code true} for the dark theme.
   *
   * @return the addresses of the tokens, the base and the components.
   */
  static List<String> stylesheets(final boolean dark) {
    return List.of(url(dark ? DARK_TOKENS : LIGHT_TOKENS), url(BASE), url(COMPONENTS));
  }

  /**
   * Returns the address of a style sheet of this package.
   *
   * @param name the name of the style sheet.
   *
   * @return its address.
   */
  private static String url(final String name) {
    return Theme.class.getResource(name).toExternalForm();
  }

}
