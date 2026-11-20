package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.HeadingDisplay;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.export.OpenRallyReader;
import ch.varani.boxone.routing.Router;
import javafx.application.ColorScheme;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link MainController} for the editor of the roadbook through the map (development plan, phase 5): the
 * boxes and the waypoints added by the context menu of the map, corrected, removed and brought back, exported with the
 * changes. Taken out of {@code MainControllerTest}, which reached the length allowed (Checkstyle). The routing engine
 * and the thread of the long work are fakes; the work is run by the tests on the JavaFX Application Thread.
 */
@ExtendWith(ApplicationExtension.class)
final class MainControllerEditorTest {

  /** The waypoints placed by the context menu of the map in the test of the waypoints: two WPM and a DZ. */
  private static final int WAYPOINTS_PLACED = 3;
  /** The rank of the item "Add a box here" in the context menu of the map; "Add a waypoint here" follows it. */
  private static final int ADD_BOX_ITEM = 2;
  /** The latitude of the station of Yverdon-les-Bains. */
  private static final double STATION_LATITUDE = 46.7813;
  /** The longitude of the station of Yverdon-les-Bains. */
  private static final double STATION_LONGITUDE = 6.6410;
  /** The latitude of the castle of Yverdon-les-Bains. */
  private static final double CASTLE_LATITUDE = 46.7784;
  /** The longitude of the castle of Yverdon-les-Bains. */
  private static final double CASTLE_LONGITUDE = 6.6413;
  /** The texts of the tests, in French. */
  private static final ResourceBundle TEXTS = Languages.bundle(Languages.FRENCH);
  /** The boxes of the roadbook of a route with one turn: the start, the turn, the finish. */
  private static final int TURNING_BOXES = 3;
  /** The latitude halfway from the station to the latitude of the castle. */
  private static final double HALFWAY_LATITUDE = 46.7800;
  /** A longitude about 4 m east of the station: a right-click near the route going south from it. */
  private static final double NEAR_LONGITUDE = 6.641_05;
  /** A longitude about 76 m east of the station: a right-click far from the route going south from it. */
  private static final double FAR_LONGITUDE = 6.6420;
  /** A heading typed in the editor, in degrees. */
  private static final int EASTWARDS = 45;

  /** The data folder of the tests. */
  @TempDir
  private Path data;
  /** The thread of the long work, run by the tests. */
  private final QueueExecutor background = new QueueExecutor();

  /**
   * A window shown by the tests.
   *
   * @param stage the window.
   * @param controller its controller.
   */
  private record Shown(Stage stage, MainController controller) {
  }

  /**
   * Shows the main window, outside macOS, with a graph loaded from a router and the choice of the file of the
   * roadbook, in the light mode of the system.
   *
   * @param robot the TestFX robot.
   * @param router the router of the graph.
   * @param saver chooses the file of the roadbook, and of the plain GPX.
   *
   * @return the window.
   */
  private Shown show(final FxRobot robot, final Router router,
                     final Function<javafx.stage.Window, Optional<Path>> saver) {
    final Services services = new Services(data, background, folder -> Optional.of(router), (map, graph) -> router,
                                           owner -> Optional.empty(), saver, owner -> Optional.empty(), saver);
    final SystemColors colors = new SystemColors(new SimpleObjectProperty<>(ColorScheme.LIGHT),
                                                 new SimpleObjectProperty<>(Color.web("#007AFF")),
                                                 new SimpleObjectProperty<>(Color.WHITE),
                                                 new SimpleObjectProperty<>(Color.BLACK));
    final AtomicReference<Shown> window = new AtomicReference<>();
    robot.interact(() -> {
      final Stage stage = new Stage();
      final MainController controller = new MainController(stage, TEXTS, Languages.FRENCH, false, colors, () -> { },
                                                           services);
      controller.show();
      window.set(new Shown(stage, controller));
    });
    return window.get();
  }

  /**
   * Returns the view of the map of a window, once its page has loaded.
   *
   * @param window the window.
   *
   * @return the view of the map.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  private static WebView map(final Shown window) throws TimeoutException {
    final WebView view = (WebView) window.stage().getScene().lookup(".web-view");
    MapPages.waitLoaded(view);
    return view;
  }

  /**
   * Returns the text of a label of the window.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   * @param id the identifier of the label.
   *
   * @return its text.
   */
  private static String text(final FxRobot robot, final Shown window, final String id) {
    return robot.from(window.stage().getScene().getRoot()).lookup("#" + id).queryAs(Label.class).getText();
  }

  /**
   * Fires a menu item of the menu bar of a window.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   * @param id the identifier of the item.
   */
  private static void fire(final FxRobot robot, final Shown window, final String id) {
    final MenuBar bar = robot.from(window.stage().getScene().getRoot()).lookup(".menu-bar").queryAs(MenuBar.class);
    robot.interact(bar.getMenus().stream().flatMap(menu -> menu.getItems().stream())
                      .filter(candidate -> id.equals(candidate.getId())).findFirst().orElseThrow()::fire);
  }

  /**
   * Returns the boxes of the strip.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   *
   * @return the list of the boxes.
   */
  private static ListView<?> boxes(final FxRobot robot, final Shown window) {
    return robot.from(window.stage().getScene().getRoot()).lookup("#boxList").queryAs(ListView.class);
  }

  /**
   * A box is added where the user right-clicks near the route, and selected; a click far from the route says so; the
   * box selected is corrected in the editor under the strip (its heading, shown always), and removed; Edit › Undo
   * brings the box back with its corrections; the roadbook exported holds the changes, which are kept when the finish
   * moves and the route still passes there. A box corrected is selected and the strip scrolled to it (found by the
   * maintainer on 2026-11-15: a box added out of sight was selected, not seen).
   *
   * @param robot the TestFX robot.
   *
   * @throws Exception if the page of the map does not load in time, or the roadbook exported cannot be read.
   */
  @Test
  @DisplayName("Phase 5: a box is added on the route, corrected, removed and brought back, exported with the changes")
  void editorTest(final FxRobot robot) throws Exception {
    final Path file = data.resolve("edited.gpx");
    final Shown window = show(robot, FakeRouter.turning(), owner -> Optional.of(file));
    robot.interact(background::runAll);
    final WebView view = map(window);
    final ListView<?> boxes = boxes(robot, window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    robot.interact(background::runAll);
    MapPages.contextMenu(robot, view, HALFWAY_LATITUDE, NEAR_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM)::fire);
    robot.interact(background::runAll);
    final int added = boxes.getItems().size();
    final int selected = boxes.getSelectionModel().getSelectedIndex();
    final boolean editorShown = window.stage().getScene().lookup(".box-editor").isVisible();
    MapPages.contextMenu(robot, view, HALFWAY_LATITUDE, NEAR_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM)::fire);
    final int waitingAgain = background.size();
    MapPages.contextMenu(robot, view, HALFWAY_LATITUDE, FAR_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM)::fire);
    final String far = text(robot, window, "status");
    robot.interact(MapPages.menu()::hide);
    final TextField heading = robot.lookup("#" + BoxEditor.HEADING_ID).queryAs(TextField.class);
    robot.interact(() -> {
      heading.setText(String.valueOf(EASTWARDS));
      heading.fireEvent(new ActionEvent());
    });
    robot.interact(background::runAll);
    robot.interact(() -> robot.lookup("#" + BoxEditor.DISPLAY_ID).queryAs(ComboBox.class).getSelectionModel()
                              .select(HeadingDisplay.SHOWN.ordinal()));
    robot.interact(background::runAll);
    final Optional<String> cap = ((BoxRow) boxes.getItems().get(1)).heading();
    robot.interact(() -> robot.lookup("#" + BoxEditor.REMOVE_ID).queryAs(Button.class).fire());
    robot.interact(background::runAll);
    final int removed = boxes.getItems().size();
    final int noneSelected = boxes.getSelectionModel().getSelectedIndex();
    fire(robot, window, "undoItem");
    robot.interact(background::runAll);
    final int broughtBack = boxes.getItems().size();
    final Optional<String> capBack = ((BoxRow) boxes.getItems().get(1)).heading();
    fire(robot, window, "saveAsItem");
    robot.interact(background::runAll);
    final RoadbookEdits exported = OpenRallyReader.read(file).edits();
    final Route exportedRoute = OpenRallyReader.read(file).route();
    MapPages.drag(robot, view, "finish", CASTLE_LATITUDE, FAR_LONGITUDE);
    robot.interact(background::runAll);
    robot.interact(background::runAll);
    final boolean hiddenBefore = Strips.firstOutOfSight(robot, window.stage(), boxes);
    robot.interact(() -> {
      heading.setText(String.valueOf(EASTWARDS));
      heading.fireEvent(new ActionEvent());
    });
    robot.interact(background::runAll);
    final boolean seen = Strips.seen(robot, boxes, 0);
    assertAll(
        () -> assertTrue(hiddenBefore, "the start out of sight before it is corrected"),
        () -> assertTrue(seen, "the start corrected seen"),
        () -> assertEquals(TURNING_BOXES + 1, added),
        () -> assertEquals(1, selected),
        () -> assertTrue(editorShown),
        () -> assertEquals(0, waitingAgain),
        () -> assertEquals(TEXTS.getString(MainController.ADD_FAR), far),
        () -> assertEquals(Optional.of("CAP " + EASTWARDS), cap),
        () -> assertEquals(TURNING_BOXES, removed),
        () -> assertEquals(-1, noneSelected),
        () -> assertEquals(TURNING_BOXES + 1, broughtBack),
        () -> assertEquals(Optional.of("CAP " + EASTWARDS), capBack),
        () -> assertEquals(1, exported.added().size()),
        () -> assertEquals(List.of(1), exportedRoute.maneuvers()),
        () -> assertEquals(TURNING_BOXES, exportedRoute.points().size()),
        () -> assertEquals(OptionalInt.of(EASTWARDS), exported.boxes().getFirst().heading()),
        () -> assertEquals(HeadingDisplay.SHOWN, exported.boxes().getFirst().display()),
        () -> assertEquals(TURNING_BOXES + 1, boxes.getItems().size()),
        () -> assertEquals(Optional.of("CAP " + EASTWARDS), ((BoxRow) boxes.getItems().get(1)).heading()));
  }

  /**
   * "Add a waypoint here" places a masked waypoint on a box added where the user right-clicks near the route, away
   * from the boxes, and selects it; near a box, on that box, without adding one; far from the route, the status bar
   * says so; "Start a speed zone here" places a DZ, framed in the strip, a DZ on the box with a WPM replacing it, and a
   * DZ after it finishing its zone, at the speed limit of the map. The waypoints are in the roadbook exported.
   *
   * @param robot the TestFX robot.
   *
   * @throws Exception if the page of the map does not load in time, or the roadbook exported cannot be read.
   */
  @Test
  @DisplayName("Phase 5: a waypoint is placed on a box added on the route, or on the box near the place asked")
  void waypointTest(final FxRobot robot) throws Exception {
    final Path file = data.resolve("waypoints.gpx");
    final Shown window = show(robot, FakeRouter.turning(), owner -> Optional.of(file));
    robot.interact(background::runAll);
    final WebView view = map(window);
    final ListView<?> boxes = boxes(robot, window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    robot.interact(background::runAll);
    MapPages.contextMenu(robot, view, HALFWAY_LATITUDE, NEAR_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM + 1)::fire);
    robot.interact(background::runAll);
    final int added = boxes.getItems().size();
    final int selected = boxes.getSelectionModel().getSelectedIndex();
    MapPages.contextMenu(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM + 1)::fire);
    robot.interact(background::runAll);
    MapPages.contextMenu(robot, view, HALFWAY_LATITUDE, FAR_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM + 1)::fire);
    final String far = text(robot, window, "status");
    robot.interact(MapPages.menu()::hide);
    MapPages.contextMenu(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM + 2)::fire);
    robot.interact(background::runAll);
    MapPages.contextMenu(robot, view, HALFWAY_LATITUDE, NEAR_LONGITUDE);
    robot.interact(MapPages.menu().getItems().get(ADD_BOX_ITEM + 2)::fire);
    robot.interact(background::runAll);
    fire(robot, window, "saveAsItem");
    robot.interact(background::runAll);
    final List<Optional<WaypointKind>> kinds = boxes.getItems().stream()
        .map(row -> ((BoxRow) row).box().waypoint().map(Waypoint::kind)).toList();
    assertAll(
        () -> assertEquals(TURNING_BOXES + 1, added),
        () -> assertEquals(1, selected),
        () -> assertEquals(TURNING_BOXES + 1, boxes.getItems().size()),
        () -> assertEquals(TEXTS.getString(MainController.ADD_FAR), far),
        () -> assertEquals(Optional.of(WaypointKind.WPM), kinds.getFirst()),
        () -> assertEquals(Optional.of(WaypointKind.DZ), kinds.get(1)),
        () -> assertEquals(Optional.of(WaypointKind.DZ), kinds.getLast()),
        () -> assertEquals(OptionalInt.of(FakeRouter.TURNING_LIMIT),
                           ((BoxRow) boxes.getItems().get(1)).box().waypoint().orElseThrow().speed()),
        () -> assertEquals(OptionalInt.of(FakeRouter.TURNING_LIMIT), ((BoxRow) boxes.getItems().get(1)).limit()),
        () -> assertTrue(((BoxRow) boxes.getItems().getLast()).sign().isPresent()),
        () -> assertTrue(((BoxRow) boxes.getItems().getLast()).zone().orElseThrow().finished().isPresent()),
        () -> assertEquals(WAYPOINTS_PLACED, OpenRallyReader.read(file).edits().boxes().stream()
            .filter(edit -> edit.waypoint().isPresent()).count()));
  }

  /**
   * Without map, the boxes of a roadbook and a box added have no speed limit of the map; with a map, each box has the
   * limit of the map where the route leaves it.
   */
  @Test
  @DisplayName("Phase 5: the boxes have the speed limit of the map where the route leaves them, none without map")
  void limitsTest() {
    final GeoPoint station = new GeoPoint(STATION_LATITUDE, STATION_LONGITUDE);
    final GeoPoint castle = new GeoPoint(CASTLE_LATITUDE, CASTLE_LONGITUDE);
    final Router turning = FakeRouter.turning();
    final Route route = turning.route(station, castle);
    final Roadbook roadbook = MainController.generate(route, RoadbookEdits.NONE, null);
    final GeoPoint turn = route.points().get(1);
    assertAll(
        () -> assertEquals(Collections.nCopies(TURNING_BOXES, OptionalInt.empty()),
                           MainController.limits(roadbook, null)),
        () -> assertEquals(Collections.nCopies(TURNING_BOXES, OptionalInt.of(FakeRouter.TURNING_LIMIT)),
                           MainController.limits(roadbook, turning)),
        () -> assertEquals(OptionalInt.empty(), MainController.limit(null, route, turn)),
        () -> assertEquals(OptionalInt.of(FakeRouter.TURNING_LIMIT), MainController.limit(turning, route, turn)));
  }

  /**
   * File › Export as GPX… writes the roadbook shown as a plain GPX, each box named by its number and its total distance
   * in the format of the language, and the status bar gives the number of boxes; a file that cannot be written says
   * so.
   *
   * @param robot the TestFX robot.
   *
   * @throws Exception if the page of the map does not load in time, or the file exported cannot be read.
   */
  @Test
  @DisplayName("Phase 6: File › Export as GPX… writes the plain GPX of the roadbook, or says why it cannot")
  void gpxTest(final FxRobot robot) throws Exception {
    final Path file = data.resolve("route.gpx");
    final Path missing = data.resolve("missing").resolve("route.gpx");
    final List<Path> targets = new ArrayList<>(List.of(file, missing));
    final Shown window = show(robot, FakeRouter.turning(), owner -> Optional.of(targets.removeFirst()));
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    robot.interact(background::runAll);
    fire(robot, window, "exportGpxItem");
    final String exporting = text(robot, window, "status");
    robot.interact(background::runAll);
    final String exported = text(robot, window, "status");
    fire(robot, window, "exportGpxItem");
    robot.interact(background::runAll);
    final String content = Files.readString(file);
    final String first = new MessageFormat(TEXTS.getString(MainController.GPX_BOX), Languages.FRENCH)
        .format(new Object[] {1, BoxRow.kilometres(Languages.FRENCH).format(0)});
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.STATUS_GPX_EXPORTING), exporting),
        () -> assertEquals(new MessageFormat(TEXTS.getString(MainController.STATUS_GPX_EXPORTED), Languages.FRENCH)
                               .format(new Object[] {TURNING_BOXES}), exported),
        () -> assertTrue(content.contains("<name>" + first + "</name>")),
        () -> assertTrue(content.contains(TEXTS.getString(MainController.EXPORT_NOTICE))),
        () -> assertEquals(TEXTS.getString(MainController.GPX_ERROR), text(robot, window, "status")));
  }

}
