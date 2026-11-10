package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import ch.varani.boxone.routing.GraphHopperRouter;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * The smoke test of the packaged application (AGENTS.md §7), run by {@code Launcher --smoke-test} on each installer
 * built: it checks that the parts that need modules, native code or resources of the package work, without showing a
 * window.
 *
 * <p><b>Steps</b>: the texts of the interface are loaded in every language (the bundles are in the package); the
 * routing works: the small extract of OpenStreetMap {@value #EXTRACT} (the centre of Yverdon-les-Bains, © OpenStreetMap
 * contributors, ODbL) is imported by GraphHopper into a temporary folder (the regrouped module of GraphHopper, its
 * libraries, the compilation of its custom models), and a route joins the station and the castle; the JavaFX toolkit
 * is started (its native libraries), the map ({@link MapView}) loads its page (the native library of WebKit, Leaflet)
 * and a click on it reaches the application (the bridge, called by WebKit across the modules), then the toolkit is
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
  /** The beginning of the name of the temporary folder of the smoke test, in the data folder. */
  static final String SMOKE_FOLDER = "smoke-test";
  /** The longest wait for the page of the map and its click, in seconds. */
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
    final CountDownLatch clicked = new CountDownLatch(1);
    Platform.runLater(() -> {
      final MapView map = new MapView(Languages.bundle(Languages.ENGLISH), place -> clicked.countDown());
      map.setOnLoaded(() -> map.getNode().getEngine().executeScript(CLICK));
    });
    final boolean reached = clicked.await(PAGE_TIMEOUT, TimeUnit.SECONDS);
    Platform.exit();
    check(reached, "The page of the map did not load, or its clicks do not reach the application");
  }

  /**
   * Imports the extract of the smoke test into a temporary folder and computes a route on it; the folder is deleted
   * afterwards. The folder is made in the data folder of the user ({@link DataFolder}), private to the user, rather
   * than in the shared temporary folder of the system.
   *
   * @throws IOException if the extract cannot be copied into the temporary folder.
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
      try (GraphHopperRouter router = GraphHopperRouter.importMap(extract, folder.resolve(DataFolder.GRAPH))) {
        Objects.requireNonNull(router.route(STATION, CASTLE));
      }
    } finally {
      try (Stream<Path> files = Files.walk(folder)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
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
