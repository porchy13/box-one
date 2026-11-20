package ch.varani.boxone.training;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link Element}: the elements of each topic, the WPP left out as it is not marked in the roadbook.
 */
final class ElementTest {

  /** The kinds of waypoint marked in the roadbook: the seven of the FIA but the WPP, and the DZ and the FZ. */
  private static final int MARKED = 8;

  /**
   * Each topic holds every element of its type, the waypoints marked in the roadbook only, each of its topic.
   */
  @Test
  @DisplayName("Phase 7a: the elements of each topic, the WPP left out")
  void ofTest() {
    assertAll(
        () -> assertEquals(Symbol.values().length, Element.of(Topic.SYMBOLS).size()),
        () -> assertEquals(Abbreviation.values().length, Element.of(Topic.ABBREVIATIONS).size()),
        () -> assertEquals(MARKED, Element.of(Topic.WAYPOINTS).size()),
        () -> assertFalse(Element.of(Topic.WAYPOINTS).stream().map(element -> ((Element.OfWaypoint) element).kind())
                                 .anyMatch(WaypointKind.WPP::equals)),
        () -> assertEquals(List.of(new Element.OfDanger(Danger.LEVEL_1), new Element.OfDanger(Danger.LEVEL_2),
                                   new Element.OfDanger(Danger.LEVEL_3)), Element.of(Topic.DANGERS)),
        () -> assertEquals(Topic.SYMBOLS, new Element.OfSymbol(Symbol.BUMP).topic()),
        () -> assertEquals(Topic.ABBREVIATIONS, new Element.OfAbbreviation(Abbreviation.AT).topic()),
        () -> assertEquals(Topic.WAYPOINTS, new Element.OfWaypoint(WaypointKind.DZ).topic()),
        () -> assertEquals(Topic.DANGERS, new Element.OfDanger(Danger.LEVEL_2).topic()));
  }

  /**
   * A WPP has no mark in the roadbook, and a missing value is a programming error.
   */
  @Test
  @DisplayName("Phase 7a: a WPP, not marked in the roadbook, or a missing value is a programming error")
  void invalidTest() {
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, () -> new Element.OfWaypoint(WaypointKind.WPP)),
        () -> assertThrows(NullPointerException.class, () -> new Element.OfWaypoint(null)),
        () -> assertThrows(NullPointerException.class, () -> new Element.OfSymbol(null)),
        () -> assertThrows(NullPointerException.class, () -> new Element.OfAbbreviation(null)),
        () -> assertThrows(NullPointerException.class, () -> new Element.OfDanger(null)));
  }

}
