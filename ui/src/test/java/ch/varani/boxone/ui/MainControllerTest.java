package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.detour.Budget;
import ch.varani.boxone.detour.DetourError;
import ch.varani.boxone.export.OpenRallyReader;
import ch.varani.boxone.export.OpenRallyWriter;
import ch.varani.boxone.export.Origin;
import ch.varani.boxone.export.ReadError;
import ch.varani.boxone.export.ReadException;
import ch.varani.boxone.export.RoadbookFile;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingError;
import ch.varani.boxone.routing.RoutingException;
import javafx.application.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
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
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  /** The index of the budget of 30 minutes in the choice of the detour: after 0, 5… 25 minutes. */
  private static final int THIRTY_MINUTES = 6;

  /** The data folder of the tests. */
  @TempDir
  private Path data;
  /** The thread of the long work, run by the tests. */
  private final QueueExecutor background = new QueueExecutor();
  /** The light or dark mode of the system. */
  private final ObjectProperty<ColorScheme> scheme = new SimpleObjectProperty<>(ColorScheme.LIGHT);
  /** The accent of the system. */
  private final ObjectProperty<Color> accent = new SimpleObjectProperty<>(Color.web("#007AFF"));
  /** The mode and the colours of the system: the accent, a white background and a black text. */
  private final SystemColors colors = new SystemColors(scheme, accent, new SimpleObjectProperty<>(Color.WHITE),
                                                       new SimpleObjectProperty<>(Color.BLACK));

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
   * @param colors the mode and the colours of the system.
   *
   * @return the window.
   */
  private static Shown show(final FxRobot robot, final boolean mac, final Runnable exit,
                             final Services services, final SystemColors colors) {
    final AtomicReference<Shown> window = new AtomicReference<>();
    robot.interact(() -> {
      final Stage stage = new Stage();
      final MainController controller = new MainController(stage, TEXTS, Languages.FRENCH, mac, colors, exit,
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
    return show(robot, loader, importer, chooser, owner -> Optional.empty(), owner -> Optional.empty());
  }

  /**
   * Shows the main window, outside macOS, with all its services.
   *
   * @param robot the TestFX robot.
   * @param loader loads the graph.
   * @param importer imports a map.
   * @param chooser chooses the extract.
   * @param saver chooses the file of the roadbook exported.
   * @param opener chooses the roadbook to open.
   *
   * @return the window.
   */
  private Shown show(final FxRobot robot, final Function<Path, Optional<? extends Router>> loader,
                      final BiFunction<Path, Path, ? extends Router> importer,
                      final Function<javafx.stage.Window, Optional<Path>> chooser,
                      final Function<javafx.stage.Window, Optional<Path>> saver,
                      final Function<javafx.stage.Window, Optional<Path>> opener) {
    return show(robot, false, () -> { }, new Services(data, background, loader, importer, chooser, saver, opener),
                colors);
  }

  /**
   * Shows the main window, outside macOS, with a graph loaded from a router and the choice of the file of the
   * roadbook.
   *
   * @param robot the TestFX robot.
   * @param router the router of the graph.
   * @param saver chooses the file of the roadbook.
   *
   * @return the window.
   */
  private Shown show(final FxRobot robot, final Router router,
                      final Function<javafx.stage.Window, Optional<Path>> saver) {
    return show(robot, folder -> Optional.of(router), (map, graph) -> router, owner -> Optional.empty(), saver,
                owner -> Optional.empty());
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
    robot.interact(item(robot, window, id)::fire);
  }

  /**
   * Returns a menu item.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   * @param id the identifier of the item.
   *
   * @return the item.
   */
  private static MenuItem item(final FxRobot robot, final Shown window, final String id) {
    return menuBar(robot, window).getMenus().stream().flatMap(menu -> menu.getItems().stream())
                                 .filter(candidate -> id.equals(candidate.getId())).findFirst().orElseThrow();
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
                                            (map, graph) -> new FakeRouter(), owner -> Optional.empty(),
                                            owner -> Optional.empty(), owner -> Optional.empty()), colors);
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
        () -> assertEquals(List.of("fileMenu", "routeMenu", "toolsMenu"), menus.stream().map(Menu::getId).toList()),
        () -> assertEquals(List.of("openMapItem", "openRoadbookItem", "exportItem", "quitSeparator", "quitItem"),
                           menus.getFirst().getItems().stream().map(MenuItem::getId).toList()),
        () -> assertEquals(1, exits.get()),
        () -> assertTrue(window.stage().getScene().lookup(".web-view") instanceof WebView),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_LOADING), loading),
        () -> assertTrue(veiled),
        () -> assertEquals(TEXTS.getString(MainController.BUSY_LOADING), veil),
        () -> assertFalse(hinted),
        () -> assertFalse(busy(window).isVisible()),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_NO_MAP), text(robot, window, "status")),
        () -> assertTrue(window.stage().getScene().getStylesheets().getFirst().startsWith(Theme.DATA)),
        () -> assertTrue(window.stage().getScene().getStylesheets().get(1).endsWith(Theme.LIGHT_TOKENS)));
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
                                            (map, graph) -> new FakeRouter(), owner -> Optional.empty(),
                                            owner -> Optional.empty(), owner -> Optional.empty()), colors);
    final List<Menu> menus = menuBar(robot, window).getMenus();
    assertAll(
        () -> assertEquals(List.of("fileMenu", "routeMenu"), menus.stream().map(Menu::getId).toList()),
        () -> assertEquals(List.of("openMapItem", "openRoadbookItem", "exportItem", "settingsSeparator",
                                   "settingsItem"),
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
   * File › Export the roadbook… is disabled until a route is shown; it then writes the roadbook of the route into the
   * file chosen, in the background, and gives its number of boxes; nothing happens when no file is chosen; a new
   * click disables it again.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 2: File › Export the roadbook… writes the roadbook of the route shown for Terra Pirata")
  void exportTest(final FxRobot robot) throws TimeoutException {
    final Path file = data.resolve("yverdon.gpx");
    final AtomicInteger choices = new AtomicInteger();
    final Shown window = show(robot, new FakeRouter(),
                               owner -> choices.getAndIncrement() == 0 ? Optional.empty() : Optional.of(file));
    robot.interact(background::runAll);
    final WebView view = map(window);
    final MenuItem item = item(robot, window, "exportItem");
    final boolean disabledBefore = item.isDisable();
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    final boolean enabled = !item.isDisable();
    fire(robot, window, "exportItem");
    final int waitingAfterCancel = background.size();
    fire(robot, window, "exportItem");
    final String exporting = text(robot, window, "status");
    robot.interact(background::runAll);
    final String exported = text(robot, window, "status");
    MapPages.click(robot, view, CASTLE_LATITUDE, STATION_LONGITUDE);
    assertAll(
        () -> assertTrue(disabledBefore),
        () -> assertTrue(enabled),
        () -> assertEquals(0, waitingAfterCancel),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_EXPORTING), exporting),
        () -> assertEquals("Roadbook exporté, 2 cases. Importez le fichier dans RDS pour le lire dans Terra Pirata.",
                           exported),
        () -> assertTrue(Files.readString(file).contains("<name>yverdon</name>")),
        () -> assertTrue(Files.readString(file).contains("<desc>" + TEXTS.getString(MainController.EXPORT_NOTICE)
                                                         + "</desc>")),
        () -> assertTrue(item.isDisable()));
  }

  /**
   * Route › Generate a detour… is disabled until a route is shown; cancelled, nothing happens; asked with a budget and
   * the seed proposed, the detour is generated in the background, drawn over the fastest route, its distance and
   * duration in the card, its budget and the fastest duration in the status bar, its seed at the right of it; a new
   * click on the map begins a new itinerary, without detour.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 3: Route › Generate a detour… shows a detour within the budget, with its seed")
  void detourTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    robot.interact(background::runAll);
    final WebView view = map(window);
    final MenuItem item = menuBar(robot, window).getMenus().get(1).getItems().getFirst();
    final boolean disabledBefore = item.isDisable();
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    final boolean enabled = !item.isDisable();
    fire(robot, window, "detourItem");
    robot.interact(() -> button(robot, ButtonBar.ButtonData.CANCEL_CLOSE).fire());
    final int waitingAfterCancel = background.size();
    fire(robot, window, "detourItem");
    final AtomicReference<String> seed = new AtomicReference<>();
    robot.interact(() -> {
      seed.set(robot.lookup("#" + DetourDialog.SEED_ID).queryAs(TextField.class).getText());
      robot.lookup("#" + DetourDialog.BUDGET_ID).queryAs(ComboBox.class).getSelectionModel().select(THIRTY_MINUTES);
      button(robot, ButtonBar.ButtonData.OK_DONE).fire();
    });
    WaitForAsyncUtils.waitForFxEvents();
    final String detouring = text(robot, window, "status");
    robot.interact(background::runAll);
    final String detoured = text(robot, window, "status");
    final String title = text(robot, window, "routeTitle");
    final String summary = text(robot, window, "summary");
    final TextField seedField = robot.from(window.stage().getScene().getRoot()).lookup("#seed")
                                     .queryAs(TextField.class);
    final String shownSeed = seedField.getText();
    final boolean seedVisible = seedField.getParent().isVisible();
    final int fastest = MapPages.count(robot, view, ".route-fastest");
    final int detours = MapPages.count(robot, view, ".route-detour");
    MapPages.click(robot, view, CASTLE_LATITUDE, STATION_LONGITUDE);
    assertAll(
        () -> assertTrue(disabledBefore),
        () -> assertTrue(enabled),
        () -> assertEquals(0, waitingAfterCancel),
        () -> assertTrue(DetourDialog.isValid(seed.get())),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_DETOURING), detouring),
        () -> assertTrue(detoured.startsWith("Détour d’au plus 30 min de plus que l’itinéraire le plus rapide "
                                             + "(1 h 15)."), detoured),
        () -> assertEquals(TEXTS.getString(MainController.DETOUR_TITLE), title),
        () -> assertTrue(summary.endsWith("1 h 16") || summary.endsWith("1 h 17") || summary.endsWith("1 h 18"),
                         summary),
        () -> assertEquals(seed.get(), shownSeed),
        () -> assertTrue(seedVisible),
        () -> assertEquals(1, fastest),
        () -> assertEquals(1, detours),
        () -> assertEquals("", seedField.getText()),
        () -> assertFalse(seedField.getParent().isVisible()),
        () -> assertEquals(TEXTS.getString(MainController.ROUTE_TITLE), text(robot, window, "routeTitle")),
        () -> assertEquals(0, MapPages.count(robot, view, ".route-detour")),
        () -> assertTrue(item.isDisable()));
  }

  /**
   * The kinds of road forbidden in the dialog of the detour are forbidden to every routing of the detour, kept in the
   * settings of the user, and proposed again for the next detour.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Issue #11: the kinds of road forbidden to a detour are kept in the settings for the next ones")
  void avoidedTest(final FxRobot robot) throws TimeoutException {
    final FakeRouter fake = new FakeRouter();
    final Shown window = show(robot, fake);
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    final int before = fake.getAvoided().size();
    fire(robot, window, "detourItem");
    robot.interact(() -> {
      robot.lookup("#" + DetourDialog.AVOID_ID + RoadKind.MOTORWAY.name()).queryAs(CheckBox.class).setSelected(true);
      button(robot, ButtonBar.ButtonData.OK_DONE).fire();
    });
    WaitForAsyncUtils.waitForFxEvents();
    robot.interact(background::runAll);
    final List<Set<RoadKind>> detour = List.copyOf(fake.getAvoided().subList(before, fake.getAvoided().size()));
    fire(robot, window, "detourItem");
    final AtomicReference<Boolean> proposed = new AtomicReference<>();
    robot.interact(() -> {
      proposed.set(robot.lookup("#" + DetourDialog.AVOID_ID + RoadKind.MOTORWAY.name()).queryAs(CheckBox.class)
                        .isSelected());
      button(robot, ButtonBar.ButtonData.CANCEL_CLOSE).fire();
    });
    assertAll(
        () -> assertFalse(detour.isEmpty()),
        () -> assertTrue(detour.stream().allMatch(Set.of(RoadKind.MOTORWAY)::equals), detour::toString),
        () -> assertEquals(Set.of(RoadKind.MOTORWAY),
                           new SettingsStore(data.resolve(DataFolder.SETTINGS)).load().avoided()),
        () -> assertTrue(proposed.get()));
  }

  /**
   * A detour that cannot be generated says why in the status bar, the fastest route staying shown; a click while a
   * detour is generated cancels it.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 3: a detour that cannot be generated says why in the status bar")
  void detourFailureTest(final FxRobot robot) throws TimeoutException {
    final FakeRouter fake = new FakeRouter();
    final Shown window = show(robot, new Router() {
      @Override
      public Route route(final List<GeoPoint> points, final Set<RoadKind> avoided) {
        if (points.size() > 2) {
          throw new RoutingException(RoutingError.POINT_NOT_FOUND);
        }
        return fake.route(points, avoided);
      }

      @Override
      public void close() {
        fake.close();
      }
    });
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    final String summary = text(robot, window, "summary");
    generate(robot, window);
    robot.interact(background::runAll);
    final String failed = text(robot, window, "status");
    final String kept = text(robot, window, "summary");
    generate(robot, window);
    MapPages.click(robot, view, STATION_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.DETOUR_ERROR + DetourError.NO_DETOUR.name()), failed),
        () -> assertEquals(summary, kept),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_FINISH), text(robot, window, "status")));
  }

  /**
   * Asks for a detour with the budget and the seed proposed.
   *
   * @param robot the TestFX robot.
   * @param window the window.
   */
  private static void generate(final FxRobot robot, final Shown window) {
    fire(robot, window, "detourItem");
    robot.interact(() -> button(robot, ButtonBar.ButtonData.OK_DONE).fire());
    WaitForAsyncUtils.waitForFxEvents();
  }

  /**
   * A roadbook that cannot be written is shown in the status bar.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Phase 2: a roadbook that cannot be written says so in the status bar")
  void exportFailureTest(final FxRobot robot) throws TimeoutException {
    final Path missing = data.resolve("missing").resolve("yverdon.gpx");
    final Shown window = show(robot, new FakeRouter(), owner -> Optional.of(missing));
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    fire(robot, window, "exportItem");
    robot.interact(background::runAll);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.EXPORT_ERROR), text(robot, window, "status")),
        () -> assertFalse(Files.exists(missing)));
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
        () -> assertEquals(new Settings(TileProvider.MAPTILER, "my-key", ZoomSensitivity.HIGH, Set.of()), saved),
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
                                            (map, graph) -> new FakeRouter(), owner -> Optional.empty(),
                                            owner -> Optional.empty(), owner -> Optional.empty()), colors);
    fire(robot, window, "optionsItem");
    robot.interact(() -> button(robot, ButtonBar.ButtonData.OK_DONE).fire());
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(TEXTS.getString(MainController.SETTINGS_ERROR), text(robot, window, "status"));
  }

  /**
   * The map follows the accent and the light or dark mode of the system.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("The map follows the accent and the light or dark mode of the system")
  void themeTest(final FxRobot robot) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter());
    final WebView view = map(window);
    final String lightAccent = color(robot, view, "accent");
    final String lightDanger = color(robot, view, "danger");
    robot.interact(() -> accent.set(Color.web("#FF9500")));
    final String orange = color(robot, view, "accent");
    robot.interact(() -> scheme.set(ColorScheme.DARK));
    assertAll(
        () -> assertEquals("#007AFF", lightAccent),
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.LIGHT_TOKENS)).get("-c-danger"), lightDanger),
        () -> assertEquals("#FF9500", orange),
        () -> assertEquals(Theme.tokens(Theme.class.getResource(Theme.DARK_TOKENS)).get("-c-danger"),
                           color(robot, view, "danger")));
  }

  /**
   * Returns a colour token of the page of the map.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map.
   * @param token the name of the token, without its prefix ({@code accent} for {@code --c-accent}).
   *
   * @return the value of the variable {@code --c-<token>} of the page.
   */
  private static String color(final FxRobot robot, final WebView view, final String token) {
    return String.valueOf(MapPages.script(robot, view, "document.documentElement.style.getPropertyValue('--c-"
                                                       + token + "').trim()"));
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

  /**
   * Generates a detour of 30 minutes from the station to the castle with the seed proposed, and exports it.
   *
   * @param robot the TestFX robot.
   * @param file the file of the roadbook exported.
   *
   * @return the window, showing the detour exported.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  private Shown exportDetour(final FxRobot robot, final Path file) throws TimeoutException {
    final Shown window = show(robot, new FakeRouter(), owner -> Optional.of(file));
    robot.interact(background::runAll);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    fire(robot, window, "detourItem");
    robot.interact(() -> {
      robot.lookup("#" + DetourDialog.BUDGET_ID).queryAs(ComboBox.class).getSelectionModel().select(THIRTY_MINUTES);
      button(robot, ButtonBar.ButtonData.OK_DONE).fire();
    });
    WaitForAsyncUtils.waitForFxEvents();
    robot.interact(background::runAll);
    fire(robot, window, "exportItem");
    robot.interact(background::runAll);
    return window;
  }

  /**
   * File › Open a roadbook… shows a detour exported as it was: its route drawn alone, its card, its seed, its budget
   * and the duration of the fastest route in the status bar; without graph, no detour can be generated, but the
   * roadbook can be exported again, the same; a map imported then keeps it shown, enables the detours and proposes
   * its budget; nothing happens when no file is chosen.
   *
   * @param robot the TestFX robot.
   *
   * @throws Exception if the page of the map does not load in time, or a roadbook cannot be read.
   */
  @Test
  @DisplayName("Issue #5: File › Open a roadbook… shows a detour exported, with its seed and its budget")
  void openDetourTest(final FxRobot robot) throws Exception {
    final Path file = data.resolve("detour.gpx");
    final Path again = data.resolve("again.gpx");
    final Shown exporter = exportDetour(robot, file);
    final String exportedSummary = text(robot, exporter, "summary");
    final String exportedSeed = robot.from(exporter.stage().getScene().getRoot()).lookup("#seed")
                                     .queryAs(TextField.class).getText();
    final AtomicInteger choices = new AtomicInteger();
    final Shown window = show(robot, folder -> Optional.empty(), (map, graph) -> new FakeRouter(),
                               owner -> Optional.of(data.resolve("switzerland-latest.osm.pbf")),
                               owner -> Optional.of(again),
                               owner -> choices.getAndIncrement() == 0 ? Optional.empty() : Optional.of(file));
    robot.interact(background::runAll);
    final WebView view = map(window);
    fire(robot, window, "openRoadbookItem");
    final int waitingAfterCancel = background.size();
    fire(robot, window, "openRoadbookItem");
    final String opening = text(robot, window, "status");
    robot.interact(background::runAll);
    final String status = text(robot, window, "status");
    final String title = text(robot, window, "routeTitle");
    final String summary = text(robot, window, "summary");
    final String seed = robot.from(window.stage().getScene().getRoot()).lookup("#seed").queryAs(TextField.class)
                             .getText();
    final int fastest = MapPages.count(robot, view, ".route-fastest");
    final int detours = MapPages.count(robot, view, ".route-detour");
    final int markers = MapPages.count(robot, view, ".marker");
    final boolean detourDisabled = item(robot, window, "detourItem").isDisable();
    final boolean exportDisabled = item(robot, window, "exportItem").isDisable();
    fire(robot, window, "exportItem");
    robot.interact(background::runAll);
    fire(robot, window, "openMapItem");
    robot.interact(background::runAll);
    final String summaryAfterImport = text(robot, window, "summary");
    final boolean detourEnabled = !item(robot, window, "detourItem").isDisable();
    fire(robot, window, "detourItem");
    final AtomicInteger proposed = new AtomicInteger();
    robot.interact(() -> {
      proposed.set(robot.lookup("#" + DetourDialog.BUDGET_ID).queryAs(ComboBox.class).getSelectionModel()
                        .getSelectedIndex());
      button(robot, ButtonBar.ButtonData.CANCEL_CLOSE).fire();
    });
    assertAll(
        () -> assertEquals(0, waitingAfterCancel),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_OPENING), opening),
        () -> assertTrue(status.startsWith("Détour d’au plus 30 min de plus que l’itinéraire le plus rapide "
                                           + "(1 h 15)."), status),
        () -> assertEquals(TEXTS.getString(MainController.DETOUR_TITLE), title),
        () -> assertEquals(exportedSummary, summary),
        () -> assertEquals(exportedSeed, seed),
        () -> assertEquals(0, fastest),
        () -> assertEquals(1, detours),
        () -> assertEquals(2, markers),
        () -> assertTrue(detourDisabled),
        () -> assertFalse(exportDisabled),
        () -> assertEquals(OpenRallyReader.read(file), OpenRallyReader.read(again)),
        () -> assertEquals(summary, summaryAfterImport),
        () -> assertTrue(detourEnabled),
        () -> assertEquals(THIRTY_MINUTES, proposed.get()));
  }

  /**
   * File › Open a roadbook… shows a fastest route exported, without seed, the detours enabled with a graph; a click
   * then begins a new itinerary.
   *
   * @param robot the TestFX robot.
   *
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Issue #5: File › Open a roadbook… shows a fastest route exported")
  void openFastestTest(final FxRobot robot) throws TimeoutException {
    final Path file = data.resolve("fastest.gpx");
    final Shown exporter = show(robot, new FakeRouter(), owner -> Optional.of(file));
    robot.interact(background::runAll);
    final WebView exporterView = map(exporter);
    MapPages.click(robot, exporterView, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, exporterView, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    robot.interact(background::runAll);
    fire(robot, exporter, "exportItem");
    robot.interact(background::runAll);
    final FakeRouter router = new FakeRouter();
    final Shown window = show(robot, folder -> Optional.of(router), (map, graph) -> router,
                               owner -> Optional.empty(), owner -> Optional.empty(), owner -> Optional.of(file));
    robot.interact(background::runAll);
    final WebView view = map(window);
    fire(robot, window, "openRoadbookItem");
    robot.interact(background::runAll);
    final String status = text(robot, window, "status");
    final String title = text(robot, window, "routeTitle");
    final String seed = robot.from(window.stage().getScene().getRoot()).lookup("#seed").queryAs(TextField.class)
                             .getText();
    final int fastest = MapPages.count(robot, view, ".route-fastest");
    final boolean detourEnabled = !item(robot, window, "detourItem").isDisable();
    MapPages.click(robot, view, CASTLE_LATITUDE, STATION_LONGITUDE);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.STATUS_ROUTED), status),
        () -> assertEquals(TEXTS.getString(MainController.ROUTE_TITLE), title),
        () -> assertEquals("", seed),
        () -> assertEquals(1, fastest),
        () -> assertTrue(detourEnabled),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_FINISH), text(robot, window, "status")),
        () -> assertEquals("", text(robot, window, "summary")));
  }

  /**
   * A roadbook that cannot be opened says why in a dialog over the window, titled by the action: a missing file, a
   * detour of an unknown budget; the status bar is left as before the opening, or as the routing asked before set it
   * meanwhile, and the route shown stays.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException if the roadbook of the test cannot be written.
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Issue #5: a roadbook that cannot be opened says why in a dialog, the route shown stays")
  void openFailureTest(final FxRobot robot) throws IOException, TimeoutException {
    final Path invalid = invalidRoadbook();
    final AtomicInteger choices = new AtomicInteger();
    final FakeRouter router = new FakeRouter();
    final Shown window = show(robot, folder -> Optional.of(router), (map, graph) -> router,
                               owner -> Optional.empty(), owner -> Optional.empty(),
                               owner -> Optional.of(choices.getAndIncrement() == 0 ? data.resolve("missing.gpx")
                                                                                   : invalid));
    robot.interact(background::runAll);
    fire(robot, window, "openRoadbookItem");
    robot.interact(background::runAll);
    final String missingStatus = text(robot, window, "status");
    final List<String> missing = alert(robot);
    final WebView view = map(window);
    MapPages.click(robot, view, STATION_LATITUDE, STATION_LONGITUDE);
    MapPages.click(robot, view, CASTLE_LATITUDE, CASTLE_LONGITUDE);
    fire(robot, window, "openRoadbookItem");
    robot.interact(background::runAll);
    final List<String> damaged = alert(robot);
    assertAll(
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_START), missingStatus),
        () -> assertEquals(List.of(TEXTS.getString(RoadbookFiles.OPEN_TITLE),
                                   TEXTS.getString(MainController.OPEN_ERROR),
                                   TEXTS.getString(MainController.READ_ERROR + ReadError.UNREADABLE.name()),
                                   TEXTS.getString(MainController.OPEN_CLOSE)), missing),
        () -> assertEquals(TEXTS.getString(MainController.READ_ERROR + ReadError.INVALID.name()), damaged.get(2)),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_ROUTED), text(robot, window, "status")),
        () -> assertFalse(text(robot, window, "summary").isEmpty()),
        () -> assertTrue(robot.lookup(".dialog-pane").queryAll().isEmpty()));
  }

  /**
   * A roadbook being opened is cancelled by a new opening, and by a click on the map: neither its route nor its
   * failure is shown.
   *
   * @param robot the TestFX robot.
   *
   * @throws IOException if the roadbook of the test cannot be written.
   * @throws TimeoutException if the page of the map does not load in time.
   */
  @Test
  @DisplayName("Issue #5: a roadbook being opened is cancelled by a new opening or a click")
  void openCancelledTest(final FxRobot robot) throws IOException, TimeoutException {
    final Path invalid = invalidRoadbook();
    final FakeRouter router = new FakeRouter();
    final Shown window = show(robot, folder -> Optional.of(router), (map, graph) -> router,
                               owner -> Optional.empty(), owner -> Optional.empty(), owner -> Optional.of(invalid));
    robot.interact(background::runAll);
    fire(robot, window, "openRoadbookItem");
    fire(robot, window, "openRoadbookItem");
    MapPages.click(robot, map(window), STATION_LATITUDE, STATION_LONGITUDE);
    robot.interact(background::runAll);
    assertAll(
        () -> assertTrue(robot.lookup(".dialog-pane").queryAll().isEmpty()),
        () -> assertEquals(TEXTS.getString(MainController.STATUS_PICK_FINISH), text(robot, window, "status")));
  }

  /**
   * Writes a roadbook whose detour has a budget in hours, unknown to this version.
   *
   * @return the file of the roadbook, in the data folder of the test.
   *
   * @throws IOException if it cannot be written.
   */
  private Path invalidRoadbook() throws IOException {
    final Path invalid = data.resolve("hours.gpx");
    OpenRallyWriter.save(RoadbookGenerator.generate(new FakeRouter().route(SmokeTest.STATION, SmokeTest.CASTLE)),
                         new Origin.Detour(SmokeTest.STATION, SmokeTest.CASTLE, 1, "HOURS", 1, Set.of(),
                                           FakeRouter.DURATION), "hours", "Indicative", invalid);
    return invalid;
  }

  /**
   * Reads the dialog shown, then closes it with its button.
   *
   * @param robot the TestFX robot.
   *
   * @return the title of its window, its heading, its text and the label of its button.
   */
  private static List<String> alert(final FxRobot robot) {
    final DialogPane pane = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
    final Button close = button(robot, ButtonBar.ButtonData.OK_DONE);
    final List<String> texts = List.of(((Stage) pane.getScene().getWindow()).getTitle(), pane.getHeaderText(),
                                       pane.getContentText(), close.getText());
    robot.interact(close::fire);
    return texts;
  }

  /**
   * The detour of a roadbook read back is checked: an amount out of the bounds of its unit, or an unknown kind of
   * road, is invalid; a fastest route, or a detour of this version, is kept; a failure that is not a reading is
   * unexpected.
   */
  @Test
  @DisplayName("Issue #5: the budget and the kinds of road of a detour opened are checked")
  void checkedTest() {
    final Route route = new FakeRouter().route(SmokeTest.STATION, SmokeTest.CASTLE);
    final RoadbookFile fastest = new RoadbookFile(route, new Origin.Fastest(SmokeTest.STATION, SmokeTest.CASTLE));
    final RoadbookFile detour = new RoadbookFile(route, new Origin.Detour(
        SmokeTest.STATION, SmokeTest.CASTLE, 1, "PERCENT", Budget.MAX_PERCENT, Set.of("TOLL"), FakeRouter.DURATION));
    final RoadbookFile sevenMinutes = new RoadbookFile(route, new Origin.Detour(
        SmokeTest.STATION, SmokeTest.CASTLE, 1, "MINUTES", Budget.STEP + 2, Set.of(), FakeRouter.DURATION));
    final RoadbookFile bridges = new RoadbookFile(route, new Origin.Detour(
        SmokeTest.STATION, SmokeTest.CASTLE, 1, "MINUTES", Budget.STEP, Set.of("BRIDGE"), FakeRouter.DURATION));
    assertAll(
        () -> assertEquals(fastest, MainController.checked(fastest)),
        () -> assertEquals(detour, MainController.checked(detour)),
        () -> assertEquals(Set.of(RoadKind.TOLL), MainController.kinds((Origin.Detour) detour.origin())),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> MainController.checked(sevenMinutes)).getError()),
        () -> assertEquals(ReadError.INVALID,
                           assertThrows(ReadException.class, () -> MainController.checked(bridges)).getError()),
        () -> assertEquals(MainController.UNEXPECTED, MainController.readError(new IllegalStateException("broken"))));
  }

}
