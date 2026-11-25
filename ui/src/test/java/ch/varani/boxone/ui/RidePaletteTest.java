package ch.varani.boxone.ui;

import ch.varani.boxone.core.WaypointKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RidePalette}: the colours of the simulated run read from the token style sheet.
 */
final class RidePaletteTest {

  /**
   * Every token of the run and every letter of the sprite of the motorbike has a colour, the same in both themes; a
   * dot is transparent; a token missing is a defect.
   */
  @Test
  @DisplayName("Phase 7c: the colours of the run from the tokens, the same in both themes")
  void colorsTest() {
    final RidePalette palette = RidePalette.load();
    final String everyLetter = String.join("", RideRenderer.MOTO).replace(".", "");
    assertAll(
        () -> assertTrue(everyLetter.chars().allMatch(letter -> palette.sprite((char) letter) != null)),
        () -> assertNull(palette.sprite('.')),
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.LIGHT_TOKENS)).get(RidePalette.SKY),
                           Theme.tokens(Theme.class.getResource(Theme.DARK_TOKENS)).get(RidePalette.SKY)),
        () -> assertThrows(IllegalArgumentException.class, () -> palette.color("-c-missing")),
        () -> assertTrue(Arrays.stream(WaypointKind.values())
                               .allMatch(kind -> palette.color(RidePalette.waypoint(kind)) != null)));
  }

}
