package ch.varani.boxone.ui;

import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingError;
import ch.varani.boxone.routing.RoutingException;
import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link MainController} (DESIGN.md §2, §6, §14): the window, its menus on each system, the map in the centre,
 * the start and the finish clicked on the map, the fastest route with its distance and duration, the load and the
 * import of the map, the failures in the status bar, and the settings of the background map. The routing engine and
 * the thread of the long work are fakes; the work is run by the tests on the JavaFX Application Thread.
 */
@ExtendWith(ApplicationExtension.class)
final class MainControllerTest {

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

  /** The data folder of the tests. */
  @TempDir
  private Path data;
  /** The thread of the long work, run by the tests. */
  private final QueueExecutor background = new QueueExecutor();
  /** The light or dark mode of the system. */
  private final ObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>(ColorScheme.LIGHT);

  /**
   * A window shown by the tests.
   *
   * @param stage the window.
   * @param controller its controller.
   */
  private record Shown(Stage stage, MainController controller) {
  }

  /**
   * Shows the main window in a new stage.
   *
   * @param robot the TestFX robot.
   * @param mac whether the window is shown as on macOS.
   * @param exit quits the application.
   * @param services the services of the window.
   * @param scheme the light or dark mode of the system.
   *
   * @return the window.
   */
  private static Shown show(final FxRobot robot, final boolean mac, final Runnable exit,
                             final Services services, final ObjectProperty<ColorScheme> scheme) {
    final AtomicReference<Shown> window = new AtomicReference<>();
    robot.interact(() -> {
      final Stage stage = new Stage();
      final MainController controller = new MainController(stage, TEXTS, Languages.FRENCH, mac, scheme, exit,
                                                           services);
      controller.show();
      window.set(new Shown(stage, controller));
    });
    return window.get();
  }

  /**
   * Shows the main window, outside macOS, with a loader and an importer.
   *
   * @param robot the TestFX robot.
   * @param loader loads the graph.
   * @param importer imports a map.
   * @param chooser chooses the extract.
   *
   * @return the window.
   */
  private Shown show(final FxRobot robot, final Function<Path, Optional<? extends Router>> loader,
                      final BiFunction<Path, Path, ? extends Router> importer,
                      final Function<javafx.stage.Window, Optional<Path>> chooser) {
    return show(robot, false, () -> { }, new Services(data, background, loader, importer, chooser), scheme);
  }

  /**
   * Shows the main window, outside macOS, with a graph loaded from a router.
   *
   * @param robot the TestFX robot.
   * @param router the router of the graph.
   *
   * @return the window.
   */
  private Shown show(final FxRobot robot, final Router router) {
    return show(robot, folder -> Optional.of(router), (map, graph) -> router, owner -> Optional.empty());
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
   * Returns the veil over the map of a window.
   *
   * @param window the window.
   *
   * @return the veil.
   */
  private static Node busy(final Shown window) {
    return window.stage().getScene().lookup("#busyPane");
  }

  /**
   * Returns the menu bar of a window.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   *
   * @return the menu bar.
   */
  private static MenuBar menuBar(final FxRobot robot, final Shown window) {
    return robot.from(window.stage().getScene().getRoot()).lookup(".menu-bar").queryAs(MenuBar.class);
  }

  /**
   * Fires a menu item.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   * @param id the identifier of the item.
   */
  private static void fire(final FxRobot robot, final Shown window, final String id) {
    final MenuItem item = menuBar(robot, window).getMenus().stream().flatMap(menu -> menu.getItems().stream())
                                               .filter(candidate -> id.equals(candidate.getId())).findFirst()
                                               .orElseThrow();
    robot.interact(item::fire);
  }

  /**
   * Outside macOS: the File menu opens a map and quits, the Tools menu shows the options; the window has its title,
   * its sizes, the map in the centre, and says how to open a map when there is none.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Outside macOS, the File menu opens a map and quits, the Tools menu shows the options")
  void windowTest(final FxRobot robot) {
    final AtomicInteger exits = new AtomicInteger();
    final Shown window = show(robot, false, exits::incrementAndGet,
                               new Services(data, background, folder -> Optional.empty(),
                                            (map, graph) -> new FakeRouter(), owner -> Optional.empty()), scheme);
    final String loading = text(robot, window, "status");
    final boolean veiled = busy(window).isVisible();
    final String veil = text(robot, window, "busyText");
    final boolean hinted = window.stage().getScene().lookup("#busyHint").isVisible();
    robot.interact(background::runAll);
    final MenuBar menuBar = menuBar(robot, window);
    final List<Menu> menus = menuBar.getMenus();
    fire(robot, window, "quitItem");
    assertAll(
        () -> assertEquals("Box One", window.stage().getTitle()),
        () -> assertTrue(window.stage().isShowing()),
        () -> assertEquals(MainController.MIN_WIDTH, window.stage().getMinWidth()),
        () -> assertEquals(MainController.MIN_HEIGHT, window.stage().getMinHeight()),
        () -> assertEquals(MainController.WIDTH, window.stage().getScene().getWidth()),
        () -> assertEquals(MainController.HEIGHT, window.stage().getScene().getHeight()),
        () -> assertTrue(menuBar.isUseSystemMenuBar()),
        () -> assertEquals(List.of("fileMenu", "toolsMenu"), menus.stream().map(Menu::getId).toList()),
        () -> assertEquals(List.of("openMapItem", "quitSeparator", "quitItem"),
                           menus.getFirst().getItems().stream().map(MenuItem::getId).toList()),
        () -> assertEquals(1, exits.get()),
        () -> assertTrue(window.stage().getScene().lookup(".web-view") instanceof WebView),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_LOADING), loading),
        () -> assertTrue(veiled),
        () -> assertEquals(TEXTS.getString(MainController.BUSY_LOADING), veil),
        () -> assertFalse(hinted),
        () -> assertFalse(busy(window).isVisible()),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_NO_MAP), text(robot, window, "status")),
        () -> assertTrue(window.stage().getScene().getStylesheets().getFirst().endsWith(Theme.LIGHT_TOKENS)));
  }

  /**
   * On macOS, the application menu quits: the File menu opens a map and shows the settings; no Tools menu.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("On macOS, the File menu opens a map and shows the settings, without Quit nor Tools menu")
  void macTest(final FxRobot robot) {
    final Shown window = show(robot, true, () -> { },
                               new Services(data, background, folder -> Optional.empty(),
                                            (map, graph) -> new FakeRouter(), owner -> Optional.empty()), scheme);
    final List<Menu> menus = menuBar(robot, window).getMenus();
    assertAll(
        () -> assertEquals(List.of("fileMenu"), menus.stream().map(Menu::getId).toList()),
        () -> assertEquals(List.of("openMapItem", "settingsSeparator", "settingsItem"),
                           menus.getFirst().getItems().stream().map(MenuItem::getId).toList()));
  }

  /**
   * The first click places the start, the second the finish; the fastest route is then computed and shown with its
   * distance and duration; a third click begins a new itinerary.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the start and the finish clicked on the map give the fastest route, its distance and duration")
  void routeTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    robot.interact(background::runAll);
    final WebView view = map(window);
    final String start = text(robot, window, "status");
    final boolean cardBefore = window.stage().getScene().lookup("#routeCard").isVisible();
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    final String finish = text(robot, window, "status");
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    final String routing = text(robot, window, "status");
    robot.interact(background::runAll);
    final String routed = text(robot, window, "status");
    final String summary = text(robot, window, "summary");
    final boolean cardAfter = window.stage().getScene().lookup("#routeCard").isVisible();
    final int markers = MapPages.count(robot, view, ".marker");
    final int routes = MapPages.count(robot, view, ".route-fastest");
    MapPages.click(robot, view, CASTLE_LATITUDE, STATION_LONGITUDE);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_START), start),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_FINISH), finish),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_ROUTING), routing),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_ROUTED), routed),
        () -> assertEquals(RouteText.summary(new FakeRouter().route(SmokeTest.STATION, SmokeTest.CASTLE), TEXTS,
                                             Languages.FRENCH), summary),
        () -> assertFalse(cardBefore),
        () -> assertTrue(cardAfter),
        () -> assertEquals(2, markers),
        () -> assertEquals(1, routes),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_FINISH), text(robot, window, "status")),
        () -> assertEquals("", text(robot, window, "summary")),
        () -> assertEquals(1, MapPages.count(robot, view, ".marker")),
        () -> assertEquals(0, MapPages.count(robot, view, ".route-fastest")));
  }

  /**
   * The places clicked while the map loads are kept: once loaded, the route is computed.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the start and the finish clicked while the map loads give the route once it is loaded")
  void clickWhileLoadingTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    final String loading = text(robot, window, "status");
    robot.interact(background::runAll);
    robot.interact(background::runAll);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.STATUS_LOADING), loading),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_ROUTED), text(robot, window, "status")));
  }

  /**
   * A click while the route is computed cancels it: its result is never shown.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 1: a new click cancels the route being computed")
  void cancelTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, STATION_LONGITUDE);
    robot.interact(background::runAll);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_FINISH), text(robot, window, "status")),
        () -> assertEquals("", text(robot, window, "summary")));
  }

  /**
   * A failed routing is shown in the status bar: the reason of the engine, or an unexpected failure.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 1: a failed routing says why in the status bar")
  void routingFailureTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter(new RoutingException(RoutingError.NO_ROUTE)));
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    final String noRoute = text(robot, window, "status");
    final Shown other = show(robot, new FakeRouter(new IllegalStateException("broken")));
    robot.interact(background::runAll);
    final WebView otherView = map(other);
    MapPages.click(robot, otherView, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, otherView, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.ROUTING_ERROR + "NO_ROUTE"), noRoute),
        () -> assertEquals(TEXTS.getString(MainController.UNEXPECTED), text(robot, other, "status")));
  }

  /**
   * A graph that cannot be loaded is shown in the status bar.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: a graph that cannot be loaded says so in the status bar")
  void loadFailureTest(final FxRobot robot) {
    final Shown window = show(robot, folder -> {
      throw new RoutingException(RoutingError.INVALID_GRAPH);
    }, (map, graph) -> new FakeRouter(), owner -> Optional.empty());
    robot.interact(background::runAll);
    assertEquals(TEXTS.getString(MainController.ROUTING_ERROR + "INVALID_GRAPH"), text(robot, window, "status"));
  }

  /**
   * File › Open a map… imports the extract chosen into the folder of the graph, the item disabled meanwhile, and
   * releases the former graph; nothing happens when no file is chosen.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: File › Open a map… imports the extract chosen and replaces the graph")
  void openMapTest(final FxRobot robot) {
    final FakeRouter former = new FakeRouter();
    final FakeRouter imported = new FakeRouter();
    final Path extract = data.resolve("switzerland-latest.osm.pbf");
    final AtomicReference<Path> importedFrom = new AtomicReference<>();
    final AtomicReference<Path> importedInto = new AtomicReference<>();
    final AtomicInteger choices = new AtomicInteger();
    final Shown window = show(robot, folder -> Optional.of(former), (map, graph) -> {
      importedFrom.set(map);
      importedInto.set(graph);
      return imported;
    }, owner -> choices.getAndIncrement() == 0 ? Optional.empty() : Optional.of(extract));
    robot.interact(background::runAll);
    fire(robot, window, "openMapItem");
    final int waitingAfterCancel = background.size();
    fire(robot, window, "openMapItem");
    final MenuItem item = menuBar(robot, window).getMenus().getFirst().getItems().getFirst();
    final boolean disabled = item.isDisable();
    final String importing = text(robot, window, "status");
    final boolean veiled = busy(window).isVisible();
    final String hint = text(robot, window, "busyHint");
    robot.interact(background::runAll);
    assertAll(
        () -> assertTrue(veiled),
        () -> assertEquals(TEXTS.getString(MainController.BUSY_IMPORT_HINT), hint),
        () -> assertFalse(busy(window).isVisible()),
        () -> assertEquals(0, waitingAfterCancel),
        () -> assertTrue(disabled),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_IMPORTING), importing),
        () -> assertFalse(item.isDisable()),
        () -> assertEquals(extract, importedFrom.get()),
        () -> assertEquals(data.resolve(DataFolder.GRAPH), importedInto.get()),
        () -> assertEquals(1, former.getClosings()),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_START), text(robot, window, "status")));
  }

  /**
   * An extract that cannot be imported is shown in the status bar, and the item can be used again.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: an extract that cannot be imported says so in the status bar")
  void importFailureTest(final FxRobot robot) {
    final Shown window = show(robot, folder -> Optional.empty(), (map, graph) -> {
      throw new RoutingException(RoutingError.INVALID_MAP);
    }, owner -> Optional.of(data.resolve("photo.pbf")));
    robot.interact(background::runAll);
    fire(robot, window, "openMapItem");
    robot.interact(background::runAll);
    final MenuItem item = menuBar(robot, window).getMenus().getFirst().getItems().getFirst();
    assertAll(
        () -> assertFalse(item.isDisable()),
        () -> assertFalse(busy(window).isVisible()),
        () -> assertEquals(TEXTS.getString(MainController.ROUTING_ERROR + "INVALID_MAP"),
                           text(robot, window, "status")));
  }

  /**
   * Closing the window releases the graph on the thread of the long work.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: closing the window releases the graph of the map")
  void closeTest(final FxRobot robot) {
    final FakeRouter router = new FakeRouter();
    final Shown window = show(robot, router);
    robot.interact(background::runAll);
    robot.interact(() -> window.controller().close());
    robot.interact(background::runAll);
    assertEquals(1, router.getClosings());
  }

  /**
   * The settings choose MapTiler with a key and a fast zoom: they are saved, and the map shows its tiles and zooms
   * faster; cancelled, nothing changes.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the settings choose the provider of the background map and save its key")
  void settingsTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    final WebView view = map(window);
    fire(robot, window, "optionsItem");
    robot.interact(() -> button(robot, ButtonBar.ButtonData.CANCEL_CLOSE).fire());
    final boolean savedAfterCancel = Files.exists(data.resolve(DataFolder.SETTINGS));
    fire(robot, window, "optionsItem");
    robot.interact(() -> {
      robot.lookup("#" + SettingsDialog.TILES_ID).queryAs(ComboBox.class).getSelectionModel().select(1);
      robot.lookup("#" + SettingsDialog.ZOOM_ID).queryAs(ComboBox.class).getSelectionModel().select(2);
      robot.lookup(".password-field").queryAs(PasswordField.class).setText("my-key");
      button(robot, ButtonBar.ButtonData.OK_DONE).fire();
    });
    WaitForAsyncUtils.waitForFxEvents();
    final Settings saved = new SettingsStore(data.resolve(DataFolder.SETTINGS)).load();
    assertAll(
        () -> assertFalse(savedAfterCancel),
        () -> assertEquals(new Settings(TileProvider.MAPTILER, "my-key", ZoomSensitivity.HIGH), saved),
        () -> assertEquals(ZoomSensitivity.HIGH.getPixelsPerLevel(), ((Number) MapPages.script(robot, view,
                           "boxOne.getWheel()")).intValue()),
        () -> assertTrue(String.valueOf(MapPages.script(robot, view,
                         "document.querySelector('.leaflet-control-attribution').textContent")).contains("MapTiler")));
  }

  /**
   * Settings that cannot be saved are shown in the status bar.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException if the file blocking the data folder cannot be written.
   */
  @Test
  @DisplayName("Phase 1: settings that cannot be saved say so in the status bar")
  void settingsFailureTest(final FxRobot robot) throws IOException {
    final Path blocked = Files.writeString(data.resolve("file"), "not a folder");
    final Shown window = show(robot, false, () -> { },
                               new Services(blocked, background, folder -> Optional.empty(),
                                            (map, graph) -> new FakeRouter(), owner -> Optional.empty()), scheme);
    fire(robot, window, "optionsItem");
    robot.interact(() -> button(robot, ButtonBar.ButtonData.OK_DONE).fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(TEXTS.getString(MainController.SETTINGS_ERROR), text(robot, window, "status"));
  }

  /**
   * The map follows the light or dark mode of the system.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 1: the map follows the light or dark mode of the system")
  void themeTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    final WebView view = map(window);
    final String light = color(robot, view);
    robot.interact(() -> scheme.set(ColorScheme.DARK));
    assertAll(
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.LIGHT_TOKENS)).get("-c-accent"), light),
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.DARK_TOKENS)).get("-c-accent"),
                           color(robot, view)));
  }

  /**
   * Returns the accent of the page of the map.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map.
   *
   * @return the value of the variable {@code --c-accent} of the page.
   */
  private static String color(final FxRobot robot, final WebView view) {
    return String.valueOf(MapPages.script(robot, view,
                                          "document.documentElement.style.getPropertyValue('--c-accent').trim()"));
  }

  /**
   * Returns a button of the dialog shown.
   *
   * @param robot the TestFX robot.
   * @param data the role of the button.
   *
   * @return the button.
   */
  private static Button button(final FxRobot robot, final ButtonBar.ButtonData data) {
    final DialogPane pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
    final ButtonType type = pane.getButtonTypes().stream().filter(candidate -> candidate.getButtonData() == data)
                                .findFirst().orElseThrow();
    return (Button) pane.lookupButton(type);
  }

}
