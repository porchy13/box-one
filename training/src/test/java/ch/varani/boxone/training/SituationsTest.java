package ch.varani.boxone.training;

import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link Situations}: the situations written by hand, their names, their themes, and the rules of the FIM
 * their boxes follow.
 */
final class SituationsTest {

  /**
   * Every theme has situations, each with a name of its own; all of them fill a session.
   */
  @Test
  @DisplayName("Phase 7: situations of every theme, each named once, enough for a session")
  void allTest() {
    final Set<String> names = Situations.all().stream().map(Situation::name).collect(Collectors.toSet());
    final Set<Theme> themes = Situations.all().stream().map(Situation::theme).collect(Collectors.toSet());
    assertAll(
        () -> assertEquals(Situations.all().size(), names.size()),
        () -> assertEquals(EnumSet.allOf(Theme.class), themes),
        () -> assertTrue(Situations.all().size() >= QuizGenerator.MAX_QUESTIONS));
  }

  /**
   * The situations of some themes are those of these themes only, in their order.
   */
  @Test
  @DisplayName("Phase 7: the situations of the themes chosen")
  void ofTest() {
    final Set<Theme> zones = Set.of(Theme.SPEED_ZONES);
    assertAll(
        () -> assertTrue(Situations.of(zones).stream().allMatch(situation -> situation.theme() == Theme.SPEED_ZONES)),
        () -> assertEquals(Situations.all().stream().filter(situation -> situation.theme() == Theme.SPEED_ZONES)
                                     .toList(), Situations.of(zones)),
        () -> assertEquals(Situations.all(), Situations.of(EnumSet.allOf(Theme.class))));
  }

  /**
   * A danger of level 3 always has a WPS (FIM, art. 11.4); every box has a tulip.
   */
  @Test
  @DisplayName("Phase 7: the boxes of the situations follow the rules of the FIM")
  void rulesTest() {
    assertTrue(Situations.all().stream().flatMap(situation -> situation.boxes().stream())
                         .allMatch(box -> box.tulip().isPresent() && (box.notes().danger().isEmpty()
                                          || box.notes().danger().get() != Danger.LEVEL_3 || safety(box))));
  }

  /**
   * Tells whether a box has a WPS.
   *
   * @param box the box.
   *
   * @return {@code true} if its waypoint is a WPS.
   */
  private static boolean safety(final Box box) {
    return box.waypoint().map(Waypoint::kind).equals(Optional.of(WaypointKind.WPS));
  }

}
