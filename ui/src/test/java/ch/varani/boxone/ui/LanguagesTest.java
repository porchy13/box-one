package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link Languages}: French for a French system, English for any other; the texts of the language, or the
 * default texts (English), whatever the default locale of the JVM.
 */
final class LanguagesTest {

  /**
   * The language of the system chooses the locale of the application.
   */
  @Test
  @DisplayName("French for a French system, English for any other language")
  void resolveTest() {
    assertAll(
        () -> assertEquals(Languages.FRENCH, Languages.resolve(Locale.FRANCE)),
        () -> assertEquals(Languages.FRENCH, Languages.resolve(Locale.forLanguageTag("fr-CH"))),
        () -> assertEquals(Languages.ENGLISH, Languages.resolve(Locale.US)),
        () -> assertEquals(Languages.ENGLISH, Languages.resolve(Locale.forLanguageTag("de-CH"))));
  }

  /**
   * The texts are those of the language of the application; English takes the default bundle.
   */
  @Test
  @DisplayName("The texts are those of the language, English by default")
  void bundleTest() {
    assertAll(
        () -> assertEquals("_Fichier", Languages.bundle(Languages.FRENCH).getString("menu.file")),
        () -> assertEquals("_File", Languages.bundle(Languages.ENGLISH).getString("menu.file")),
        () -> assertEquals("_File", Languages.bundle(Locale.GERMAN).getString("menu.file")));
  }

}
