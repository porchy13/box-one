package ch.varani.boxone.ui;

import ch.varani.boxone.catalog.Catalogue;
import ch.varani.boxone.core.BoxEdit;
import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.core.RoadFeature;
import ch.varani.boxone.core.Roadbook;
import ch.varani.boxone.core.RoadbookEdits;
import ch.varani.boxone.core.Route;
import ch.varani.boxone.core.Waypoint;
import ch.varani.boxone.core.WaypointKind;
import ch.varani.boxone.detour.Budget;
import ch.varani.boxone.detour.Detour;
import ch.varani.boxone.detour.DetourException;
import ch.varani.boxone.detour.DetourGenerator;
import ch.varani.boxone.export.GpxWriter;
import ch.varani.boxone.export.OpenRallyReader;
import ch.varani.boxone.export.OpenRallyWriter;
import ch.varani.boxone.export.Origin;
import ch.varani.boxone.export.PdfWriter;
import ch.varani.boxone.export.ReadError;
import ch.varani.boxone.export.ReadException;
import ch.varani.boxone.export.RoadbookFile;
import ch.varani.boxone.roadbook.FeatureNotes;
import ch.varani.boxone.roadbook.RoadbookEditor;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.RoadKind;
import ch.varani.boxone.routing.Router;
import ch.varani.boxone.routing.RoutingException;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

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
 * <p><b>Route</b>: a click on the map places the start, the next one the finish ({@link Itinerary}); once both are
 * placed, a click does nothing, and they are moved by dragging their marker or by the context menu of the map (issue #4
 * of GitLab). The fastest route is then computed in the background, a route still being computed being cancelled, and
 * shown on the map with its distance and duration in a card at the top right of the map. Without graph, the status bar
 * says how to open a map. File › Open a map… imports an extract of OpenStreetMap in the background, replacing the
 * graph; the import of GraphHopper cannot be interrupted, so the item is disabled until it ends. The long work runs one
 * task after the other on the executor of the {@link Services}; the results come back on the JavaFX Application
 * Thread.</p>
 *
 * <p><b>Roadbook</b> (development plan, phase 2): once a route is shown, File › Save the roadbook as… asks for a GPX
 * file ({@link RoadbookFiles}), then, in the background, generates the boxes of the route ({@link RoadbookGenerator})
 * and writes them for Terra Pirata ({@link OpenRallyWriter}); the status bar gives the number of boxes, or says why
 * the file could not be written. The file saved, or opened, is then the file of the roadbook: File › Save the roadbook
 * writes it again without asking. The title of the window gives its name, and says when the roadbook is modified since
 * (another route, other changes of the user; a roadbook never saved, once the user changed it). The application asks
 * whether to save such changes before it closes (Save, Don't Save, Cancel), and closes only once a roadbook being
 * saved is saved. The items are disabled while no route is shown.</p>
 *
 * <p><b>Plain GPX</b> (development plan, phase 6; decision of the maintainer, 2026-11-20): File › Export as GPX… asks
 * for a GPX file ({@link RoadbookFiles#gpx}), then, in the background, writes the roadbook shown for other applications
 * ({@link GpxWriter}): its track and one point per box, named by its number and its total distance in the format of
 * the language; the status bar gives the number of boxes, or says why the file could not be written. The file is not
 * the file of the roadbook: the roadbook is not marked as saved. The item is disabled while no route is shown.</p>
 *
 * <p><b>PDF strip</b> (development plan, phase 6; decisions of the maintainer, 2026-11-20): File › Export as PDF… asks
 * for a PDF file ({@link RoadbookFiles#pdf}), then, in the background, writes the roadbook shown as a strip to print
 * for a roadbook holder ({@link PdfWriter}), the distances in the format of the language; the status bar gives the
 * number of boxes and of pages, or says why the file could not be written. As the plain GPX, the file is not the file
 * of the roadbook. The item is disabled while no route is shown.</p>
 *
 * <p><b>Detour</b> (development plan, phase 3): once a route is shown, Route › Generate a detour… asks for the budget
 * and the seed ({@link DetourDialog}, a new seed proposed each time), then, in the background, generates a detour
 * from the start to the finish ({@link DetourGenerator}); it is drawn over the fastest route, its distance and
 * duration in the card of the route, its budget and the duration of the fastest route in the status bar, and its
 * seed at the right of the status bar, in a field that can be selected and copied. The roadbook exported is then the
 * one of the detour. The start or the finish moved, as for the fastest route, cancels a detour being generated. The
 * item is disabled while no route is shown, and while there is no graph (a map imported, or none).</p>
 *
 * <p><b>Opening</b> (issue #5 of GitLab): the roadbook exported holds where its route comes from ({@link Origin}): the
 * start and the finish clicked and, for a detour, its seed, its budget, the kinds of road it avoids and the duration of
 * the fastest route. File › Open a roadbook… asks for a GPX file ({@link RoadbookFiles#opening}), reads it in the
 * background ({@link OpenRallyReader}), checks the budget and the kinds of road of its detour, then shows it as it was
 * exported: the start and the finish, the route (a detour drawn alone, the fastest route not being kept), its card,
 * its seed, its budget in the status bar; the budget is proposed for the next detour. It can be exported again, and a
 * detour generated from its start and finish once a graph is loaded; the graph loaded or imported meanwhile does not
 * replace it. A file that cannot be opened says why in a dialog ({@code error.read.<code>}). A start or a finish placed
 * cancels a file being read. File › Open a recent roadbook opens one of the roadbooks opened or saved last (as many as
 * the settings keep, {@value Settings#MIN_RECENT} by default); one that no longer exists is removed from the list, and
 * the user told so.</p>
 *
 * <p><b>Editor</b> (development plan, phase 5): the box selected in the strip is corrected in the editor under it
 * ({@link BoxEditor}): its total distance, its heading, whether it is shown, and its tulip; it can be removed, and a
 * box added where the user right-clicks on the route (Add a box here, within {@value RoadbookEditor#NEAR_ROUTE} m
 * of it). Each change can be undone (Edit › Undo), and the roadbook is drawn again with it, the box changed selected.
 * The changes are kept by their places when the route changes ({@link RoadbookEditor#retain(RoadbookEdits, Route)}:
 * the start or the finish moved, a detour generated), exported with the roadbook and opened with it.</p>
 *
 * <p><b>Training</b> (development plan, phase 7): Training › Symbol quiz shows the quiz of the catalogue
 * ({@link TrainingView}), Training › Reading of boxes the reading of the junctions of random routes drawn in the area
 * of the map seen ({@link ReadingView}, which borrows the map, {@link TrainingMap}, and its clicks), in the centre of
 * the window, over the three columns, which stay as they are; Training › Back to the roadbook, the button of the
 * exercise, or any item of the File, Edit and Route menus but Settings… brings them back, as these items act on the
 * roadbook.</p>
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
  /** The icon of the application in the window, at each size, in pixels, in {@value #ICON}. */
  static final List<Integer> ICON_SIZES = List.of(16, 32, 48, 64, 128, 256);
  /** The icon of the window at a size, in this package (drawn from {@code ui/src/packaging/icons/box-one.svg}). */
  static final String ICON = "icons/box-one-%d.png";
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
  /** The key of the reminder written into the roadbook exported: the route is indicative (issue #7 of GitLab). */
  static final String EXPORT_NOTICE = "export.notice";
  /** The key of the failure to export the roadbook. */
  static final String EXPORT_ERROR = "error.export";
  /** The key of the status while the plain GPX is exported. */
  static final String STATUS_GPX_EXPORTING = "status.gpxExporting";
  /** The key of the status once the plain GPX is exported, with its number of boxes. */
  static final String STATUS_GPX_EXPORTED = "status.gpxExported";
  /** The key of the name of the point of a box in the plain GPX, with its number and its total distance. */
  static final String GPX_BOX = "gpx.box";
  /** The key of the failure to export the plain GPX. */
  static final String GPX_ERROR = "error.gpx";
  /** The key of the status while the PDF strip is exported. */
  static final String STATUS_PDF_EXPORTING = "status.pdfExporting";
  /** The key of the status once the PDF strip is exported, with its number of boxes and of pages. */
  static final String STATUS_PDF_EXPORTED = "status.pdfExported";
  /** The key of the failure to export the PDF strip. */
  static final String PDF_ERROR = "error.pdf";
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
  /** The key of the status while a roadbook is opened. */
  static final String STATUS_OPENING = "status.opening";
  /** The beginning of the keys of the failures to open a roadbook ({@code error.read.<code>}). */
  static final String READ_ERROR = "error.read.";
  /** The key of the heading of the dialog of a failure to open a roadbook: the consequence. */
  static final String OPEN_ERROR = "open.error";
  /** The key of the button closing the dialog of a failure to open a roadbook. */
  static final String OPEN_CLOSE = "open.close";
  /** The key of the title of the roadbook, the name of its list for the readers of the screen. */
  static final String ROADBOOK_TITLE = "roadbook.title";
  /** The key of the number of boxes of the roadbook. */
  static final String ROADBOOK_COUNT = "roadbook.count";
  /** The key of the failure to draw the roadbook of the route. */
  static final String ROADBOOK_ERROR = "error.roadbook";
  /** The key of the title of the window showing a roadbook, with its name. */
  static final String TITLE_DOCUMENT = "title.document";
  /** The key of the title of the window showing a roadbook modified since it was saved or opened, with its name. */
  static final String TITLE_MODIFIED = "title.modified";
  /** The key of the name of a roadbook never saved. */
  static final String TITLE_UNTITLED = "title.untitled";
  /** The key of the status while the roadbook is saved before the application closes. */
  static final String STATUS_CLOSING = "status.closing";
  /** The key of the question asked when the application closes with changes not saved. */
  static final String CLOSE_QUESTION = "close.question";
  /** The key of the consequence of not saving, under the question. */
  static final String CLOSE_TEXT = "close.text";
  /** The key of the button saving the roadbook, then closing. */
  static final String CLOSE_SAVE = "close.save";
  /** The key of the button closing without saving. */
  static final String CLOSE_DISCARD = "close.discard";
  /** The key of the button cancelling the closing. */
  static final String CLOSE_CANCEL = "close.cancel";
  /** The key of an item of the roadbooks opened last: the name of the file and the name of its folder. */
  static final String RECENT_ITEM = "recent.item";
  /** The key of the heading of the dialog of a roadbook opened last that no longer exists, with its name. */
  static final String RECENT_MISSING = "recent.missing";
  /** The key of the text of the dialog of a roadbook opened last that no longer exists. */
  static final String RECENT_MISSING_TEXT = "recent.missingText";
  /** The keys of the strip removing the box selected (DESIGN.md §10: Del, ⌫ on macOS). */
  static final Set<KeyCode> REMOVE_KEYS = Set.of(KeyCode.DELETE, KeyCode.BACK_SPACE);
  /** The key of the status when a box is added too far from the route. */
  static final String ADD_FAR = "status.addFar";
  /** The key of the status when a box is dropped too far from the route. */
  static final String MOVE_FAR = "status.moveFar";
  /**
   * The position of the divider between the roadbook, at the left, and the map, as a part of the width of the window
   * (decision of the maintainer, 2026-11-17: the strip at the left, the map in the middle, the editor at the right).
   */
  static final double ROADBOOK_DIVIDER = 0.2;
  /** The position of the divider between the map and the editor of the box, at the right, as a part of the width. */
  static final double EDITOR_DIVIDER = 0.75;
  /** The other shortcut of Redo outside macOS, beside Shortcut+Shift+Z of its item (DESIGN.md §10: Ctrl+Y). */
  static final KeyCombination REDO_OTHER = new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN);

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
  /**
   * The catalogue of symbols of the roadbooks exported: the FIA (phase 4), whose lexicon the FIM and other rallies
   * share for the tulips.
   */
  private static final Catalogue CATALOGUE = Catalogue.fia();

  /** The router of the graph, or {@code null} while there is none. */
  private Router router;
  /** Whether the graph is being loaded or imported. */
  private boolean busy;
  /** The route or the detour being computed, or {@code null}. */
  private Task<?> routing;
  /** The roadbook being opened, or {@code null}: the route shown and the work asked stay until it is read. */
  private Task<?> reading;
  /** The roadbook of the route shown being drawn, or {@code null}. */
  private Task<?> drawing;
  /** The fastest route drawn under the detour shown, or {@code null} (no detour, or a detour opened). */
  private Route under;
  /** The states shown before, brought back by Undo and Redo (issue #4 of GitLab). */
  private final History<View> history = new History<>();
  /** The route shown, the fastest one or a detour, or {@code null} while there is none. */
  private Route route;
  /** Where the route shown comes from, written into its roadbook, or {@code null} while there is none. */
  private Origin origin;
  /** Whether the route shown was opened from a roadbook, kept when a graph is loaded or imported. */
  private boolean opened;
  /** The changes of the user to the roadbook of the route shown, kept by their places when the route changes. */
  private RoadbookEdits edits = RoadbookEdits.NONE;
  /** The editor of the box selected, under the strip. */
  private BoxEditor boxEditor;
  /** The quiz of symbols of the training mode, over the three columns while it is shown (phase 7a). */
  private TrainingView training;
  /** The reading of boxes of the training mode, over the three columns while it is shown (phase 7b). */
  private ReadingView boxReading;
  /** The map lent to the reading of boxes. */
  private TrainingMap trainingMap;
  /** The situations of a roadbook of the training mode, over the three columns while they are shown (phase 7). */
  private SituationView situations;
  /** The views of the training mode: the quiz, the reading of boxes and the situations, one shown at a time. */
  private List<Node> trainingModes = List.of();
  /** The file of the roadbook, opened or saved last, written again by Save; {@code null} before the first. */
  private Path file;
  /** The roadbook as it was saved or opened last, or {@code null} if it was never. */
  private Saved saved;
  /** The roadbook being saved, or {@code null}. */
  private Task<?> saving;
  /** Whether the application closes once the roadbook being saved is saved. */
  private boolean closing;
  /** The place of the box to select once the roadbook is drawn again, or {@code null} for none. */
  private GeoPoint toSelect;
  /**
   * Whether the strip is being filled again: its selection is cleared then restored, which must not move the map;
   * the box is shown on the map once the strip is filled, if it is another one.
   */
  private boolean redrawing;
  /**
   * The detour to generate again instead of the fastest route, once its start or its finish has moved (decision of
   * the maintainer, 2026-11-15: the same budget, the same kinds of road avoided, the same seed), or {@code null}.
   */
  private DetourDialog.Request again;
  /** The budget of the last detour asked, proposed for the next one. */
  private Budget budget = Budget.DEFAULT;
  /** The duration of the fastest route shown, alone or under a detour, of which a budget in percent is a share. */
  private Duration fastest = Duration.ZERO;
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
  /** The item Open a roadbook…, injected by the view. */
  @FXML
  private MenuItem openRoadbookItem;
  /** The menu Open a recent roadbook, injected by the view. */
  @FXML
  private Menu recentMenu;
  /** The item Save the roadbook, injected by the view. */
  @FXML
  private MenuItem saveItem;
  /** The item Save the roadbook as…, injected by the view. */
  @FXML
  private MenuItem saveAsItem;
  /** The item Export as GPX…, injected by the view. */
  @FXML
  private MenuItem exportGpxItem;
  /** The item Export as PDF…, injected by the view. */
  @FXML
  private MenuItem exportPdfItem;
  /** The item Undo, injected by the view. */
  @FXML
  private MenuItem undoItem;
  /** The item Redo, injected by the view. */
  @FXML
  private MenuItem redoItem;
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
  /** The Edit menu, injected by the view. */
  @FXML
  private Menu editMenu;
  /** The Route menu, injected by the view. */
  @FXML
  private Menu routeMenu;
  /** The item Back to the roadbook of the Training menu, injected by the view. */
  @FXML
  private MenuItem roadbookItem;
  /** The centre of the window: the three columns, under the training mode, injected by the view. */
  @FXML
  private StackPane centre;
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
  /** The map and the roadbook, side by side, injected by the view. */
  @FXML
  private SplitPane split;
  /** The pane of the roadbook, at the left of the map, injected by the view. */
  @FXML
  private VBox roadbookPane;
  /** The pane of the editor of the box selected, at the right of the map, injected by the view. */
  @FXML
  private VBox editorPane;
  /** The text shown in the pane of the editor while no box is selected, injected by the view. */
  @FXML
  private Label editorEmpty;
  /** The number of boxes of the roadbook, injected by the view. */
  @FXML
  private Label roadbookCount;
  /** The boxes of the roadbook, injected by the view. */
  @FXML
  private ListView<BoxRow> boxList;
  /** The empty state of the roadbook, injected by the view. */
  @FXML
  private Label roadbookEmpty;

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
    if (!mac) {
      scene.getAccelerators().put(REDO_OTHER, this::redo);
    }
    final Theme theme = new Theme(scene, colors);
    settings = store.load();
    updateRecent();
    map = new MapView(texts, this::clicked, this::moved, this::addBox, this::addWaypoint, this::moveBox);
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
    split.setDividerPositions(ROADBOOK_DIVIDER, EDITOR_DIVIDER);
    SplitPane.setResizableWithParent(roadbookPane, false);
    SplitPane.setResizableWithParent(editorPane, false);
    boxList.setCellFactory(list -> new BoxCell());
    boxList.setAccessibleText(texts.getString(ROADBOOK_TITLE));
    roadbookEmpty.visibleProperty().bind(Bindings.isEmpty(boxList.getItems()));
    boxEditor = new BoxEditor(texts, locale, CATALOGUE, edit -> changeEdits(edits.with(edit), edit.place()),
                              place -> changeEdits(edits.withRemoved(place), null));
    VBox.setVgrow(boxEditor.getNode(), Priority.ALWAYS);
    editorPane.getChildren().add(boxEditor.getNode());
    editorEmpty.visibleProperty().bind(boxEditor.getNode().visibleProperty().not());
    editorEmpty.managedProperty().bind(editorEmpty.visibleProperty());
    boxList.getSelectionModel().selectedItemProperty().addListener((property, before, selected) -> {
      if (!redrawing) {
        showBox(selected);
      }
      showEditor();
    });
    boxList.setOnKeyPressed(event -> {
      if (REMOVE_KEYS.contains(event.getCode()) && !event.isShortcutDown() && !event.isShiftDown()
          && !event.isAltDown()) {
        removeSelected();
        event.consume();
      }
    });
    showRoadbook(List.of());
    final LongSupplier seeds = () -> RandomGenerator.getDefault().nextLong();
    training = new TrainingView(texts, locale, CATALOGUE, seeds, this::leaveTraining);
    trainingMap = new TrainingMap(map, mapPane);
    boxReading = new ReadingView(new QuizText(texts, locale), CATALOGUE, () -> router, services.background(),
                                 trainingMap, seeds, this::leaveTraining);
    situations = new SituationView(texts, locale, CATALOGUE, services.background(), seeds, this::leaveTraining);
    trainingModes = List.of(training.getNode(), boxReading.getNode(), situations.getNode());
    for (final Node mode : trainingModes) {
      mode.setVisible(false);
      centre.getChildren().add(mode);
    }
    for (final Menu menu : List.of(fileMenu, editMenu, routeMenu)) {
      menu.addEventHandler(ActionEvent.ACTION, event -> {
        if (event.getTarget() != settingsItem) {
          leaveTraining();
        }
      });
    }
    stage.setOnCloseRequest(event -> {
      if (!mayClose()) {
        event.consume();
      }
    });
    updateTitle();
    stage.getIcons().setAll(icons());
    stage.setMinWidth(MIN_WIDTH);
    stage.setMinHeight(MIN_HEIGHT);
    stage.setScene(scene);
    stage.centerOnScreen();
    stage.show();
    load();
  }

  /**
   * Returns the icon of the application at each size, for the title bar and the task bar of the window and of its
   * dialogs (the installers carry their own, from {@code ui/src/packaging/icons/}).
   *
   * @return the images of the icon, the smallest first; an image that cannot be loaded is in error ({@link
   *         Image#isError()}), which the smoke test checks.
   */
  static List<Image> icons() {
    return ICON_SIZES.stream()
                     .map(size -> new Image(MainController.class.getResource(ICON.formatted(size)).toExternalForm()))
                     .toList();
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
   * Receives a click on the map: places the start or the finish, if one of them is not placed, and computes the route
   * once both are placed; once both are placed, a click does nothing (issue #4 of GitLab).
   *
   * @param place the place clicked.
   */
  private void clicked(final GeoPoint place) {
    if (trainingMap.click(place)) {
      return;
    }
    if (!itinerary.isComplete()) {
      edit(() -> itinerary.place(place));
    }
  }

  /**
   * Receives the start or the finish moved by the user, its marker dropped or by the context menu of the map, and
   * computes the route once both are placed (issue #4 of GitLab): the detour shown, if any, is generated again between
   * the new ends, with its budget, its kinds of road avoided and its seed, once a map is loaded.
   *
   * @param end the end moved.
   * @param place its new place.
   */
  private void moved(final Itinerary.End end, final GeoPoint place) {
    again = origin instanceof final Origin.Detour detour && router != null
            ? new DetourDialog.Request(budget(detour), kinds(detour), detour.seed()) : null;
    edit(() -> itinerary.move(end, place));
  }

  /**
   * Changes the itinerary, the state shown before being recorded for Undo: the work asked is cancelled, the route
   * shown is removed, and the route of the new itinerary computed once it is complete.
   *
   * @param change the change of the itinerary.
   */
  private void edit(final Runnable change) {
    cancelRouting();
    history.remember(current());
    change.run();
    display(new View(itinerary.getStart().orElse(null), itinerary.getFinish().orElse(null), null, null, null, false,
                     edits));
  }

  /**
   * Adds a box to the roadbook where the user asked it, by the context menu of the map: at the nearest point of the
   * route shown, if it is near enough; otherwise the status bar says so.
   *
   * @param place the place asked.
   */
  private void addBox(final GeoPoint place) {
    final Optional<GeoPoint> onRoute = RoadbookEditor.project(route, place);
    if (onRoute.isEmpty()) {
      setStatus(ADD_FAR);
      return;
    }
    changeEdits(edits.withAdded(onRoute.get()), onRoute.get());
  }

  /**
   * Places a waypoint where the user asked it, by the context menu of the map (phase 5, increment 4; decision of the
   * maintainer, 2026-11-18): on the box of the roadbook within {@value RoadbookEditor#NEAR_ROUTE} m of the nearest
   * point of the route shown, the nearest one, or on a box added there; a masked waypoint (WPM), or the start (DZ, at
   * {@value Waypoint#DEFAULT_SPEED} km/h) or the finish (FZ) of a speed zone (increment 5; decision of the maintainer,
   * 2026-11-19), with the radii of the FIA, its kind, its radii and its speed then chosen in the editor of the box,
   * which is selected. A DZ takes the speed limit of the map where the route leaves its box, if any (increment 5b;
   * decision of the maintainer, 2026-11-20). A place far from the route is said in the status bar.
   *
   * @param kind the kind of the waypoint.
   * @param place the place asked.
   */
  private void addWaypoint(final WaypointKind kind, final GeoPoint place) {
    final Optional<GeoPoint> onRoute = RoadbookEditor.project(route, place);
    if (onRoute.isEmpty()) {
      setStatus(ADD_FAR);
      return;
    }
    final Optional<BoxRow> near = boxList.getItems().stream()
        .min(Comparator.comparingDouble(row -> RoadbookEditor.distance(row.place(), onRoute.get())))
        .filter(row -> RoadbookEditor.distance(row.place(), onRoute.get()) <= RoadbookEditor.NEAR_ROUTE);
    final GeoPoint at = near.map(BoxRow::place).orElse(onRoute.get());
    final RoadbookEdits changed = near.isPresent() ? edits : edits.withAdded(at);
    final OptionalInt limit = near.isPresent() ? near.get().limit()
                                              : limit(router, RoadbookEditor.route(route, changed), at);
    changeEdits(changed.with(changed.edit(at).orElse(new BoxEdit(at))
                                    .withWaypoint(Optional.of(Waypoint.of(kind, limit)))), at);
  }

  /**
   * Reads the speed limit of the map at a box added to the route shown.
   *
   * @param on the router of the map, or {@code null} without map: no limit.
   * @param changed the route shown, with the box added.
   * @param at the place of the box added: a maneuver of the route.
   *
   * @return the limit of the map where the route leaves the box, in km/h; empty if the map gives none, or without
   *         map.
   */
  static OptionalInt limit(final Router on, final Route changed, final GeoPoint at) {
    if (on == null) {
      return OptionalInt.empty();
    }
    return changed.maneuvers().stream().filter(index -> changed.points().get(index).equals(at)).findFirst()
                  .map(index -> on.speedLimit(changed, index)).orElse(OptionalInt.empty());
  }

  /**
   * Moves the box selected where it is dropped on the map, dragged along the route: at the nearest point of the route,
   * if it is near enough, its distances, its tulip and its heading computed again there; otherwise the status bar says
   * so and the box goes back to its place.
   *
   * @param place the place where the box is dropped.
   */
  private void moveBox(final GeoPoint place) {
    final BoxRow selected = boxList.getSelectionModel().getSelectedItem();
    final Optional<GeoPoint> onRoute = RoadbookEditor.project(route, place);
    if (onRoute.isEmpty()) {
      setStatus(MOVE_FAR);
      showBox(selected);
      return;
    }
    if (!onRoute.get().equals(selected.place())) {
      changeEdits(edits.withMoved(selected.place(), onRoute.get()), onRoute.get());
    }
  }

  /**
   * Changes the roadbook of the route shown, the state shown before being recorded for Undo (which forgets the states
   * undone), and draws it again.
   *
   * @param changed the new changes of the user.
   * @param select the place of the box to select once drawn, or {@code null} for none (a box removed).
   */
  private void changeEdits(final RoadbookEdits changed, final GeoPoint select) {
    if (changed.equals(edits)) {
      return;
    }
    history.remember(current());
    edits = changed;
    undoItem.setDisable(false);
    redoItem.setDisable(true);
    toSelect = select;
    updateTitle();
    drawRoadbook(route, true);
  }

  /**
   * Removes the box selected in the strip, unless it is the start or the finish: the keys Delete and Backspace of the
   * strip (DESIGN.md §10: Del, ⌫ on macOS), as the button of its editor.
   */
  private void removeSelected() {
    final int index = boxList.getSelectionModel().getSelectedIndex();
    if (index > 0 && index < boxList.getItems().size() - 1) {
      changeEdits(edits.withRemoved(boxList.getItems().get(index).place()), null);
    }
  }

  /**
   * Shows the box selected in the strip in its editor, or hides the editor.
   */
  private void showEditor() {
    final int index = boxList.getSelectionModel().getSelectedIndex();
    boxEditor.show(index < 0 ? null : BoxEditor.Selection.of(boxList.getItems(), index, edits));
  }

  /**
   * Shows the training mode in the centre of the window, over the three columns, on its setup page (Training ›
   * Symbol quiz; development plan, phase 7a).
   */
  @FXML
  private void train() {
    showTraining(training.getNode());
    training.showSetup();
  }

  /**
   * Shows the reading of boxes in the centre of the window, over the three columns, on its setup page (Training ›
   * Reading of boxes; development plan, phase 7b).
   */
  @FXML
  private void read() {
    showTraining(boxReading.getNode());
    boxReading.showSetup();
  }

  /**
   * Shows the situations of a roadbook in the centre of the window, over the three columns, on their setup page
   * (Training › Roadbook situations; development plan, phase 7).
   */
  @FXML
  private void situate() {
    showTraining(situations.getNode());
    situations.showSetup();
  }

  /**
   * Shows an exercise of the training over the three columns, the others hidden, the map given back.
   *
   * @param shown the view of the exercise shown.
   */
  private void showTraining(final Node shown) {
    boxReading.showSetup();
    split.setVisible(false);
    for (final Node mode : trainingModes) {
      mode.setVisible(mode == shown);
    }
    roadbookItem.setDisable(false);
  }

  /**
   * Leaves the training mode, if it is shown, back to the three columns: by Training › Back to the roadbook, its
   * button, or an item of the File, Edit and Route menus, which act on the roadbook.
   */
  @FXML
  private void leaveTraining() {
    if (split.isVisible()) {
      return;
    }
    boxReading.showSetup();
    situations.showSetup();
    for (final Node mode : trainingModes) {
      mode.setVisible(false);
    }
    split.setVisible(true);
    roadbookItem.setDisable(true);
    boxList.requestFocus();
  }

  /**
   * Brings back the state shown before the last change: the item Edit › Undo.
   */
  @FXML
  private void undo() {
    bringBack(history::undo);
  }

  /**
   * Brings back the state undone last: the item Edit › Redo, or Shortcut+Y outside macOS.
   */
  @FXML
  private void redo() {
    bringBack(history::redo);
  }

  /**
   * Brings back a state of the history, if there is one: the work asked is cancelled, and the state shown.
   *
   * @param step takes the state shown, and returns the state to bring back, or nothing.
   */
  private void bringBack(final Function<View, Optional<View>> step) {
    step.apply(current()).ifPresent(view -> {
      cancelRouting();
      display(view);
    });
  }

  /**
   * Returns the state shown: the start, the finish, the route, where it comes from.
   *
   * @return the state shown.
   */
  private View current() {
    return new View(itinerary.getStart().orElse(null), itinerary.getFinish().orElse(null), under, route, origin,
                    opened, edits);
  }

  /**
   * Shows a state: its start and its finish, and its route, the fastest one or a detour, with its card, its seed and
   * its status; without route, the next step of the itinerary is done (the route computed if it is complete). The
   * items Undo and Redo are enabled when there is a state to bring back. A state of the route shown, only the changes
   * of its roadbook differing (Undo or Redo of the editor), draws the roadbook again only: the map stays where it is,
   * at its zoom, and the box selected stays selected.
   *
   * @param view the state to show.
   */
  private void display(final View view) {
    enableHistory();
    showState(view);
    updateTitle();
  }

  /**
   * Shows a state, the items of the history and the title of the window excepted: {@link #display(View)}.
   *
   * @param view the state to show.
   */
  private void showState(final View view) {
    if (view.route() != null && view.route() == route) {
      edits = view.edits();
      final BoxRow selected = boxList.getSelectionModel().getSelectedItem();
      toSelect = selected == null ? null : selected.place();
      drawRoadbook(route, true);
      return;
    }
    itinerary.set(view.start(), view.finish());
    opened = view.opened();
    edits = view.edits();
    under = view.under();
    map.setPlaces(view.start(), view.finish());
    if (view.route() == null) {
      showRoute(null, null);
      next();
    } else if (view.origin() instanceof final Origin.Detour detour) {
      map.setDetour(view.under(), view.route());
      fastest = detour.fastest();
      budget = budget(detour);
      showRoute(view.route(), detour);
      showBudget(budget);
    } else {
      map.setRoute(view.route());
      fastest = view.route().duration();
      showRoute(view.route(), view.origin());
      setStatus(STATUS_ROUTED);
    }
  }

  /**
   * Enables the items Undo and Redo when there is a state to bring back.
   */
  private void enableHistory() {
    undoItem.setDisable(!history.canUndo());
    redoItem.setDisable(!history.canRedo());
  }

  /**
   * Does the next step of the itinerary: waits for a graph, for the start or for the finish, or computes the route,
   * or the detour to generate again once its start or its finish has moved.
   */
  private void next() {
    final DetourDialog.Request request = again;
    again = null;
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
    if (request != null) {
      generate(request, this::fastestInstead);
      return;
    }
    final Router current = router;
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    setStatus(STATUS_ROUTING);
    routing = run(() -> current.route(start, finish), this::routed, this::failed);
  }

  /**
   * Shows the fastest route when the detour generated again between the ends moved fails, with the reason of the
   * failure in the status bar.
   *
   * @param failure the failure of the detour.
   */
  private void fastestInstead(final Throwable failure) {
    final String error = error(failure);
    final Router current = router;
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    routing = run(() -> current.route(start, finish), shown -> {
      routed(shown);
      setStatus(error);
    }, this::failed);
  }

  /**
   * Shows the fastest route on the map, and its distance and duration in the status bar.
   *
   * @param route the route.
   */
  private void routed(final Route route) {
    routing = null;
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    display(new View(start, finish, null, route, new Origin.Fastest(start, finish), false,
                     RoadbookEditor.retain(edits, route)));
  }

  /**
   * Shows a detour over the fastest route, its budget and the duration of the fastest route in the status bar.
   *
   * @param detour the detour.
   */
  private void detoured(final Detour detour) {
    routing = null;
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    if (route != null) {
      history.remember(current());
    }
    display(new View(start, finish, detour.fastest(), detour.route(), new Origin.Detour(
        start, finish, detour.seed(), detour.budget().unit().name(), detour.budget().amount(),
        detour.avoided().stream().map(RoadKind::name).collect(Collectors.toSet()), detour.fastest().duration()),
                     false, RoadbookEditor.retain(edits, detour.route())));
  }

  /**
   * Shows the budget of the detour shown and the duration of the fastest route in the status bar.
   *
   * @param shown the budget of the detour shown.
   */
  private void showBudget(final Budget shown) {
    status.setText(new MessageFormat(texts.getString(STATUS_DETOUR), locale).format(new Object[] {
        RouteText.budget(shown, fastest, texts, locale), RouteText.duration(fastest, texts, locale)}));
  }

  /**
   * Keeps the route shown and where it comes from, writes its distance and its duration in its card, with the seed of
   * a detour, and enables its export and the generation of a detour.
   *
   * @param shown the route shown, or {@code null} for none: the card is hidden and the export disabled.
   * @param from where the route comes from, or {@code null} for none.
   */
  private void showRoute(final Route shown, final Origin from) {
    route = shown;
    origin = from;
    summary.setText(shown == null ? "" : RouteText.summary(shown, texts, locale));
    if (from instanceof final Origin.Detour detour) {
      routeTitle.setText(texts.getString(DETOUR_TITLE));
      seed.setText(String.valueOf(detour.seed()));
    } else {
      routeTitle.setText(texts.getString(ROUTE_TITLE));
      seed.setText("");
    }
    saveItem.setDisable(shown == null);
    saveAsItem.setDisable(shown == null);
    exportGpxItem.setDisable(shown == null);
    exportPdfItem.setDisable(shown == null);
    enableDetour();
    drawRoadbook(shown, false);
  }

  /**
   * Draws, in the background, the roadbook of the route shown in the strip at the right of the map: its boxes, with
   * the changes of the user, the tulips of its junctions read on the map loaded, if any, drawn by the catalogue. The
   * drawing of a roadbook no longer shown is cancelled. For a new route, the roadbook drawn before is removed at once;
   * for a change of the user, it stays until the new one is drawn, and the box to select is selected then.
   *
   * @param shown the route shown, or {@code null} for none.
   * @param changed whether the route is the same, the changes of the user changed.
   */
  private void drawRoadbook(final Route shown, final boolean changed) {
    if (drawing != null) {
      drawing.cancel();
      drawing = null;
    }
    if (!changed) {
      toSelect = null;
      showRoadbook(List.of());
    }
    if (shown == null) {
      return;
    }
    final Router on = router;
    final RoadbookEdits drawn = edits;
    drawing = run(() -> {
      final Roadbook generated = generate(shown, drawn, on);
      return BoxRow.rows(generated, RoadbookEditor.roadbook(generated, drawn), limits(generated, on), CATALOGUE,
                         locale, texts);
    }, rows -> {
      drawing = null;
      showRoadbook(rows);
    }, failure -> {
      drawing = null;
      setStatus(ROADBOOK_ERROR);
    });
  }

  /**
   * Generates the roadbook of a route with the boxes added and removed by the user, the tulips of its junctions read
   * on the map, if any, and the notes of the features of the map along it (phase 5, increment 3b: a box of its own
   * for a feature far from the junctions, {@link FeatureNotes}); the corrections of the boxes are applied to it by
   * {@link RoadbookEditor#roadbook}.
   *
   * @param shown the route, as computed or opened.
   * @param changes the changes of the user.
   * @param on the router of the map, or {@code null} without map: the roadbook has no tulips and no notes.
   *
   * @return the roadbook generated from the route with its boxes added and removed.
   */
  static Roadbook generate(final Route shown, final RoadbookEdits changes, final Router on) {
    final Route changed = RoadbookEditor.route(shown, changes);
    if (on == null) {
      return RoadbookGenerator.generate(changed);
    }
    final List<RoadFeature> features = on.features(changed);
    final Route boxed = FeatureNotes.route(changed, features, changes.removed());
    return FeatureNotes.noted(RoadbookGenerator.generate(boxed, on.junctions(boxed)), features);
  }

  /**
   * Reads the speed limits of the map where the route of a roadbook leaves each of its boxes (phase 5, increment 5b:
   * an information for the user; decision of the maintainer, 2026-11-20).
   *
   * @param generated the roadbook generated: a box at the start, one at each maneuver of its route, one at the
   *                  finish.
   * @param on the router of the map, or {@code null} without map: no limit.
   *
   * @return the limits, one per box, in their order, in km/h; empty where the map gives none.
   */
  static List<OptionalInt> limits(final Roadbook generated, final Router on) {
    final Route boxed = generated.route();
    final List<Integer> places = new ArrayList<>();
    places.add(0);
    places.addAll(boxed.maneuvers());
    places.add(boxed.points().size() - 1);
    if (on == null) {
      return Collections.nCopies(places.size(), OptionalInt.empty());
    }
    return places.stream().map(index -> on.speedLimit(boxed, index)).toList();
  }

  /**
   * Shows the boxes of a roadbook in the strip, and their number; the box to select, if any, is selected, and the
   * strip scrolled to it.
   *
   * @param rows the boxes, empty for none.
   */
  private void showRoadbook(final List<BoxRow> rows) {
    final BoxRow before = boxList.getSelectionModel().getSelectedItem();
    redrawing = true;
    boxList.getItems().setAll(rows);
    roadbookCount.setText(new MessageFormat(texts.getString(ROADBOOK_COUNT), locale)
                              .format(new Object[] {rows.size()}));
    final GeoPoint place = toSelect;
    toSelect = null;
    final Optional<BoxRow> chosen = rows.stream().filter(row -> row.place().equals(place)).findFirst();
    chosen.ifPresent(row -> boxList.getSelectionModel().select(row));
    redrawing = false;
    chosen.ifPresent(row -> reveal(rows.indexOf(row)));
    final BoxRow after = boxList.getSelectionModel().getSelectedItem();
    final GeoPoint was = before == null ? null : before.place();
    final GeoPoint now = after == null ? null : after.place();
    if (!Objects.equals(was, now)) {
      showBox(after);
    }
  }

  /**
   * Scrolls the strip as little as needed for a box to be seen whole: a box selected by a change (a box added below
   * the boxes seen, found by the maintainer on 2026-11-15, was selected out of sight). The panel is laid out first,
   * with the editor of the box shown beside it.
   *
   * @param index the index of the box in the strip.
   */
  private void reveal(final int index) {
    roadbookPane.applyCss();
    roadbookPane.layout();
    ((VirtualFlow<?>) boxList.lookup(".virtual-flow")).scrollTo(index);
  }

  /**
   * Shows a box of the strip on the map, or hides the box shown; the boxes but the start and the finish can be
   * dragged along the route.
   *
   * @param row the box, or {@code null} for none.
   */
  private void showBox(final BoxRow row) {
    final int index = boxList.getItems().indexOf(row);
    map.setBox(row == null ? null : row.place(), index > 0 && index < boxList.getItems().size() - 1);
  }

  /**
   * Enables the generation of a detour while a route is shown and a graph is loaded.
   */
  private void enableDetour() {
    detourItem.setDisable(route == null || router == null);
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
    if (opened) {
      enableDetour();
    } else {
      next();
    }
  }

  /**
   * Imports an extract of OpenStreetMap chosen by the user, replacing the graph: the item File › Open a map….
   */
  @FXML
  private void openMap() {
    final Optional<Path> extract = services.chooser().apply(stage);
    if (extract.isEmpty()) {
      return;
    }
    busy = true;
    openMapItem.setDisable(true);
    cancelRouting();
    replaceRouter(null);
    enableDetour();
    showBusy(BUSY_IMPORTING, BUSY_IMPORT_HINT);
    setStatus(STATUS_IMPORTING);
    run(() -> services.importer().apply(extract.get(), graph()), this::ready, this::failed);
  }

  /**
   * Saves the roadbook of the route shown into its file, opened or saved last, without asking for it; the first time,
   * asks for it as Save the roadbook as…: the item File › Save the roadbook.
   */
  @FXML
  private void save() {
    if (file == null) {
      saveAs();
    } else {
      write(file);
    }
  }

  /**
   * Saves the roadbook of the route shown into the file chosen by the user, which becomes its file: the item File ›
   * Save the roadbook as….
   */
  @FXML
  private void saveAs() {
    services.saver().apply(stage).ifPresent(this::write);
  }

  /**
   * Exports the roadbook of the route shown as a plain GPX, into the file chosen by the user: the item File › Export as
   * GPX….
   */
  @FXML
  private void exportGpx() {
    services.exporter().apply(stage).ifPresent(this::writeGpx);
  }

  /**
   * Writes, in the background, the roadbook of the route shown as a plain GPX ({@link GpxWriter}): its boxes, with the
   * changes of the user, each a point named by its number and its total distance, and the track of its route. The
   * status bar then gives the number of boxes, or says that the file could not be written.
   *
   * @param target the file.
   */
  private void writeGpx(final Path target) {
    final Route shown = route;
    final RoadbookEdits drawn = edits;
    final Router exportedOn = router;
    final String notice = texts.getString(EXPORT_NOTICE);
    final String pattern = texts.getString(GPX_BOX);
    setStatus(STATUS_GPX_EXPORTING);
    run(() -> {
      final Roadbook roadbook = RoadbookEditor.roadbook(generate(shown, drawn, exportedOn), drawn);
      final NumberFormat kilometres = BoxRow.kilometres(locale);
      final MessageFormat names = new MessageFormat(pattern, locale);
      GpxWriter.save(roadbook, RoadbookFiles.name(target), notice,
                     box -> names.format(new Object[] {box.number(),
                                                       kilometres.format(box.total() / BoxRow.METRES_PER_KILOMETRE)}),
                     target);
      return roadbook;
    }, roadbook -> status.setText(new MessageFormat(texts.getString(STATUS_GPX_EXPORTED), locale)
                                      .format(new Object[] {roadbook.boxes().size()})),
        _ -> setStatus(GPX_ERROR));
  }

  /**
   * Exports the roadbook of the route shown as a PDF strip, into the file chosen by the user: the item File › Export
   * as PDF….
   */
  @FXML
  private void exportPdf() {
    services.printer().apply(stage).ifPresent(this::writePdf);
  }

  /**
   * Writes, in the background, the roadbook of the route shown as a PDF strip ({@link PdfWriter}): its boxes, with the
   * changes of the user. The status bar then gives the number of boxes and of pages, or says that the file could not
   * be written.
   *
   * @param target the file.
   */
  private void writePdf(final Path target) {
    final Route shown = route;
    final RoadbookEdits drawn = edits;
    final Router exportedOn = router;
    final String notice = texts.getString(EXPORT_NOTICE);
    final String heading = texts.getString(BoxRow.HEADING_TEXT);
    setStatus(STATUS_PDF_EXPORTING);
    run(() -> {
      final Roadbook roadbook = RoadbookEditor.roadbook(generate(shown, drawn, exportedOn), drawn);
      PdfWriter.save(roadbook, RoadbookFiles.name(target), notice, heading, locale, CATALOGUE, target);
      return roadbook;
    }, roadbook -> status.setText(new MessageFormat(texts.getString(STATUS_PDF_EXPORTED), locale)
                                      .format(new Object[] {roadbook.boxes().size(), PdfWriter.pages(roadbook)})),
        _ -> setStatus(PDF_ERROR));
  }

  /**
   * Writes, in the background, the roadbook of the route shown into a file, for Terra Pirata and to open it again: its
   * boxes, with the changes of the user, and its route as computed, the changes beside it ({@link OpenRallyWriter}),
   * so that a box added can be moved or removed once the roadbook is opened again. Once written, the file is the one
   * of the roadbook, the roadbook shown when it was asked is the one saved, and the status bar gives the number of
   * boxes; the application then closes if it was asked meanwhile. A failure says so in the status bar, and the
   * application does not close.
   *
   * @param target the file.
   */
  private void write(final Path target) {
    final Saved written = new Saved(route, origin, edits);
    final Router exportedOn = router;
    final String notice = texts.getString(EXPORT_NOTICE);
    setStatus(STATUS_EXPORTING);
    saving = run(() -> {
      final Roadbook roadbook = RoadbookEditor.roadbook(generate(written.route(), written.edits(), exportedOn),
                                                        written.edits());
      OpenRallyWriter.save(new Roadbook(written.route(), roadbook.boxes()), written.origin(), written.edits(),
                           RoadbookFiles.name(target), notice, CATALOGUE, target);
      return roadbook;
    }, roadbook -> {
      saving = null;
      file = target;
      saved = written;
      remember(target);
      updateTitle();
      status.setText(new MessageFormat(texts.getString(STATUS_EXPORTED), locale)
                         .format(new Object[] {roadbook.boxes().size()}));
      if (closing) {
        exit.run();
      }
    }, _ -> {
      saving = null;
      closing = false;
      setStatus(EXPORT_ERROR);
    });
  }

  /**
   * Tells whether the roadbook shown has changes not saved: it differs from the roadbook saved or opened last (another
   * route, other changes of the user); a roadbook never saved, only if the user changed it.
   *
   * @return {@code true} if closing would lose changes.
   */
  private boolean isModified() {
    if (route == null) {
      return false;
    }
    if (saved == null) {
      return !edits.isEmpty();
    }
    return saved.route() != route || !saved.edits().equals(edits);
  }

  /**
   * Writes the title of the window: the name of the file of the roadbook, and whether it is modified; the name of the
   * application alone while no roadbook was saved nor changed.
   */
  private void updateTitle() {
    final boolean modified = isModified();
    if (file == null && !modified) {
      stage.setTitle(texts.getString(TITLE));
      return;
    }
    final String name = file == null ? texts.getString(TITLE_UNTITLED) : RoadbookFiles.name(file);
    stage.setTitle(new MessageFormat(texts.getString(modified ? TITLE_MODIFIED : TITLE_DOCUMENT), locale)
                       .format(new Object[] {name}));
  }

  /**
   * Tells whether the application may close now: nothing is being saved, and nothing would be lost. Otherwise, it
   * closes once the roadbook being saved is saved; or the user is asked whether to save the changes first.
   *
   * @return {@code true} if the application may close now.
   */
  private boolean mayClose() {
    if (saving != null) {
      closing = true;
      setStatus(STATUS_CLOSING);
      return false;
    }
    if (!isModified()) {
      return true;
    }
    askToSave();
    return false;
  }

  /**
   * Asks the user, in a dialog over the window, whether to save the changes of the roadbook before closing: Save
   * saves it (into its file, or the file chosen), then closes; Don't Save closes; Cancel, or the dialog closed, keeps
   * the window open.
   */
  private void askToSave() {
    final ButtonType saveButton = new ButtonType(texts.getString(CLOSE_SAVE), ButtonBar.ButtonData.OK_DONE);
    final ButtonType discard = new ButtonType(texts.getString(CLOSE_DISCARD), ButtonBar.ButtonData.NO);
    final ButtonType cancel = new ButtonType(texts.getString(CLOSE_CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
    final Alert alert = new Alert(Alert.AlertType.CONFIRMATION, texts.getString(CLOSE_TEXT), saveButton, discard,
                                  cancel);
    alert.initOwner(stage);
    alert.setTitle(texts.getString(TITLE));
    alert.setHeaderText(texts.getString(CLOSE_QUESTION));
    alert.getDialogPane().getStylesheets().setAll(stage.getScene().getStylesheets());
    alert.setOnHidden(event -> {
      final ButtonType answer = alert.getResult();
      if (answer == discard) {
        exit.run();
      } else if (answer == saveButton) {
        final Optional<Path> target = file == null ? services.saver().apply(stage) : Optional.of(file);
        target.ifPresent(chosen -> {
          closing = true;
          write(chosen);
        });
      }
    });
    alert.show();
  }

  /**
   * Opens a roadbook exported by Box One, chosen by the user, and shows its route: the item File › Open a roadbook….
   */
  @FXML
  private void openRoadbook() {
    services.opener().apply(stage).ifPresent(this::open);
  }

  /**
   * Opens a roadbook opened or saved last, from File › Open a recent roadbook: a file that no longer exists (moved or
   * deleted) is removed from the list, and the user told so in a dialog.
   *
   * @param recent the file of the roadbook.
   */
  private void openRecent(final Path recent) {
    if (Files.isRegularFile(recent)) {
      open(recent);
      return;
    }
    save(settings.withoutRecent(recent));
    updateRecent();
    final Alert alert = new Alert(Alert.AlertType.INFORMATION, texts.getString(RECENT_MISSING_TEXT),
                                  new ButtonType(texts.getString(OPEN_CLOSE), ButtonBar.ButtonData.OK_DONE));
    alert.initOwner(stage);
    alert.setTitle(texts.getString(RoadbookFiles.OPEN_TITLE));
    alert.setHeaderText(new MessageFormat(texts.getString(RECENT_MISSING), locale)
                            .format(new Object[] {RoadbookFiles.name(recent)}));
    alert.getDialogPane().getStylesheets().setAll(stage.getScene().getStylesheets());
    alert.show();
  }

  /**
   * Fills File › Open a recent roadbook with the roadbooks opened or saved last, the last one first, each by the name
   * of its file and of its folder; the menu is disabled while there is none.
   */
  private void updateRecent() {
    final List<MenuItem> items = new ArrayList<>();
    for (final Path recent : settings.recent()) {
      final String folder = Optional.ofNullable(recent.toAbsolutePath().getParent()).map(Path::getFileName)
                                    .map(Path::toString).orElse("");
      final MenuItem item = new MenuItem(new MessageFormat(texts.getString(RECENT_ITEM), locale).format(
          new Object[] {RoadbookFiles.name(recent), folder}));
      item.setMnemonicParsing(false);
      item.setOnAction(event -> openRecent(recent));
      items.add(item);
    }
    recentMenu.getItems().setAll(items);
    recentMenu.setDisable(items.isEmpty());
  }

  /**
   * Puts a roadbook first in the roadbooks opened last, kept in the settings, and in File › Open a recent roadbook.
   *
   * @param roadbook the file of the roadbook, opened or saved.
   */
  private void remember(final Path roadbook) {
    save(settings.withRecent(roadbook));
    updateRecent();
  }

  /**
   * Opens a roadbook exported by Box One, in the background, and shows its route once read; a failure is shown in a
   * dialog.
   *
   * @param file the file of the roadbook.
   */
  private void open(final Path file) {
    cancelReading();
    final String before = status.getText();
    setStatus(STATUS_OPENING);
    reading = run(() -> checked(OpenRallyReader.read(file)), read -> opened(read, file), failure -> {
      reading = null;
      if (status.getText().equals(texts.getString(STATUS_OPENING))) {
        status.setText(before);
      }
      showReadError(failure);
    });
  }

  /**
   * Shows the failure to open a roadbook in a dialog over the window, where it is seen, the status bar being left as
   * it was before the opening, or as the work asked before set it meanwhile.
   *
   * @param failure the exception of the opening.
   */
  private void showReadError(final Throwable failure) {
    final Alert alert = new Alert(Alert.AlertType.ERROR, texts.getString(readError(failure)),
                                  new ButtonType(texts.getString(OPEN_CLOSE), ButtonBar.ButtonData.OK_DONE));
    alert.initOwner(stage);
    alert.setTitle(texts.getString(RoadbookFiles.OPEN_TITLE));
    alert.setHeaderText(texts.getString(OPEN_ERROR));
    alert.getDialogPane().getStylesheets().setAll(stage.getScene().getStylesheets());
    alert.show();
  }

  /**
   * Returns the key of the text of a failure to open a roadbook.
   *
   * @param failure the exception of the opening.
   *
   * @return {@code error.read.<code>} for a roadbook that cannot be read, {@value #UNEXPECTED} otherwise.
   */
  static String readError(final Throwable failure) {
    return failure instanceof final ReadException error ? READ_ERROR + error.getError().name() : UNEXPECTED;
  }

  /**
   * Checks the detour of a roadbook read back: its budget and its kinds of road must be ones of this version.
   *
   * @param file the roadbook read back.
   *
   * @return the roadbook.
   *
   * @throws ReadException if the budget or a kind of road of its detour is not known, or out of its bounds.
   */
  static RoadbookFile checked(final RoadbookFile file) throws ReadException {
    if (file.origin() instanceof final Origin.Detour detour) {
      try {
        budget(detour);
        kinds(detour);
      } catch (DetourException | IllegalArgumentException _) {
        throw new ReadException(ReadError.INVALID);
      }
    }
    return file;
  }

  /**
   * Returns the budget of the detour of a roadbook read back.
   *
   * @param detour the detour.
   *
   * @return its budget.
   *
   * @throws IllegalArgumentException if its unit is not one of {@link Budget.Unit}.
   * @throws DetourException if its amount is out of the bounds of its unit.
   */
  static Budget budget(final Origin.Detour detour) {
    return new Budget(Budget.Unit.valueOf(detour.unit()), detour.amount());
  }

  /**
   * Returns the kinds of road the detour of a roadbook read back avoids.
   *
   * @param detour the detour.
   *
   * @return the kinds of road.
   *
   * @throws IllegalArgumentException if a kind is not one of {@link RoadKind}.
   */
  static Set<RoadKind> kinds(final Origin.Detour detour) {
    final Set<RoadKind> kinds = EnumSet.noneOf(RoadKind.class);
    detour.avoided().forEach(name -> kinds.add(RoadKind.valueOf(name)));
    return kinds;
  }

  /**
   * Shows a roadbook opened: its start and finish, its route (without the boxes added, which a roadbook saved before
   * 2026-11-15 holds in it), and for a detour its seed and its budget, proposed for the next detour.
   *
   * @param read the roadbook read back, its detour checked.
   * @param path its file, which becomes the file of the roadbook.
   */
  private void opened(final RoadbookFile read, final Path path) {
    reading = null;
    cancelRouting();
    history.remember(current());
    final Origin from = read.origin();
    final View view = new View(from.start(), from.finish(), null,
                               RoadbookEditor.withoutAdded(read.route(), read.edits()), from, true, read.edits());
    file = path;
    saved = new Saved(view.route(), view.origin(), view.edits());
    remember(path);
    display(view);
  }

  /**
   * Asks for the budget and the seed of a detour, and generates it when asked: the item Route › Generate a detour….
   */
  @FXML
  private void generateDetour() {
    final long newSeed = DetourGenerator.newSeed(RandomGenerator.getDefault());
    final DetourDialog.Request proposed = new DetourDialog.Request(budget, settings.avoided(), newSeed);
    final Dialog<DetourDialog.Request> dialog = new DetourDialog(stage, texts, locale, proposed, fastest,
                                                                 stage.getScene().getStylesheets()).getDialog();
    dialog.setOnHidden(event -> {
      if (dialog.getResult() != null) {
        detour(dialog.getResult());
      }
    });
    dialog.show();
  }

  /**
   * Generates, in the background, a detour from the start to the finish shown, a detour being generated cancelled; the
   * kinds of road forbidden, when they change, are kept in the settings for the next detours (issue #11 of GitLab).
   *
   * @param request the budget, the kinds of road forbidden and the seed of the detour.
   */
  private void detour(final DetourDialog.Request request) {
    budget = request.budget();
    if (!request.avoided().equals(settings.avoided())) {
      save(settings.withAvoided(request.avoided()));
    }
    generate(request, this::failed);
  }

  /**
   * Generates, in the background, a detour from the start to the finish shown, a detour being generated cancelled.
   *
   * @param request the budget, the kinds of road forbidden and the seed of the detour.
   * @param failed receives the failure of the generation, on the JavaFX thread.
   */
  private void generate(final DetourDialog.Request request, final Consumer<Throwable> failed) {
    cancelRouting();
    final DetourGenerator generator = new DetourGenerator(router);
    final GeoPoint start = itinerary.getStart().orElseThrow();
    final GeoPoint finish = itinerary.getFinish().orElseThrow();
    setStatus(STATUS_DETOURING);
    routing = run(() -> generator.generate(start, finish, request.budget(), request.avoided(), request.seed()),
                  this::detoured, failed);
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
    if (save(saved)) {
      map.setTiles(saved.source());
      map.setZoom(saved.zoom());
    }
  }

  /**
   * Saves new settings, and keeps them once saved; a failure is told in the status bar.
   *
   * @param chosen the new settings.
   *
   * @return {@code true} if they are saved, {@code false} if the file cannot be written: the former settings are kept.
   */
  private boolean save(final Settings chosen) {
    try {
      store.save(chosen);
    } catch (UncheckedIOException _) {
      setStatus(SETTINGS_ERROR);
      return false;
    }
    settings = chosen;
    return true;
  }

  /**
   * Quits the application, unless the roadbook is being saved (it quits once saved) or has changes not saved (the
   * user is asked first): the item Quit of the File menu.
   */
  @FXML
  private void quit() {
    if (mayClose()) {
      exit.run();
    }
  }

  /**
   * Cancels the route being computed and the roadbook being opened, if any: their result is not shown.
   */
  private void cancelRouting() {
    if (routing != null) {
      routing.cancel();
      routing = null;
    }
    cancelReading();
  }

  /**
   * Cancels the roadbook being opened, if any: it is not shown.
   */
  private void cancelReading() {
    if (reading != null) {
      reading.cancel();
      reading = null;
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
    setStatus(error(failure));
  }

  /**
   * Returns the key of the message of a failure of the work in the background.
   *
   * @param failure the failure.
   *
   * @return the key of its message: the code of a failure of the routing or of the detour, unexpected otherwise.
   */
  private static String error(final Throwable failure) {
    return switch (failure) {
      case final RoutingException error -> ROUTING_ERROR + error.getError().name();
      case final DetourException error -> DETOUR_ERROR + error.getError().name();
      default -> UNEXPECTED;
    };
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

  /**
   * A state of the window brought back by Undo and Redo (issue #4 of GitLab): the start, the finish, the route shown
   * and the changes of the user to its roadbook. Immutable; its places are personal data, never logged.
   *
   * @param start the start, or {@code null}.
   * @param finish the finish, or {@code null}.
   * @param under the fastest route drawn under the detour, or {@code null}.
   * @param route the route shown, the fastest one or a detour, or {@code null} for none: it is computed again when
   *              the state is shown, if the start and the finish are placed.
   * @param origin where the route comes from, or {@code null} without route.
   * @param opened whether the route was opened from a roadbook.
   * @param edits the changes of the user to the roadbook of the route, or to the next route computed.
   */
  private record View(GeoPoint start, GeoPoint finish, Route under, Route route, Origin origin, boolean opened,
                      RoadbookEdits edits) {
  }

  /**
   * The roadbook as it was saved or opened: its route, compared by identity (a route computed again is another
   * route; its origin goes with it), its origin, and the changes of the user. Immutable; its places are personal
   * data, never logged.
   *
   * @param route the route.
   * @param origin where the route comes from.
   * @param edits the changes of the user.
   */
  private record Saved(Route route, Origin origin, RoadbookEdits edits) {
  }

}
