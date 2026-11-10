package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.detour.Budget;
import ch.varani.boxone.detour.Detour;
import ch.varani.boxone.detour.DetourException;
import ch.varani.boxone.detour.DetourGenerator;
import ch.varani.boxone.export.OpenRallyWriter;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;

/**
 * The main window (DESIGN.md §6, §14): the menu bar, the map in the centre and the status bar, with the fastest route
 * between the start and the finish clicked on the map (development plan, phase 1).
 *
 * <p><b>Sequence</b>: {@link #show()} loads the view {@value #VIEW} with the texts of the language of the application,
 * makes the menu bar the global one on macOS, keeps the menu items of the system (Settings… in the File menu on macOS,
 * whose application menu quits; Options… in a Tools menu and Quit in the File menu elsewhere), applies the theme of
 * the system ({@link Theme}) to the window and to the map, shows the map with the tiles of the settings, then shows the
 * window and loads, in the background, the graph of the last map imported. While a graph is loaded or imported, a veil
 * covers the map, with a progress indicator and what is happening, and the map cannot be clicked.</p>
 *
 * <p><b>Route</b>: a click on the map places the start, the next one the finish ({@link Itinerary}); the fastest route
 * is then computed in the background, a route still being computed being cancelled, and shown on the map with its
 * distance and duration in a card at the top right of the map. Without graph, the status bar says how to open a map.
 * File › Open a map… imports an extract of OpenStreetMap in the background, replacing the graph; the import of
 * GraphHopper cannot be interrupted, so the item is disabled until it ends. The long work runs one task after the
 * other on the executor of the {@link Services}; the results come back on the JavaFX Application Thread.</p>
 *
 * <p><b>Roadbook</b> (development plan, phase 2): once a route is shown, File › Export the roadbook… asks for a GPX
 * file ({@link RoadbookFiles}), then, in the background, generates the boxes of the route ({@link RoadbookGenerator})
 * and writes them for Terra Pirata ({@link OpenRallyWriter}); the status bar gives the number of boxes, or says why
 * the file could not be written. The item is disabled while no route is shown.</p>
 *
 * <p><b>Detour</b> (development plan, phase 3): once a route is shown, Route › Generate a detour… asks for the budget
 * and the seed ({@link DetourDialog}, a new seed proposed each time), then, in the background, generates a detour
 * from the start to the finish ({@link DetourGenerator}); it is drawn over the fastest route, its distance and
 * duration in the card of the route, its budget and the duration of the fastest route in the status bar, and its
 * seed at the right of the status bar, in a field that can be selected and copied. The roadbook exported is then the
 * one of the detour. A click on the map, as for the fastest route, cancels a detour being generated, and begins a new
 * itinerary. The item is disabled while no route is shown, and while a map is imported.</p>
 *
 * <p>The places clicked are personal data (AGENTS.md §5): never logged.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class MainController {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the name of the application, the title of the window. */
  static final String TITLE = "app.title";
  /** The FXML file of the window. */
  static final String VIEW = "main.fxml";
  /** The minimum width of the window, in pixels (DESIGN.md §2). */
  static final double MIN_WIDTH = 960;
  /** The minimum height of the window, in pixels (DESIGN.md §2). */
  static final double MIN_HEIGHT = 600;
  /** The width of the window at the first start, in pixels (DESIGN.md §2). */
  static final double WIDTH = 1280;
  /** The height of the window at the first start, in pixels (DESIGN.md §2). */
  static final double HEIGHT = 800;
  /** The key of the status while the graph is loaded. */
  static final String STATUS_LOADING = "status.loading";
  /** The key of the status without graph. */
  static final String STATUS_NO_MAP = "status.noMap";
  /** The key of the status while a map is imported. */
  static final String STATUS_IMPORTING = "status.importing";
  /** The key of the status waiting for the start. */
  static final String STATUS_PICK_START = "status.pickStart";
  /** The key of the status waiting for the finish. */
  static final String STATUS_PICK_FINISH = "status.pickFinish";
  /** The key of the status while the route is computed. */
  static final String STATUS_ROUTING = "status.routing";
  /** The key of the status once the route is shown. */
  static final String STATUS_ROUTED = "status.route";
  /** The beginning of the keys of the failures of the routing ({@code error.routing.<code>}). */
  static final String ROUTING_ERROR = "error.routing.";
  /** The key of an unexpected failure of the long work. */
  static final String UNEXPECTED = "error.unexpected";
  /** The key of the title of the veil while the graph is loaded. */
  static final String BUSY_LOADING = "busy.loading";
  /** The key of the title of the veil while a map is imported. */
  static final String BUSY_IMPORTING = "busy.importing";
  /** The key of the caption of the veil while a map is imported. */
  static final String BUSY_IMPORT_HINT = "busy.importHint";
  /** The margin of the card of the route to the edges of the map, in pixels (DESIGN.md §5). */
  static final double CARD_MARGIN = 12;
  /** The key of the failure to save the settings. */
  static final String SETTINGS_ERROR = "error.settings";
  /** The key of the status while the roadbook is exported. */
  static final String STATUS_EXPORTING = "status.exporting";
  /** The key of the status once the roadbook is exported, with its number of boxes. */
  static final String STATUS_EXPORTED = "status.exported";
  /** The key of the failure to export the roadbook. */
  static final String EXPORT_ERROR = "error.export";
  /** The key of the status while a detour is generated. */
  static final String STATUS_DETOURING = "status.detouring";
  /** The key of the status once a detour is shown, with its budget and the duration of the fastest route. */
  static final String STATUS_DETOUR = "status.detour";
  /** The beginning of the keys of the failures of the detours ({@code error.detour.<code>}). */
  static final String DETOUR_ERROR = "error.detour.";
  /** The key of the title of the card of the fastest route. */
  static final String ROUTE_TITLE = "route.title";
  /** The key of the title of the card of a detour. */
  static final String DETOUR_TITLE = "route.detourTitle";

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The window. */
  private final Stage stage;
  /** The texts of the user interface. */
  private final ResourceBundle texts;
  /** The locale of the application, for the formats. */
  private final Locale locale;
  /** Whether the application runs on macOS. */
  private final boolean mac;
  /** The mode and the colours of the system. */
  private final SystemColors colors;
  /** Quits the application. */
  private final Runnable exit;
  /** The data folder, the thread of the long work and the routing engine. */
  private final Services services;
  /** The settings file. */
  private final SettingsStore store;
  /** The start and the finish. */
  private final Itinerary itinerary = new Itinerary();
  /** The settings of the user. */
  private Settings settings;
  /** The map. */
  private MapView map;
  /** The router of the graph, or {@code null} while there is none. */
  private Router router;
  /** Whether the graph is being loaded or imported. */
  private boolean busy;
  /** The route or the detour being computed, or {@code null}. */
  private Task<?> routing;
  /** The route shown, the fastest one or a detour, or {@code null} while there is none. */
  private Route route;
  /** The budget of the last detour asked, proposed for the next one. */
  private Budget budget = Budget.FIFTEEN_MINUTES;
  /** The pane of the map, under the veil, injected by the view. */
  @FXML
  private StackPane mapPane;
  /** The veil over the map while the graph is loaded or imported, injected by the view. */
  @FXML
  private StackPane busyPane;
  /** The progress indicator of the veil, injected by the view. */
  @FXML
  private ProgressIndicator busyIndicator;
  /** The title of the veil, injected by the view. */
  @FXML
  private Label busyText;
  /** The caption of the veil, injected by the view. */
  @FXML
  private Label busyHint;
  /** The menu bar, injected by the view. */
  @FXML
  private MenuBar menuBar;
  /** The File menu, injected by the view. */
  @FXML
  private Menu fileMenu;
  /** The item Open a map…, injected by the view. */
  @FXML
  private MenuItem openMapItem;
  /** The item Export the roadbook…, injected by the view. */
  @FXML
  private MenuItem exportItem;
  /** The item Generate a detour…, injected by the view. */
  @FXML
  private MenuItem detourItem;
  /** The separator before Settings… (macOS), injected by the view. */
  @FXML
  private SeparatorMenuItem settingsSeparator;
  /** The item Settings… of the File menu (macOS), injected by the view. */
  @FXML
  private MenuItem settingsItem;
  /** The separator before Quit (outside macOS), injected by the view. */
  @FXML
  private SeparatorMenuItem quitSeparator;
  /** The item Quit (outside macOS), injected by the view. */
  @FXML
  private MenuItem quitItem;
  /** The Tools menu (outside macOS), injected by the view. */
  @FXML
  private Menu toolsMenu;
  /** The state of the work, at the left of the status bar, injected by the view. */
  @FXML
  private Label status;
  /** The card of the route, at the top right of the map, injected by the view. */
  @FXML
  private VBox routeCard;
  /** The distance and the duration of the route, in its card, injected by the view. */
  @FXML
  private Label summary;
  /** The title of the card of the route, injected by the view. */
  @FXML
  private Label routeTitle;
  /** The seed of the detour shown, at the right of the status bar, injected by the view. */
  @FXML
  private TextField seed;
  /** The label of the seed, injected by the view. */
  @FXML
  private Label seedLabel;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the controller of the main window; the window is shown by {@link #show()}.
   *
   * @param stage the window.
   * @param texts the texts of the user interface, in the language of the application.
   * @param locale the locale of the application ({@link Languages#resolve(Locale)}), for the formats.
   * @param mac whether the application runs on macOS.
   * @param colors the mode and the colours of the system.
   * @param exit quits the application ({@code Platform::exit}, replaced in the tests).
   * @param services the data folder, the thread of the long work and the routing engine.
   */
  MainController(final Stage stage, final ResourceBundle texts, final Locale locale, final boolean mac,
                 final SystemColors colors, final Runnable exit, final Services services) {
    this.stage = stage;
    this.texts = texts;
    this.locale = locale;
    this.mac = mac;
    this.colors = colors;
    this.exit = exit;
    this.services = services;
    this.store = new SettingsStore(services.dataFolder().resolve(DataFolder.SETTINGS));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Builds and shows the window, then loads the graph of the last map imported.
   */
  void show() {
    final Scene scene = new Scene(Views.load(MainController.class.getResource(VIEW), texts, this), WIDTH, HEIGHT);
    menuBar.setUseSystemMenuBar(true);
    if (mac) {
      fileMenu.getItems().removeAll(quitSeparator, quitItem);
      menuBar.getMenus().remove(toolsMenu);
    } else {
      fileMenu.getItems().removeAll(settingsSeparator, settingsItem);
    }
    final Theme theme = new Theme(scene, colors);
    settings = store.load();
    map = new MapView(texts, this::clicked);
    map.setColors(theme.tokens());
    map.setTiles(settings.source());
    map.setZoom(settings.zoom());
    theme.addListener(() -> map.setColors(theme.tokens()));
    mapPane.getChildren().addFirst(map.getNode());
    busyHint.visibleProperty().bind(busyHint.textProperty().isNotEmpty());
    busyHint.managedProperty().bind(busyHint.visibleProperty());
    routeCard.visibleProperty().bind(summary.textProperty().isNotEmpty());
    seedLabel.setLabelFor(seed);
    seed.getParent().visibleProperty().bind(seed.textProperty().isNotEmpty());
    seed.getParent().managedProperty().bind(seed.getParent().visibleProperty());
    StackPane.setMargin(routeCard, new Insets(CARD_MARGIN));
    stage.setTitle(texts.getString(TITLE));
    stage.setMinWidth(MIN_WIDTH);
    stage.setMinHeight(MIN_HEIGHT);
    stage.setScene(scene);
    stage.centerOnScreen();
    stage.show();
    load();
  }

  /**
   * Releases the graph of the map, on the thread of the long work, after the work already asked.
   */
  void close() {
    replaceRouter(null);
  }

  /**
   * Loads, in the background, the graph of the last map imported.
   */
  private void load() {
    busy = true;
    showBusy(BUSY_LOADING, null);
    setStatus(STATUS_LOADING);
    run(() -> services.loader().apply(graph()), loaded -> ready(loaded.orElse(null)), this::failed);
  }

  /**
   * Receives a click on the map: places the start or the finish, and computes the route once both are placed.
   *
   * @param place the place clicked.
   */
  private void clicked(final GeoPoint place) {
    cancelRouting();
    itinerary.place(place);
    showRoute(null, null);
    map.setPlaces(itinerary.getStart().orElse(null), itinerary.getFinish().orElse(null));
    next();
  }

  /**
   * Does the next step of the itinerary: waits for a graph, for the start or for the finish, or computes the route.
   */
  private void next() {
    if (router == null) {
      if (!busy) {
        setStatus(STATUS_NO_MAP);
      }
      return;
    }
    if (!itinerary.isComplete()) {
      setStatus(itinerary.getStart().isPresent() ? STATUS_PICK_FINISH : STATUS_PICK_START);
      return;
    }
    final Router current = router;
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    setStatus(STATUS_ROUTING);
    routing = run(() -> current.route(start, finish), this::routed, this::failed);
  }

  /**
   * Shows the fastest route on the map, and its distance and duration in the status bar.
   *
   * @param route the route.
   */
  private void routed(final Route route) {
    routing = null;
    map.setRoute(route);
    showRoute(route, null);
    setStatus(STATUS_ROUTED);
  }

  /**
   * Shows a detour over the fastest route, its budget and the duration of the fastest route in the status bar.
   *
   * @param detour the detour.
   */
  private void detoured(final Detour detour) {
    routing = null;
    map.setDetour(detour.fastest(), detour.route());
    showRoute(detour.route(), detour);
    status.setText(new MessageFormat(texts.getString(STATUS_DETOUR), locale).format(new Object[] {
        RouteText.duration(detour.budget().getDuration(), texts, locale),
        RouteText.duration(detour.fastest().duration(), texts, locale)}));
  }

  /**
   * Keeps the route shown, writes its distance and its duration in its card, with the seed of a detour, and enables
   * its export and the generation of a detour.
   *
   * @param shown the route shown, or {@code null} for none: the card is hidden and the export disabled.
   * @param detour the detour shown, or {@code null} for the fastest route or none.
   */
  private void showRoute(final Route shown, final Detour detour) {
    route = shown;
    summary.setText(shown == null ? "" : RouteText.summary(shown, texts, locale));
    routeTitle.setText(texts.getString(detour == null ? ROUTE_TITLE : DETOUR_TITLE));
    seed.setText(detour == null ? "" : String.valueOf(detour.seed()));
    exportItem.setDisable(shown == null);
    detourItem.setDisable(shown == null);
  }

  /**
   * Takes the router of a graph loaded or imported, and goes on with the itinerary.
   *
   * @param loaded the router, or {@code null} if there is no graph.
   */
  private void ready(final Router loaded) {
    busy = false;
    busyPane.setVisible(false);
    openMapItem.setDisable(false);
    router = loaded;
    next();
  }

  /**
   * Imports an extract of OpenStreetMap chosen by the user, replacing the graph: the item File › Open a map….
   */
  @FXML
  private void openMap() {
    final Optional<Path> file = services.chooser().apply(stage);
    if (file.isEmpty()) {
      return;
    }
    busy = true;
    openMapItem.setDisable(true);
    detourItem.setDisable(true);
    cancelRouting();
    replaceRouter(null);
    showBusy(BUSY_IMPORTING, BUSY_IMPORT_HINT);
    setStatus(STATUS_IMPORTING);
    run(() -> services.importer().apply(file.get(), graph()), this::ready, this::failed);
  }

  /**
   * Exports the roadbook of the route shown, for Terra Pirata, into the file chosen by the user: the item File ›
   * Export the roadbook….
   */
  @FXML
  private void exportRoadbook() {
    final Optional<Path> file = services.saver().apply(stage);
    if (file.isEmpty()) {
      return;
    }
    final Route exported = route;
    setStatus(STATUS_EXPORTING);
    run(() -> {
      final Roadbook roadbook = RoadbookGenerator.generate(exported);
      OpenRallyWriter.save(roadbook, RoadbookFiles.name(file.get()), file.get());
      return roadbook;
    }, roadbook -> status.setText(new MessageFormat(texts.getString(STATUS_EXPORTED), locale)
                                      .format(new Object[] {roadbook.boxes().size()})),
        _ -> setStatus(EXPORT_ERROR));
  }

  /**
   * Asks for the budget and the seed of a detour, and generates it when asked: the item Route › Generate a detour….
   */
  @FXML
  private void generateDetour() {
    final Dialog<DetourDialog.Request> dialog = new DetourDialog(stage, texts, locale, budget,
        DetourGenerator.newSeed(RandomGenerator.getDefault()), stage.getScene().getStylesheets()).getDialog();
    dialog.setOnHidden(event -> {
      if (dialog.getResult() != null) {
        detour(dialog.getResult());
      }
    });
    dialog.show();
  }

  /**
   * Generates, in the background, a detour from the start to the finish shown, a detour being generated cancelled.
   *
   * @param request the budget and the seed of the detour.
   */
  private void detour(final DetourDialog.Request request) {
    cancelRouting();
    budget = request.budget();
    final DetourGenerator generator = new DetourGenerator(router);
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    setStatus(STATUS_DETOURING);
    routing = run(() -> generator.generate(start, finish, request.budget(), request.seed()), this::detoured,
                  this::failed);
  }

  /**
   * Shows the settings, and applies them when they are saved: the item Settings… (macOS) or Options….
   */
  @FXML
  private void settings() {
    final Dialog<Settings> dialog = new SettingsDialog(stage, texts, settings, stage.getScene().getStylesheets())
        .getDialog();
    dialog.setOnHidden(event -> {
      if (dialog.getResult() != null) {
        apply(dialog.getResult());
      }
    });
    dialog.show();
  }

  /**
   * Saves new settings, and shows the map with their tiles and their sensitivity of the zoom.
   *
   * @param saved the new settings.
   */
  private void apply(final Settings saved) {
    try {
      store.save(saved);
    } catch (UncheckedIOException _) {
      setStatus(SETTINGS_ERROR);
      return;
    }
    settings = saved;
    map.setTiles(saved.source());
    map.setZoom(saved.zoom());
  }

  /**
   * Quits the application: the item Quit of the File menu.
   */
  @FXML
  private void quit() {
    exit.run();
  }

  /**
   * Cancels the route being computed, if any: its result is not shown.
   */
  private void cancelRouting() {
    if (routing != null) {
      routing.cancel();
      routing = null;
    }
  }

  /**
   * Replaces the router, releasing the former one on the thread of the long work, after the work already asked.
   *
   * @param replacement the new router, or {@code null}.
   */
  private void replaceRouter(final Router replacement) {
    final Router former = router;
    router = replacement;
    if (former != null) {
      services.background().execute(former::close);
    }
  }

  /**
   * Runs a long work on the thread of the long work; its result, or its failure, comes back on the JavaFX
   * Application Thread.
   *
   * @param <T> the type of the result.
   * @param work the work.
   * @param done receives the result, on the JavaFX Application Thread.
   * @param failed receives the exception of a failed work, on the JavaFX Application Thread.
   *
   * @return the task of the work, which can be cancelled.
   */
  private <T> Task<T> run(final Callable<T> work, final Consumer<T> done, final Consumer<Throwable> failed) {
    final Task<T> task = new Task<>() {
      @Override
      protected T call() throws Exception {
        return work.call();
      }
    };
    task.setOnSucceeded(event -> done.accept(task.getValue()));
    task.setOnFailed(event -> failed.accept(task.getException()));
    services.background().execute(task);
    return task;
  }

  /**
   * Shows the failure of a load, an import, a routing or a detour in the status bar; after a failed load or import,
   * there is no graph; after a failed detour, the route shown stays.
   *
   * @param failure the exception of the work.
   */
  private void failed(final Throwable failure) {
    routing = null;
    busy = false;
    busyPane.setVisible(false);
    openMapItem.setDisable(false);
    setStatus(switch (failure) {
      case final RoutingException error -> ROUTING_ERROR + error.getError().name();
      case final DetourException error -> DETOUR_ERROR + error.getError().name();
      default -> UNEXPECTED;
    });
  }

  /**
   * Shows the veil over the map, which cannot be clicked meanwhile, with what is happening.
   *
   * @param title the key of the title of the veil.
   * @param hint the key of its caption, or {@code null} for none.
   */
  private void showBusy(final String title, final String hint) {
    busyText.setText(texts.getString(title));
    busyHint.setText(hint == null ? "" : texts.getString(hint));
    busyIndicator.setAccessibleText(busyText.getText());
    busyPane.setVisible(true);
  }

  /**
   * Returns the folder of the graph.
   *
   * @return the folder of the graph, in the data folder.
   */
  private Path graph() {
    return services.dataFolder().resolve(DataFolder.GRAPH);
  }

  /**
   * Shows a text in the status bar.
   *
   * @param key the key of the text.
   */
  private void setStatus(final String key) {
    status.setText(texts.getString(key));
  }

}
