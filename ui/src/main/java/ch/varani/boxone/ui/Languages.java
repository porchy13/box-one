package ch.varani.boxone.ui;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The languages of the application (AGENTS.md §12): the language of the system chooses the texts and the formats of
 * the user interface.
 *
 * <ul>
 *   <li>French: texts in French, formats of {@code fr-CH};</li>
 *   <li>English or any other language: texts in English, formats of {@code en-GB}.</li>
 * </ul>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class Languages {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** French of Switzerland. */
  static final Locale FRENCH = Locale.forLanguageTag("fr-CH");
  /** British English, for English and the other languages. */
  static final Locale ENGLISH = Locale.forLanguageTag("en-GB");
  /** Every language of the application. */
  static final List<Locale> ALL = List.of(FRENCH, ENGLISH);
  /** The base name of the bundles of texts of the user interface. */
  static final String MESSAGES = "ch.varani.boxone.ui.messages";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Languages() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Chooses the locale of the application from the locale of the system.
   *
   * @param system the locale of the system.
   *
   * @return {@link #FRENCH} or {@link #ENGLISH}.
   */
  static Locale resolve(final Locale system) {
    return "fr".equals(system.getLanguage()) ? FRENCH : ENGLISH;
  }

  /**
   * Loads the texts of the user interface: the bundle of the language of the locale, or the default bundle
   * (English).
   *
   * <p>{@link ResourceBundle#getBundle(String, Locale, Module)} falls back to the default locale of the JVM before the
   * default bundle; the bundle found is therefore kept only if it is of the requested language.</p>
   *
   * @param locale the locale of the application.
   *
   * @return the bundle.
   */
  static ResourceBundle bundle(final Locale locale) {
    final ResourceBundle found = ResourceBundle.getBundle(MESSAGES, locale, Languages.class.getModule());
    if (found.getLocale().getLanguage().equals(locale.getLanguage())) {
      return found;
    }
    return ResourceBundle.getBundle(MESSAGES, Locale.ROOT, Languages.class.getModule());
  }

}
