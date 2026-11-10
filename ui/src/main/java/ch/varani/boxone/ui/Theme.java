package ch.varani.boxone.ui;

import javafx.application.ColorScheme;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The theme of the window (DESIGN.md §2, §3, §12): the colours of the system, the style sheets of the light or dark
 * tokens, then the base and the components, following the mode and the colours of the system.
 *
 * <p><b>Algorithm</b>: the scene first gets the tokens taken from the accent, the background and the text of the system
 * ({@link Palette}), as a style sheet built in memory ({@code data:} address); then {@value #DARK_TOKENS} when the
 * system is in dark mode, {@value #LIGHT_TOKENS} otherwise, which hold the tokens that do not come from the system
 * (errors, warnings, success, shadow); then {@value #BASE} and {@value #COMPONENTS}. The style sheets are applied again
 * when the mode or a colour of the system changes. The page of the map, which cannot read the style sheets of JavaFX,
 * gets the values of all the tokens ({@link #tokens()}).</p>
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
  /** The beginning of the address of a style sheet built in memory, its content following in base 64. */
  static final String DATA = "data:text/css;base64,";
  /** The selectors of the tokens, as in the style sheets of the tokens: the window and the dialogs. */
  private static final String SELECTORS = ".root, .dialog-pane {\n";
  /** A token of a style sheet of tokens: its name ({@code -c-…}) and its value, up to the semicolon. */
  private static final Pattern TOKEN = Pattern.compile("(-c-[a-z0-9-]++)\\s*+:\\s*+([^;]++);");

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The scene of the window. */
  private final Scene scene;
  /** The mode and the colours of the system. */
  private final SystemColors system;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Applies the theme following the system to a scene, and follows the changes of the system.
   *
   * @param scene the scene of the window.
   * @param system the mode and the colours of the system ({@code SystemColors.of(Platform.getPreferences())}).
   */
  Theme(final Scene scene, final SystemColors system) {
    this.scene = scene;
    this.system = system;
    addListener(this::apply);
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
    return system.scheme().getValue() == ColorScheme.DARK;
  }

  /**
   * Returns the tokens of the theme shown, for the page of the map.
   *
   * @return the names of the tokens ({@code -c-…}) and their values: those of the system, then those of the style
   *     sheet of the mode.
   */
  Map<String, String> tokens() {
    final Map<String, String> tokens = new LinkedHashMap<>(systemTokens());
    tokens.putAll(tokens(Theme.class.getResource(isDark() ? DARK_TOKENS : LIGHT_TOKENS)));
    return tokens;
  }

  /**
   * Follows the changes of the mode and of the colours of the system.
   *
   * @param listener called after the style sheets of the new mode or colours have been applied.
   */
  void addListener(final Runnable listener) {
    for (final ObservableValue<?> value : List.of(system.scheme(), system.accent(), system.background(),
                                                    system.text())) {
      value.addListener((property, before, after) -> listener.run());
    }
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
   * Applies the style sheets of the mode and of the colours of the system to the scene.
   */
  private void apply() {
    scene.getStylesheets().setAll(stylesheets(isDark(), systemTokens()));
  }

  /**
   * Returns the tokens taken from the current colours of the system.
   *
   * @return the names of the tokens ({@code -c-…}) and their values ({@link Palette#tokens}).
   */
  private Map<String, String> systemTokens() {
    return Palette.tokens(system.accent().getValue(), system.background().getValue(), system.text().getValue());
  }

  /**
   * Returns the style sheets of a theme, in the order they apply.
   *
   * @param dark {@code true} for the dark theme.
   * @param system the tokens taken from the colours of the system.
   *
   * @return the addresses of the tokens of the system (built in memory), the tokens of the mode, the base and the
   *     components.
   */
  static List<String> stylesheets(final boolean dark, final Map<String, String> system) {
    return List.of(sheet(system), url(dark ? DARK_TOKENS : LIGHT_TOKENS), url(BASE), url(COMPONENTS));
  }

  /**
   * Builds a style sheet of tokens in memory.
   *
   * @param tokens the names of the tokens ({@code -c-…}) and their values.
   *
   * @return the {@code data:} address of a style sheet defining the tokens on the window and the dialogs.
   */
  static String sheet(final Map<String, String> tokens) {
    final StringBuilder css = new StringBuilder(SELECTORS);
    tokens.forEach((name, value) -> css.append("    ").append(name).append(": ").append(value).append(";\n"));
    css.append("}\n");
    return DATA + Base64.getEncoder().encodeToString(css.toString().getBytes(StandardCharsets.UTF_8));
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
