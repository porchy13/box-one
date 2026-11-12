package ch.varani.boxone.ui;

import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Helpers of the tests of the page of the map: wait for the page, run a script in it, click on it.
 *
 * <p>Utility class without state.</p>
 */
final class MapPages {

  /** The longest wait for the page, in seconds. */
  private static final long TIMEOUT = 30;

  /**
   * Prevents the instantiation of this utility class.
   */
  private MapPages() {
  }

  /**
   * Waits until the page of a view has loaded.
   *
   * @param view the view of the page.
   *
   * @throws TimeoutException if the page does not load in time.
   */
  static void waitLoaded(final WebView view) throws TimeoutException {
    WaitForAsyncUtils.waitFor(TIMEOUT, TimeUnit.SECONDS, () -> WaitForAsyncUtils.asyncFx(
        () -> view.getEngine().getLoadWorker().getState() == Worker.State.SUCCEEDED).get());
    WaitForAsyncUtils.waitForFxEvents();
  }

  /**
   * Runs a script in the page, on the JavaFX Application Thread.
   *
   * @param robot the TestFX robot.
   * @param view the view of the page.
   * @param script the script.
   *
   * @return the value of the script.
   */
  static Object script(final FxRobot robot, final WebView view, final String script) {
    final AtomicReference<Object> value = new AtomicReference<>();
    robot.interact(() -> value.set(view.getEngine().executeScript(script)));
    return value.get();
  }

  /**
   * Clicks on the map, through Leaflet, at a place.
   *
   * @param robot the TestFX robot.
   * @param view the view of the page.
   * @param latitude the latitude of the place.
   * @param longitude the longitude of the place.
   */
  static void click(final FxRobot robot, final WebView view, final double latitude, final double longitude) {
    script(robot, view, String.format(Locale.ROOT, "boxOne.map.fire('click', {latlng: L.latLng(%s, %s)})", latitude,
                                      longitude));
  }

  /**
   * Drags the marker of the start or of the finish to a place, and drops it there, as Leaflet does.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map, loaded.
   * @param kind the kind of the marker: {@code start} or {@code finish}.
   * @param latitude the latitude of the place, in degrees.
   * @param longitude the longitude of the place, in degrees.
   */
  static void drag(final FxRobot robot, final WebView view, final String kind, final double latitude,
                   final double longitude) {
    script(robot, view, String.format(Locale.ROOT, "boxOne.map.eachLayer(function (layer) {"
                                                   + " if (layer.options.kind === '%s') {"
                                                   + " layer.setLatLng(L.latLng(%s, %s)); layer.fire('dragend'); } })",
                                      kind, latitude, longitude));
  }

  /**
   * Asks for the context menu of a place of the map, as a right-click does.
   *
   * @param robot the TestFX robot.
   * @param view the view of the map, loaded.
   * @param latitude the latitude of the place, in degrees.
   * @param longitude the longitude of the place, in degrees.
   */
  static void contextMenu(final FxRobot robot, final WebView view, final double latitude, final double longitude) {
    script(robot, view, String.format(Locale.ROOT, "boxOne.map.fire('contextmenu', {latlng: L.latLng(%s, %s),"
                                                   + " containerPoint: L.point(10, 20)})", latitude, longitude));
  }

  /**
   * Counts the elements of the page matching a selector.
   *
   * @param robot the TestFX robot.
   * @param view the view of the page.
   * @param selector the CSS selector.
   *
   * @return the number of elements.
   */
  static int count(final FxRobot robot, final WebView view, final String selector) {
    return ((Number) script(robot, view, "document.querySelectorAll('" + selector + "').length")).intValue();
  }

}
