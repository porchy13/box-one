package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.export.OpenRallyReader;
import ch.varani.boxone.export.OpenRallyWriter;
import ch.varani.boxone.export.Origin;
import ch.varani.boxone.export.ReadException;
import ch.varani.boxone.roadbook.RoadbookGenerator;
import ch.varani.boxone.routing.GraphHopperRouter;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * The smoke test of the packaged application (AGENTS.md §7), run by {@code Launcher --smoke-test} on each installer
 * built: it checks that the parts that need modules, native code or resources of the package work, without showing a
 * window.
 *
 * <p><b>Steps</b>: the texts of the interface are loaded in every language (the bundles are in the package), the
 * terms of use of the start included, once the toolkit runs (the smoke test never shows them: it does not start the
 * application); the
 * routing works: the small extract of OpenStreetMap {@value #EXTRACT} (the centre of Yverdon-les-Bains, © OpenStreetMap
 * contributors, ODbL) is imported by GraphHopper into a temporary folder (the regrouped module of GraphHopper, its
 * libraries, the compilation of its custom models), and a route joins the station and the castle; its roadbook is
 * generated and written for Terra Pirata, then read back (the writer and the parser of XML of the JDK, linked into the
 * runtime; issue #5 of GitLab); the JavaFX toolkit
 * is started (its native libraries), the map ({@link MapView}) loads its page (the native library of WebKit, Leaflet)
 * and a click on it and the marker of the start dropped reach the application (the bridge, called by WebKit across the
 * modules; issue #4 of GitLab), then the toolkit is
 * stopped. A failing step throws an exception, so that the launcher ends with an error;
 * otherwise the launcher ends normally. Each step of a later phase that needs native code or a resource (fonts of the
 * PDF…) adds its check here.</p>
 *
 * <p>Utility class without state; run once per process, as the JavaFX toolkit can start only once.</p>
 */
final class SmokeTest {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The argument of the launcher that runs the smoke test. */
  static final String ARGUMENT = "--smoke-test";
  /** The small extract of OpenStreetMap of the smoke test, in this package. */
  static final String EXTRACT = "smoke/yverdon-centre.osm.gz";
  /** The start of the route of the smoke test: the station of Yverdon-les-Bains. */
  static final GeoPoint STATION = new GeoPoint(46.7813, 6.6410);
  /** The finish of the route of the smoke test: the castle of Yverdon-les-Bains. */
  static final GeoPoint CASTLE = new GeoPoint(46.7784, 6.6413);
  /** A click on the map, at the station, run in the page once loaded. */
  static final String CLICK = "boxOne.map.fire('click', {latlng: L.latLng(46.7813, 6.6410)})";
  /** The marker of the start dropped at the castle, run in the page once loaded (issue #4 of GitLab). */
  static final String DROP = "boxOne.map.eachLayer(function (layer) { if (layer.options.kind === 'start') {"
                             + " layer.setLatLng(L.latLng(46.7784, 6.6413)); layer.fire('dragend'); } })";
  /** The beginning of the name of the temporary folder of the smoke test, in the data folder. */
  static final String SMOKE_FOLDER = "smoke-test";
  /** The longest wait for the page of the map, its click and its marker dropped, in seconds. */
  static final long PAGE_TIMEOUT = 30;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private SmokeTest() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Runs the smoke test.
   *
   * @throws java.util.MissingResourceException if the texts of a language are missing.
   * @throws IOException if the extract cannot be copied into the temporary folder.
   * @throws InterruptedException if the wait for the JavaFX toolkit or for the page is interrupted.
   * @throws ch.varani.boxone.routing.RoutingException if the extract cannot be imported or the route computed.
   * @throws IllegalStateException if the page of the map does not load.
   */
  static void run() throws IOException, InterruptedException {
    for (final Locale locale : Languages.ALL) {
      Languages.bundle(locale).getString(MainController.TITLE);
    }
    route();
    final CountDownLatch started = new CountDownLatch(1);
    Platform.startup(started::countDown);
    started.await();
    // The terms of use are checked once the toolkit runs, as in the application: their class uses JavaFX controls.
    for (final Locale locale : Languages.ALL) {
      StartupNotice.PARAGRAPHS.forEach(Languages.bundle(locale)::getString);
    }
    final CountDownLatch events = new CountDownLatch(2);
    // The page holds its bridge by a weak reference only: the map is kept until its events, or a collection drops it.
    final AtomicReference<MapView> kept = new AtomicReference<>();
    Platform.runLater(() -> {
      final MapView map = new MapView(Languages.bundle(Languages.ENGLISH), place -> events.countDown(),
                                       (end, place) -> events.countDown());
      kept.set(map);
      map.setPlaces(STATION, null);
      map.setOnLoaded(() -> {
        map.getNode().getEngine().executeScript(CLICK);
        map.getNode().getEngine().executeScript(DROP);
      });
    });
    final boolean reached = events.await(PAGE_TIMEOUT, TimeUnit.SECONDS);
    Reference.reachabilityFence(kept);
    Platform.exit();
    check(reached, "The page of the map did not load, or its clicks and its markers do not reach the application");
  }

  /**
   * Imports the extract of the smoke test into a temporary folder, computes a route on it, writes its roadbook into
   * the folder and reads it back (issue #5 of GitLab: the parser of XML of the JDK); the folder is deleted
   * afterwards. The folder is made in the data folder of the user ({@link DataFolder}), private to the user, rather
   * than in the shared temporary folder of the system.
   *
   * @throws IOException if the extract cannot be copied into the temporary folder, or the roadbook written.
   * @throws IllegalStateException if the roadbook written cannot be read back.
   * @throws ch.varani.boxone.routing.RoutingException if the extract cannot be imported or the route computed.
   */
  private static void route() throws IOException {
    final Path data = DataFolder.of(System.getProperty("os.name"), System.getenv(), System.getProperty("user.home"));
    final Path folder = Files.createTempDirectory(Files.createDirectories(data), SMOKE_FOLDER);
    try {
      final Path extract = folder.resolve("extract.osm.gz");
      try (InputStream input = SmokeTest.class.getResourceAsStream(EXTRACT)) {
        Files.copy(input, extract);
      }
      final Origin origin = new Origin.Fastest(STATION, CASTLE);
      final Path roadbook = folder.resolve("roadbook.gpx");
      try (GraphHopperRouter router = GraphHopperRouter.importMap(extract, folder.resolve(DataFolder.GRAPH))) {
        OpenRallyWriter.save(RoadbookGenerator.generate(router.route(STATION, CASTLE)), origin, SMOKE_FOLDER,
                             Languages.bundle(Languages.ENGLISH).getString(MainController.EXPORT_NOTICE), roadbook);
      }
      check(readsBack(roadbook, origin), "The roadbook exported cannot be opened again");
    } finally {
      try (Stream<Path> files = Files.walk(folder)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
  }

  /**
   * Tells whether a roadbook written can be read back, with its origin.
   *
   * @param roadbook the file of the roadbook.
   * @param origin the origin written into it.
   *
   * @return {@code true} if the file is read back with this origin, {@code false} if it cannot be read or holds
   *         another one.
   */
  static boolean readsBack(final Path roadbook, final Origin origin) {
    try {
      return origin.equals(OpenRallyReader.read(roadbook).origin());
    } catch (ReadException _) {
      return false;
    }
  }

  /**
   * Checks a condition of the smoke test.
   *
   * @param condition the condition.
   * @param failure the message of the failure.
   *
   * @throws IllegalStateException if the condition is false.
   */
  static void check(final boolean condition, final String failure) {
    if (!condition) {
      throw new IllegalStateException(failure);
    }
  }

}
