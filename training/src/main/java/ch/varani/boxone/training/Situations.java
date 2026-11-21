package ch.varani.boxone.training;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.Danger;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The situations of a roadbook, written by hand for Box One (development plan, phase 7; decisions of the maintainer,
 * 2026-11-20: the dangers, the waypoints, the speed zones, the distances and the notes; extracts of two or three
 * boxes). Their behaviours follow the regulations of the FIM (2026 FIM World Rally-Raid Championship, Sporting
 * Regulations; asked by the maintainer, 2026-11-21): the itinerary and the waypoints (art. 11.1, 11.2), the dangers
 * (art. 11.4), the speed zones (art. 16) and the symbols of the road book; the boxes close to each other, which the
 * FIM does not rule on, after Box One (the rule of the FIA its strip draws). Their texts — the question, the
 * behaviours, the explanation — are those of the user interface, named after each situation.
 *
 * <p>The boxes are fictitious, all at one public place (the station of Yverdon-les-Bains): an extract is drawn, never
 * shown on the map.</p>
 *
 * <p>Utility class, its situations immutable, safe for concurrent use.</p>
 */
public final class Situations {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The place of every box: a public place, never shown. */
  private static final GeoPoint PLACE = new GeoPoint(46.7813, 6.6410);
  /** The angle of a branch to the left, square, in degrees from straight ahead. */
  private static final int LEFT = -90;
  /** The angle of a branch to the right, square, in degrees from straight ahead. */
  private static final int RIGHT = 90;
  /** The angle of a fork to either side, in degrees from straight ahead. */
  private static final int FORK = 30;
  /** A crossing of four roads, straight on. */
  private static final Tulip STRAIGHT = crossing(1);
  /** A crossing of four roads, turning right. */
  private static final Tulip TURN_RIGHT = crossing(2);
  /** A crossing of four roads, turning left. */
  private static final Tulip TURN_LEFT = crossing(0);
  /** A box on a road without junction, straight on. */
  private static final Tulip ROAD = new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(0),
                                               List.of(BranchKind.TARMAC), 0);
  /** A box on a track without junction, straight on. */
  private static final Tulip TRACK = new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, List.of(0),
                                                List.of(BranchKind.TRACK), 0);
  /** The speed limit of a village, in km/h (chosen for Box One: the FIM leaves it to the road book, art. 16). */
  private static final int VILLAGE_SPEED = 50;
  /** A lower speed limit of a zone, in km/h (chosen for Box One: the FIM leaves it to the road book, art. 16). */
  private static final int SLOW_SPEED = 30;

  /** Every situation, by theme then name. */
  private static final List<Situation> ALL = List.of(
      // Dangers.
      new Situation("dangerThree", Theme.DANGERS, List.of(
          box(14, 21_300, 1_250, STRAIGHT, Notes.NONE, Optional.empty()),
          box(15, 22_150, 850, TRACK, danger(Danger.LEVEL_3, Symbol.DITCH),
              Optional.of(Waypoint.of(WaypointKind.WPS)))), 1),
      new Situation("dangerOne", Theme.DANGERS, List.of(
          box(7, 9_400, 600, TURN_RIGHT, Notes.NONE, Optional.empty()),
          box(8, 10_550, 1_150, TRACK, danger(Danger.LEVEL_1, Symbol.BUMP), Optional.empty())), 1),
      new Situation("dangerTwo", Theme.DANGERS, List.of(
          box(3, 2_100, 700, TURN_LEFT, Notes.NONE, Optional.empty()),
          box(4, 3_000, 900, TRACK, danger(Danger.LEVEL_2, Symbol.DIP_HOLE), Optional.empty())), 1),
      new Situation("dangerAhead", Theme.DANGERS, List.of(
          box(21, 30_100, 1_400, TURN_RIGHT, Notes.NONE, Optional.empty()),
          box(22, 30_350, 250, TRACK, danger(Danger.LEVEL_3, Symbol.STEP_DOWN),
              Optional.of(Waypoint.of(WaypointKind.WPS)))), 0),
      new Situation("dangerSummit", Theme.DANGERS, List.of(
          box(11, 15_800, 2_100, STRAIGHT, Notes.NONE, Optional.empty()),
          box(12, 16_450, 650, TRACK, danger(Danger.LEVEL_2, Symbol.SUMMIT), Optional.empty())), 1),
      // Waypoints.
      new Situation("maskedWaypoint", Theme.WAYPOINTS, List.of(
          box(10, 12_300, 900, STRAIGHT, Notes.NONE, Optional.empty()),
          box(11, 13_700, 1_400, TURN_LEFT, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.WPM)))), 1),
      new Situation("controlWaypoint", Theme.WAYPOINTS, List.of(
          box(5, 4_200, 800, TURN_RIGHT, Notes.NONE, Optional.empty()),
          box(6, 5_350, 1_150, TURN_LEFT, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.WPC)))), 1),
      new Situation("visibleWaypoint", Theme.WAYPOINTS, List.of(
          box(1, 0, 0, ROAD, Notes.NONE, Optional.empty()),
          box(2, 1_800, 1_800, TURN_RIGHT, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.WPV)))), 1),
      new Situation("navigationWaypoint", Theme.WAYPOINTS, List.of(
          box(30, 48_600, 3_200, TRACK, Notes.NONE, Optional.empty()),
          box(31, 50_900, 2_300, TRACK, notes(Abbreviation.DUNES_LEVEL_2),
              Optional.of(Waypoint.of(WaypointKind.WPN)))), 1),
      new Situation("eclipseWaypoint", Theme.WAYPOINTS, List.of(
          box(17, 24_000, 1_100, TURN_LEFT, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.WPM))),
          box(18, 27_300, 3_300, TURN_RIGHT, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.WPE)))), 1),
      new Situation("missedWaypoint", Theme.WAYPOINTS, List.of(
          box(6, 7_200, 1_300, TURN_RIGHT, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.WPM))),
          box(7, 8_900, 1_700, STRAIGHT, Notes.NONE, Optional.empty()),
          box(8, 9_600, 700, TURN_LEFT, Notes.NONE, Optional.empty())), 1),
      // Speed zones.
      new Situation("zoneStart", Theme.SPEED_ZONES, List.of(
          box(3, 3_100, 1_200, TURN_RIGHT, Notes.NONE, Optional.empty()),
          box(4, 4_500, 1_400, ROAD, Notes.NONE, Optional.of(zone(VILLAGE_SPEED))),
          box(5, 5_100, 600, TURN_LEFT, Notes.NONE, Optional.empty())), 1),
      new Situation("zoneInside", Theme.SPEED_ZONES, List.of(
          box(4, 4_500, 1_400, ROAD, Notes.NONE, Optional.of(zone(VILLAGE_SPEED))),
          box(5, 5_100, 600, TURN_LEFT, Notes.NONE, Optional.empty()),
          box(6, 5_900, 800, ROAD, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.FZ)))), 1),
      new Situation("zoneFinish", Theme.SPEED_ZONES, List.of(
          box(9, 11_200, 500, TURN_RIGHT, Notes.NONE, Optional.of(zone(VILLAGE_SPEED))),
          box(10, 12_000, 800, ROAD, Notes.NONE, Optional.of(Waypoint.of(WaypointKind.FZ)))), 1),
      new Situation("zoneSigns", Theme.SPEED_ZONES, List.of(
          box(1, 0, 0, ROAD, Notes.NONE, Optional.empty()),
          box(2, 900, 900, ROAD, Notes.NONE, Optional.of(zone(VILLAGE_SPEED)))), 1),
      new Situation("zoneAcceleration", Theme.SPEED_ZONES, List.of(
          box(12, 18_300, 1_100, TURN_LEFT, Notes.NONE, Optional.of(zone(SLOW_SPEED))),
          box(13, 18_900, 600, ROAD, Notes.NONE, Optional.of(zone(VILLAGE_SPEED)))), 1),
      // Distances and notes.
      new Situation("closeBox", Theme.DISTANCES, List.of(
          box(5, 6_400, 1_500, TURN_RIGHT, Notes.NONE, Optional.empty()),
          box(6, 6_580, 180, TURN_LEFT, Notes.NONE, Optional.empty())), 0),
      new Situation("keepLeft", Theme.DISTANCES, List.of(
          box(8, 11_000, 900, STRAIGHT, Notes.NONE, Optional.empty()),
          box(9, 12_250, 1_250, new Tulip(JunctionShape.CROSSING, BranchKind.TRACK, List.of(-FORK, FORK),
                                          List.of(BranchKind.TRACK, BranchKind.TRACK), 0),
              notes(Abbreviation.KEEP_LEFT), Optional.empty())), 1),
      new Situation("offTrack", Theme.DISTANCES, List.of(
          box(19, 33_700, 2_600, TRACK, Notes.NONE, Optional.empty()),
          new Box(20, PLACE, 34_400, 700, OptionalInt.of(245), Optional.of(TRACK), HeadingDisplay.SHOWN,
                  notes(Abbreviation.OFF_TRACK), Optional.empty())), 1),
      new Situation("followRoad", Theme.DISTANCES, List.of(
          box(2, 1_300, 1_300, TURN_LEFT, notes(Abbreviation.FOLLOW_ROAD), Optional.empty()),
          box(3, 4_800, 3_500, TURN_RIGHT, Notes.NONE, Optional.empty())), 0));

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private Situations() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives every situation.
   *
   * @return the situations, by theme then name; unmodifiable.
   */
  public static List<Situation> all() {
    return ALL;
  }

  /**
   * Gives the situations of some themes.
   *
   * @param themes the themes.
   *
   * @return the situations of these themes, in the order of {@link #all()}; unmodifiable.
   */
  public static List<Situation> of(final Set<Theme> themes) {
    return ALL.stream().filter(situation -> themes.contains(situation.theme())).toList();
  }

  /**
   * Creates a crossing of four roads: the entry, a road to the left, straight ahead and to the right.
   *
   * @param exit the index of the exit among the three roads, from the left.
   *
   * @return the tulip, every road tarmac.
   */
  private static Tulip crossing(final int exit) {
    return new Tulip(JunctionShape.CROSSING, BranchKind.TARMAC, List.of(LEFT, 0, RIGHT),
                     Collections.nCopies(2 + 1, BranchKind.TARMAC), exit);
  }

  /**
   * Creates a box of an extract, its heading hidden: the extracts show a heading only where the situation asks it.
   *
   * @param number its number.
   * @param total its total distance, in metres.
   * @param partial its partial distance, in metres.
   * @param tulip its tulip.
   * @param notes its notes.
   * @param waypoint its waypoint, or empty.
   *
   * @return the box.
   */
  private static Box box(final int number, final double total, final double partial, final Tulip tulip,
                         final Notes notes, final Optional<Waypoint> waypoint) {
    return new Box(number, PLACE, total, partial, OptionalInt.of(0), Optional.of(tulip), HeadingDisplay.HIDDEN, notes,
                   waypoint);
  }

  /**
   * Creates the notes of a danger on the exit of a box.
   *
   * @param danger the level of the danger.
   * @param symbol the symbol of the road it applies to.
   *
   * @return the notes.
   */
  private static Notes danger(final Danger danger, final Symbol symbol) {
    return new Notes(Optional.of(danger), List.of(), "", List.of(new BoxSymbol(symbol, SymbolPlace.EXIT)));
  }

  /**
   * Creates the notes of an abbreviation.
   *
   * @param abbreviation the abbreviation.
   *
   * @return the notes.
   */
  private static Notes notes(final Abbreviation abbreviation) {
    return new Notes(Optional.empty(), List.of(abbreviation), "", List.of());
  }

  /**
   * Creates the waypoint of the start of a speed zone.
   *
   * @param speed its speed limit, in km/h.
   *
   * @return the DZ.
   */
  private static Waypoint zone(final int speed) {
    return Waypoint.of(WaypointKind.DZ, OptionalInt.of(speed));
  }

}
