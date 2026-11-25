package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.Box;
import ch.varani.boxone.core.BoxSymbol;
import ch.varani.boxone.core.BranchKind;
import ch.varani.boxone.core.DiagramPoint;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.JunctionShape;
import ch.varani.boxone.core.Notes;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.SymbolPlace;
import ch.varani.boxone.core.Tulip;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.training.Penalty;
import ch.varani.boxone.training.Ride;
import ch.varani.boxone.training.Rider;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RideView}: the setup of the simulated run, the route prepared in the background, the run ridden with
 * the keys, its trip, its errors, the run lost, the controls, a stretch skipped and the finish.
 */
@ExtendWith(ApplicationExtension.class)
final class RideViewTest {

  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The width of the window of the tests, in pixels. */
  private static final double WIDTH = 1280;
  /** The height of the window of the tests, in pixels. */
  private static final double HEIGHT = 720;
  /** The step of time of the rides of the tests, in seconds. */
  private static final double TICK = 0.1;
  /** The steps of a ride long enough to lose, or to finish, the routes of the tests. */
  private static final int LONG = 600;
  /** The steps of a ride past the first 500 m of a stretch, from a stop: about 570 m. */
  private static final int PAST_LINK = 200;
  /** The steps of a short ride. */
  private static final int SHORT = 20;
  /** The distance ahead along the road the pilot of a test aims at, in metres. */
  private static final double AHEAD_AIM = 6;
  /** The turn under which the pilot of a test goes straight on, in degrees. */
  private static final double AIM_ZONE = 2;
  /** The speed of the pilot of a test, in km/h. */
  private static final double CRUISE = 15;
  /** A full turn, in degrees. */
  private static final double FULL_TURN = 360;
  /** The time the animation is left running, in milliseconds. */
  private static final long ANIMATION = 500;
  /** The metres of a degree of latitude, on the sphere of the routing. */
  private static final double LATITUDE_METRES = 6_371_000 * Math.toRadians(1);
  /** The centre of the routes of the tests: the station of Yverdon-les-Bains. */
  private static final GeoPoint CENTRE = new GeoPoint(46.7813, 6.6410);
  /** The length of the legs of the routes of the tests, in metres. */
  private static final double LEG = 200;
  /** The length of the long road of the tests, in metres: its stretch skipped. */
  private static final double LONG_ROAD = 2500;
  /** The radius of the ring of the roundabout of the tests, in metres. */
  private static final double RADIUS = 15;
  /** The steps of the ring, in degrees. */
  private static final int RING_STEP = 10;
  /** The speed limit of the zone of the tests, in km/h. */
  private static final int LIMIT = 50;
  /** A quarter of a turn, in degrees. */
  private static final int QUARTER = 90;
  /** A junction of three roads: left, straight on, right. */
  private static final List<Integer> THREE = List.of(-QUARTER, 0, QUARTER);
  /** The south-west corner of a large area of the map. */
  private static final GeoPoint SOUTH_WEST = new GeoPoint(46.70, 6.55);
  /** The north-east corner of the large area. */
  private static final GeoPoint NORTH_EAST = new GeoPoint(46.80, 6.75);
  /** The north-east corner of a tiny area: no route fits in it. */
  private static final GeoPoint TINY = new GeoPoint(46.7009, 6.5513);

  /**
   * The view shown in a window.
   *
   * @param view the view.
   * @param queue the thread of the long work, run by the tests.
   * @param backs the number of times the view went back to the welcome page.
   */
  private record Shown(RideView view, QueueExecutor queue, AtomicInteger backs) {
  }

  /**
   * Gives a point in metres east and north of the centre of the tests.
   *
   * @param east the metres east.
   * @param north the metres north.
   *
   * @return the point.
   */
  private static GeoPoint at(final double east, final double north) {
    return new GeoPoint(CENTRE.latitude() + north / LATITUDE_METRES,
                        CENTRE.longitude() + east / (LATITUDE_METRES * Math.cos(Math.toRadians(CENTRE.latitude()))));
  }

  /**
   * Gives a box of the tests.
   *
   * @param number its number.
   * @param total its total distance, in metres.
   * @param shape the shape of its tulip of three branches, its exit at the left, a tarmac road, entered by a track,
   *              the branch straight on a track; {@code null} for none.
   *
   * @return the box.
   */
  private static Box box(final int number, final double total, final JunctionShape shape) {
    return new Box(number, CENTRE, total, 0, OptionalInt.empty(), Optional.ofNullable(shape).map(
        kind -> new Tulip(kind, BranchKind.TRACK, THREE,
                          THREE.stream().map(angle -> angle == 0 ? BranchKind.TRACK : BranchKind.TARMAC).toList(), 0)));
  }

  /**
   * Gives the roadbook of the junctions: east to a crossing whose exit is at the left, north to the south of a
   * roundabout whose exit is at the right, around it counterclockwise to its east, then east.
   *
   * @return the roadbook.
   */
  private static Roadbook junctions() {
    final List<GeoPoint> points = new ArrayList<>(List.of(at(0, 0), at(LEG, 0), at(LEG, LEG)));
    for (int angle = -QUARTER; angle <= 0; angle += RING_STEP) {
      final double radians = Math.toRadians(angle);
      points.add(at(LEG + RADIUS * Math.cos(radians), LEG + RADIUS + RADIUS * Math.sin(radians)));
    }
    points.add(at(2 * LEG, LEG + RADIUS));
    return new Roadbook(new Route(points, List.of(1, 2), 0, Duration.ZERO),
                        List.of(box(1, 0, null), box(2, LEG, JunctionShape.CROSSING),
                                new Box(2 + 1, CENTRE, 2 * LEG, 0, OptionalInt.empty(), Optional.of(
                                    new Tulip(JunctionShape.COUNTERCLOCKWISE_ROUNDABOUT, BranchKind.TARMAC, THREE,
                                              List.of(BranchKind.TARMAC, BranchKind.TARMAC, BranchKind.TARMAC),
                                              2))),
                                box(2 + 2, 2 * LEG + LEG, null)));
  }

  /**
   * Gives the roadbook of a long straight road, a stretch of 2 km between two boxes.
   *
   * @return the roadbook.
   */
  private static Roadbook straight() {
    return new Roadbook(new Route(List.of(at(0, 0), at(LONG_ROAD, 0)), List.of(), 0, Duration.ZERO),
                        List.of(box(1, 0, null), box(2, LONG_ROAD - 2 * LEG, null), box(2 + 1, LONG_ROAD, null)));
  }

  /**
   * Gives the roadbook of the scenery: a straight road, a house, a castle and a tree beside the road at the second box,
   * a bump on its exit, a hole on the entry of the third box.
   *
   * @return the roadbook.
   */
  private static Roadbook scenery() {
    final double length = 2 * LEG + LEG;
    final Notes beside = Notes.NONE.withSymbols(List.of(BoxSymbol.free(Symbol.BUILDINGS, new DiagramPoint(1, 0)),
                                                        BoxSymbol.free(Symbol.FORT, new DiagramPoint(-1, -1)),
                                                        BoxSymbol.free(Symbol.TREE, new DiagramPoint(-1, 0)),
                                                        new BoxSymbol(Symbol.BUMP, SymbolPlace.EXIT)));
    final Notes hole = Notes.NONE.withSymbols(List.of(new BoxSymbol(Symbol.DIP_HOLE, SymbolPlace.ENTRY)));
    return new Roadbook(new Route(List.of(at(0, 0), at(length, 0)), List.of(), 0, Duration.ZERO),
                        List.of(box(1, 0, null),
                                new Box(2, CENTRE, LEG / 2, 0, OptionalInt.empty(), Optional.empty(),
                                        HeadingDisplay.RULE, beside, Optional.empty()),
                                new Box(2 + 1, CENTRE, length, 0, OptionalInt.empty(), Optional.empty(),
                                        HeadingDisplay.RULE, hole, Optional.empty())));
  }

  /**
   * Gives the roadbook of the waypoints: a straight road, a WPM at the second box, a WPP not marked at the third, a
   * speed zone at 50 km/h from the fourth box to the fifth.
   *
   * @return the roadbook.
   */
  private static Roadbook waypoints() {
    final double length = (2 + 2 + 2) * LEG;
    return new Roadbook(new Route(List.of(at(0, 0), at(length, 0)), List.of(), 0, Duration.ZERO),
                        List.of(box(1, 0, null), waypoint(2, LEG, Waypoint.of(WaypointKind.WPM)),
                                waypoint(2 + 1, 2 * LEG, Waypoint.of(WaypointKind.WPP)),
                                waypoint(2 + 2, (2 + 1) * LEG, Waypoint.of(WaypointKind.DZ, OptionalInt.of(LIMIT))),
                                waypoint(2 + 2 + 1, length - LEG, Waypoint.of(WaypointKind.FZ)),
                                box(2 + 2 + 2, length, null)));
  }

  /**
   * Gives a box with a waypoint, without tulip.
   *
   * @param number its number.
   * @param total its total distance, in metres.
   * @param waypoint its waypoint.
   *
   * @return the box.
   */
  private static Box waypoint(final int number, final double total, final Waypoint waypoint) {
    return new Box(number, CENTRE, total, 0, OptionalInt.empty(), Optional.empty(), HeadingDisplay.RULE, Notes.NONE,
                   Optional.of(waypoint));
  }

  /**
   * Shows the view in a window.
   *
   * @param robot the TestFX robot.
   * @param sources where the routes come from.
   *
   * @return the view shown, on its setup page.
   */
  private static Shown show(final FxRobot robot, final RideView.Sources sources) {
    final AtomicReference<Shown> shown = new AtomicReference<>();
    robot.interact(() -> {
      final QueueExecutor queue = new QueueExecutor();
      final AtomicInteger backs = new AtomicInteger();
      final RideView view = new RideView(new QuizText(TEXTS, Languages.FRENCH), Catalogue.fia(), queue, sources,
                                         () -> 1L, backs::incrementAndGet);
      final Stage stage = new Stage();
      stage.setScene(new Scene(view.getNode(), WIDTH, HEIGHT));
      stage.show();
      shown.set(new Shown(view, queue, backs));
    });
    return shown.get();
  }

  /**
   * Gives the sources of a roadbook open, without map.
   *
   * @param roadbook the roadbook.
   *
   * @return the sources.
   */
  private static RideView.Sources open(final Roadbook roadbook) {
    return new RideView.Sources(() -> Optional.of(roadbook), () -> null, Optional::empty);
  }

  /**
   * Gives the sources of a map, without roadbook open.
   *
   * @param router the router of the map.
   * @param northEast the north-east corner of the area seen, from {@link #SOUTH_WEST}; {@code null} for no area.
   *
   * @return the sources.
   */
  private static RideView.Sources map(final Router router, final GeoPoint northEast) {
    return new RideView.Sources(Optional::empty, () -> router,
                                () -> Optional.ofNullable(northEast).map(corner -> List.of(SOUTH_WEST, corner)));
  }

  /**
   * Starts the run of the route chosen, prepared at once.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown, on its setup page.
   */
  private static void start(final FxRobot robot, final Shown shown) {
    fire(robot, shown, RideView.START_ID);
    robot.interact(shown.queue()::runAll);
  }

  /**
   * Finds a node of the view by its identifier, once the page shown is laid out.
   *
   * @param <T> the type of the node.
   * @param shown the view shown.
   * @param id the identifier.
   *
   * @return the node, or {@code null} if the page shown has none.
   */
  @SuppressWarnings("unchecked")
  private static <T extends Node> T node(final Shown shown, final String id) {
    WaitForAsyncUtils.waitForFxEvents();
    return (T) shown.view().getNode().lookup("#" + id);
  }

  /**
   * Fires a button of the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param id the identifier of the button.
   */
  private static void fire(final FxRobot robot, final Shown shown, final String id) {
    final Button button = node(shown, id);
    robot.interact(button::fire);
  }

  /**
   * Presses or releases a key in the view.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param type pressed or released.
   * @param code the key.
   */
  private static void key(final FxRobot robot, final Shown shown, final EventType<KeyEvent> type,
                          final KeyCode code) {
    robot.interact(() -> shown.view().getNode().fireEvent(new KeyEvent(type, "", "", code, false, false, false,
                                                                       false)));
  }

  /**
   * Rides for some steps of {@value #TICK} s, in one go: the animation does not move the motorbike meanwhile.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param steps the steps.
   */
  private static void ride(final FxRobot robot, final Shown shown, final int steps) {
    robot.interact(() -> {
      for (int i = 0; i < steps; i++) {
        shown.view().advance(TICK);
      }
    });
  }

  /**
   * Rides step by step until a penalty is given, or at most {@value #LONG} steps.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   */
  private static void untilPenalty(final FxRobot robot, final Shown shown) {
    final String before = text(shown, RideView.PENALTIES_ID);
    until(robot, shown, RideView.PENALTIES_ID, penalties -> !before.equals(penalties));
  }

  /**
   * Rides step by step until the text of a label of the view meets a condition, or at most {@value #LONG} steps.
   *
   * @param robot the TestFX robot.
   * @param shown the view shown.
   * @param id the identifier of the label.
   * @param condition the condition.
   */
  private static void until(final FxRobot robot, final Shown shown, final String id,
                            final Predicate<String> condition) {
    for (int i = 0; i < LONG && !condition.test(text(shown, id)); i++) {
      ride(robot, shown, 1);
    }
  }

  /**
   * Gives the text of a label of the view.
   *
   * @param shown the view shown.
   * @param id the identifier of the label.
   *
   * @return its text.
   */
  private static String text(final Shown shown, final String id) {
    return ((Label) node(shown, id)).getText();
  }

  /**
   * Reads a value of the instruments drawn in the scene, from the accessible text of the road.
   *
   * @param shown the run shown.
   * @param index 0 for the speed, 1 for the trip.
   *
   * @return its text.
   */
  private static String dash(final Shown shown, final int index) {
    return node(shown, RideView.CANVAS_ID).getAccessibleText().split(RideView.DASH_JOIN)[index];
  }

  /**
   * Without roadbook nor map, both routes are disabled, the run cannot start, a caption says what to do, a key does
   * nothing; once the map is there, its route is enabled; Back goes to the welcome page.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: no route without roadbook nor map, the route at random once the map is loaded")
  void setupTest(final FxRobot robot) {
    final AtomicReference<Router> router = new AtomicReference<>();
    final Shown shown = show(robot, new RideView.Sources(Optional::empty, router::get, Optional::empty));
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    final boolean nothing = node(shown, RideView.START_ID).isDisabled();
    final String caption = text(shown, RideView.PROBLEM_ID);
    router.set(new GridRouter(GridRouter.Kind.CROSSINGS));
    robot.interact(shown.view()::refresh);
    final boolean random = ((RadioButton) node(shown, RideView.RANDOM_ID)).isSelected();
    fire(robot, shown, TrainingView.BACK_ID);
    assertAll(
        () -> assertTrue(nothing),
        () -> assertEquals(TEXTS.getString(RideView.NOTHING), caption),
        () -> assertTrue(random),
        () -> assertFalse(node(shown, RideView.START_ID).isDisabled()),
        () -> assertEquals(1, shown.backs().get()));
  }

  /**
   * The roadbook open is ridden with the keys: stopped at the start, the throttle moves it, the brake stops it; the
   * trip is corrected by + and Q, never below 0; Space and the button pause it and go on, the motorbike standing still
   * meanwhile; H
   * shows the controls and pauses, again hides them; the refresh of a run changes nothing; the animation moves it.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: the roadbook ridden with the keys, the trip corrected, paused, the controls shown")
  void keysTest(final FxRobot robot) {
    final Shown shown = show(robot, open(junctions()));
    start(robot, shown);
    final String stopped = dash(shown, 0);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.W);
    ride(robot, shown, SHORT);
    key(robot, shown, KeyEvent.KEY_RELEASED, KeyCode.W);
    final String moving = dash(shown, 0);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.A);
    ride(robot, shown, 1);
    key(robot, shown, KeyEvent.KEY_RELEASED, KeyCode.A);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.RIGHT);
    ride(robot, shown, 1);
    key(robot, shown, KeyEvent.KEY_RELEASED, KeyCode.RIGHT);
    final String trip = dash(shown, 1);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.PLUS);
    final String corrected = dash(shown, 1);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.Q);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.SPACE);
    ride(robot, shown, SHORT);
    final String paused = dash(shown, 1);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.SPACE);
    fire(robot, shown, RideView.PAUSE_ID);
    final String pauseButton = ((Button) node(shown, RideView.PAUSE_ID)).getText();
    fire(robot, shown, RideView.PAUSE_ID);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.H);
    final boolean help = node(shown, RideView.HELP_ID).isVisible();
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.F1);
    final boolean hidden = !node(shown, RideView.HELP_ID).isVisible();
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.S);
    ride(robot, shown, SHORT);
    key(robot, shown, KeyEvent.KEY_RELEASED, KeyCode.S);
    final String braked = dash(shown, 0);
    robot.interact(shown.view()::refresh);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    WaitForAsyncUtils.sleep(ANIMATION, TimeUnit.MILLISECONDS);
    final String animated = dash(shown, 0);
    fire(robot, shown, RideView.STOP_ID);
    assertAll(
        () -> assertEquals("0 km/h", stopped),
        () -> assertNotEquals(stopped, moving),
        () -> assertNotEquals(trip, corrected),
        () -> assertEquals(trip, paused),
        () -> assertEquals(TEXTS.getString(RideView.RESUME), pauseButton),
        () -> assertTrue(help),
        () -> assertTrue(hidden),
        () -> assertEquals("0 km/h", braked),
        () -> assertNotEquals(braked, animated),
        () -> assertNotNull(node(shown, RideView.START_ID)));
  }

  /**
   * Going straight on at the crossing whose exit is at the left is a flat penalty, flashed on red and counted; the
   * motorbike put back before the junction, stopped, the word shown while it stands, the throttle held through; the
   * throttle released and opened again, it goes and the word goes.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: a tulip not followed a flat penalty, the motorbike put back and stopped, the word shown")
  void penaltiesTest(final FxRobot robot) {
    final Shown shown = show(robot, open(junctions()));
    start(robot, shown);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    untilPenalty(robot, shown);
    final String flash = text(shown, RideView.FLASH_ID);
    final boolean red = !node(shown, RideView.FLASH_ID).getStyleClass().contains(RideView.INFO_STYLE);
    final String first = text(shown, RideView.PENALTIES_ID);
    ride(robot, shown, SHORT);
    final boolean shownWhileStopped = node(shown, RideView.FLASH_ID).isVisible();
    final String stopped = dash(shown, 0);
    key(robot, shown, KeyEvent.KEY_RELEASED, KeyCode.UP);
    ride(robot, shown, 1);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    ride(robot, shown, SHORT);
    final boolean hidden = !node(shown, RideView.FLASH_ID).isVisible();
    final String going = dash(shown, 0);
    fire(robot, shown, RideView.STOP_ID);
    assertAll(
        () -> assertEquals("TULIPE MAL SUIVIE +15 min", flash),
        () -> assertTrue(red),
        () -> assertEquals("Pénalités : 15 min", first),
        () -> assertTrue(shownWhileStopped),
        () -> assertEquals("0 km/h", stopped),
        () -> assertTrue(hidden),
        () -> assertNotEquals("0 km/h", going));
  }


  /**
   * The roadbook of the junctions ridden to the finish by a pilot following the road, without map, the scene drawn at
   * each step: the crossing and the roundabout taken right, no penalty, the branches, the ring and the island of the
   * roundabout drawn from the tulips.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: the junctions ridden right to the finish without map, no penalty")
  void followedTest(final FxRobot robot) {
    final Rider rider = new Rider(Ride.of(junctions()));
    final RidePalette palette = RidePalette.load();
    robot.interact(() -> {
      final Canvas canvas = new Canvas(WIDTH, HEIGHT);
      for (int i = 0; i < LONG * 2 * 2 && !rider.isOver(); i++) {
        final double east = rider.ride().east(rider.progress() + AHEAD_AIM) - rider.east();
        final double north = rider.ride().north(rider.progress() + AHEAD_AIM) - rider.north();
        final double turn = ((Math.toDegrees(Math.atan2(east, north)) - rider.heading()) % FULL_TURN + FULL_TURN
                             + FULL_TURN / 2) % FULL_TURN - FULL_TURN / 2;
        rider.step(TICK, new Rider.Controls(!rider.isHalted() && rider.speed() < CRUISE, false, turn < -AIM_ZONE,
                                            turn > AIM_ZONE));
        RideRenderer.draw(canvas.getGraphicsContext2D(), WIDTH, HEIGHT, rider, 0,
                          new RideRenderer.Dash("km/h", "km", ','), palette);
      }
    });
    assertAll(
        () -> assertTrue(rider.isOver()),
        () -> assertEquals(List.of(), rider.penalties()));
  }

  /**
   * On a long straight road, the middle of the stretch between two boxes is skipped and flashed on blue, an
   * information; the strip follows the next box; at the finish, the length, the boxes and no error; a new run goes back
   * to the setup.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: a long stretch skipped and flashed, to the finish without error")
  void finishTest(final FxRobot robot) {
    final Shown shown = show(robot, open(straight()));
    start(robot, shown);
    final ListView<?> strip = node(shown, RideView.STRIP_ID);
    final int first = strip.getSelectionModel().getSelectedIndex();
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    ride(robot, shown, PAST_LINK);
    final String flash = text(shown, RideView.FLASH_ID);
    final boolean blue = node(shown, RideView.FLASH_ID).getStyleClass().contains(RideView.INFO_STYLE);
    ride(robot, shown, LONG);
    final String result = text(shown, RideView.RESULT_ID);
    ride(robot, shown, 1);
    fire(robot, shown, RideView.AGAIN_ID);
    assertAll(
        () -> assertEquals(1, first),
        () -> assertTrue(flash.startsWith("Liaison accélérée : ")),
        () -> assertTrue(blue),
        () -> assertEquals("2,50 km parcourus, 3 cases, pénalités : 0 min.", result),
        () -> assertNotNull(node(shown, RideView.START_ID)));
  }

  /**
   * The elements noted in the roadbook open stand in the scene: a house, a castle and a tree beside the road, a bump
   * and a hole on it; passed at full speed, each is a fall, flashed and counted, the run going on to the finish.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: the elements of the notes drawn, an obstacle passed too fast a fall, a penalty")
  void sceneryTest(final FxRobot robot) {
    final Shown shown = show(robot, open(scenery()));
    start(robot, shown);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    untilPenalty(robot, shown);
    final String flash = text(shown, RideView.FLASH_ID);
    final String penalties = text(shown, RideView.PENALTIES_ID);
    ride(robot, shown, LONG);
    assertAll(
        () -> assertEquals("CHUTE +5 min", flash),
        () -> assertEquals("Pénalités : 5 min", penalties),
        () -> assertEquals("0,60 km parcourus, 3 cases, pénalités : 10 min.", text(shown, RideView.RESULT_ID)));
  }

  /**
   * The waypoints of the roadbook open are shown as by the NAV-GPS and flashed once validated; with the limiter on
   * (L), the speed zone is ridden under its limit; off, an excess is a penalty, flashed; the finish lists it.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: the waypoints shown and validated, the speed zone ridden with the limiter, an excess")
  void waypointsTest(final FxRobot robot) {
    final Shown shown = show(robot, open(waypoints()));
    start(robot, shown);
    final String gps = text(shown, RideView.GPS_ID);
    final String off = text(shown, RideView.ZONE_ID);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.L);
    final String on = text(shown, RideView.ZONE_ID);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    until(robot, shown, RideView.FLASH_ID, flash -> !flash.isEmpty());
    final String validated = text(shown, RideView.FLASH_ID);
    until(robot, shown, RideView.ZONE_ID, zone -> zone.startsWith("Zone"));
    ride(robot, shown, SHORT);
    final String zone = text(shown, RideView.ZONE_ID);
    final String limited = text(shown, RideView.PENALTIES_ID);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.L);
    untilPenalty(robot, shown);
    final String excess = text(shown, RideView.FLASH_ID);
    ride(robot, shown, LONG);
    assertAll(
        () -> assertEquals("WPM 1 : 200 m", gps),
        () -> assertEquals("limiteur désactivé", off),
        () -> assertEquals("limiteur activé", on),
        () -> assertEquals("WPM VALIDÉ", validated),
        () -> assertEquals("Zone 50 km/h · limiteur activé", zone),
        () -> assertEquals("Pénalités : 0 min", limited),
        () -> assertTrue(excess.startsWith("EXCÈS DE VITESSE +"), excess),
        () -> assertTrue(text(shown, RideView.RESULT_ID).endsWith(" min.")));
  }

  /**
   * The words and the lines of the penalties: each reason, the waypoint missed, the times in minutes, in seconds or in
   * both, the impulses of a zone summed.
   */
  @Test
  @DisplayName("Phase 7c: the penalties written, flashed and listed by box")
  void penaltyTextsTest() {
    final List<Penalty> given = List.of(Penalty.wrongTulip(2), Penalty.missedWaypoint(2 + 1, WaypointKind.WPS),
                                        Penalty.speeding(2 + 2, 1), Penalty.speeding(2 + 2, 2 * 2 * 2),
                                        Penalty.fallOnObstacle(2 + 2 + 1));
    assertAll(
        () -> assertEquals("WPS MANQUÉ +15 min", RideView.word(given.get(1), TEXTS, Locale.forLanguageTag("fr-CH"))),
        () -> assertEquals("EXCÈS DE VITESSE +10 s", RideView.word(given.get(2), TEXTS,
                                                                    Locale.forLanguageTag("fr-CH"))),
        () -> assertEquals(List.of("Case 2 : tulipe mal suivie, 15 min", "Case 3 : WPS manqué, 15 min",
                                   "Case 4 : excès de vitesse, 1 min 10 s", "Case 5 : chute, 5 min"),
                           RideView.lines(given, TEXTS, Locale.forLanguageTag("fr-CH"))),
        () -> assertEquals("0 min", RideView.time(0, TEXTS, Locale.forLanguageTag("fr-CH"))));
  }

  /**
   * A route drawn at random in the area of the map seen is ridden, the roads of the map drawn around its junctions,
   * the roadbook open not chosen without one.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: a route drawn at random ridden, the roads of the map around its junctions")
  void randomTest(final FxRobot robot) {
    final Shown shown = show(robot, map(new GridRouter(GridRouter.Kind.CROSSINGS), NORTH_EAST));
    final boolean chosen = ((RadioButton) node(shown, RideView.RANDOM_ID)).isSelected();
    final boolean noOpen = ((RadioButton) node(shown, RideView.OPEN_ID)).isDisabled();
    start(robot, shown);
    key(robot, shown, KeyEvent.KEY_PRESSED, KeyCode.UP);
    ride(robot, shown, LONG);
    assertAll(
        () -> assertTrue(chosen),
        () -> assertTrue(noOpen),
        () -> assertNotNull(node(shown, RideView.CANVAS_ID)));
  }

  /**
   * No route in a tiny area, nor without the area of the map, says so; a router failing says that the route cannot be
   * prepared; a preparation cancelled goes back to the setup.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 7c: no route found, a route failing, a preparation cancelled, said on the setup page")
  void problemsTest(final FxRobot robot) {
    final Shown tiny = show(robot, map(new GridRouter(GridRouter.Kind.CROSSINGS), TINY));
    start(robot, tiny);
    final String none = text(tiny, RideView.PROBLEM_ID);
    final Shown hidden = show(robot, map(new GridRouter(GridRouter.Kind.CROSSINGS), null));
    fire(robot, hidden, RideView.START_ID);
    final String noArea = text(hidden, RideView.PROBLEM_ID);
    final Shown failing = show(robot, map(new GridRouter(GridRouter.Kind.FAILING), NORTH_EAST));
    start(robot, failing);
    final String failed = text(failing, RideView.PROBLEM_ID);
    fire(robot, failing, RideView.START_ID);
    fire(robot, failing, RideView.CANCEL_ID);
    assertAll(
        () -> assertEquals(TEXTS.getString(RideView.NONE), none),
        () -> assertEquals(TEXTS.getString(RideView.NONE), noArea),
        () -> assertEquals(TEXTS.getString(RideView.FAILED), failed),
        () -> assertFalse(node(failing, RideView.START_ID).isDisabled()));
  }

}
