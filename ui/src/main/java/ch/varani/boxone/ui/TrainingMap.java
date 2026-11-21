package ch.varani.boxone.ui;

import ch.varani.boxone.core.GeoPoint;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The map lent to the reading of boxes (development plan, phase 7b; decision of the maintainer, 2026-11-20: the map
 * centred on the junction, the route hidden): the view of the map taken out of its pane of the main window and laid
 * in the page of the question, the junction shown alone, its clicks given to the question; then given back.
 *
 * <p><b>Sequence</b>: {@link #show(GeoPoint, List, Consumer)} shows a junction and the route arriving at it, and
 * listens to the clicks (the main window hands them over, {@link #click(GeoPoint)}); {@link #showPath(List)} draws the
 * route through it once answered; {@link #release()} puts the view back first in its pane, the start, the finish and
 * the routes drawn again, the clicks back to the main window. The view of the map moves in the scene without loading
 * its page again.</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class TrainingMap {

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The map of the main window. */
  private final MapView map;
  /** The pane of the map in the main window, where its view goes back. */
  private final Pane home;
  /** Receives the clicks on the map while a junction is shown, or {@code null}. */
  private Consumer<GeoPoint> clicks;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the map lent to the training.
   *
   * @param map the map of the main window.
   * @param home the pane of the map, its view first in it.
   */
  TrainingMap(final MapView map, final Pane home) {
    this.map = map;
    this.home = home;
  }

  // *******************************************************************************************************************
  // Getters & Setters
  // *******************************************************************************************************************

  /**
   * Gives the view of the map, to lay in the page of a question.
   *
   * @return the view.
   */
  Node getNode() {
    return map.getNode();
  }

  /**
   * Gives the area of the map seen, where the routes of the reading are drawn.
   *
   * @return its south-west and its north-east corners, or empty before the page of the map has loaded.
   */
  Optional<List<GeoPoint>> getArea() {
    return map.getArea();
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Shows a junction alone with the route arriving at it, and listens to the clicks on the map.
   *
   * @param place the place of the junction.
   * @param arrival the points of the route arriving at the junction.
   * @param listener receives the places clicked.
   */
  void show(final GeoPoint place, final List<GeoPoint> arrival, final Consumer<GeoPoint> listener) {
    clicks = listener;
    map.showJunction(place, arrival);
  }

  /**
   * Draws the route through the junction shown.
   *
   * @param path the points of the route around the junction.
   */
  void showPath(final List<GeoPoint> path) {
    map.showPath(path);
  }

  /**
   * Gives a click on the map to the junction shown, if any.
   *
   * @param place the place clicked.
   *
   * @return {@code true} if a junction is shown and takes the click: the main window then ignores it.
   */
  boolean click(final GeoPoint place) {
    if (clicks == null) {
      return false;
    }
    clicks.accept(place);
    return true;
  }

  /**
   * Gives the map back to the main window: its view first in its pane, the start, the finish and the routes drawn
   * again, the clicks back to the main window. Nothing changes if the map is not lent.
   */
  void release() {
    if (clicks == null) {
      return;
    }
    clicks = null;
    home.getChildren().addFirst(map.getNode());
    map.endTraining();
  }

}
