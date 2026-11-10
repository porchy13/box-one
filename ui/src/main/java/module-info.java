/**
 * The user interface of Box One, written with JavaFX: the main window with the map and, phase after phase, the
 * roadbook, its editor and the training mode.
 *
 * <p>The business logic lives in the other modules ({@code ch.varani.boxone.core}, {@code ch.varani.boxone.routing}…),
 * without any dependency on JavaFX (AGENTS.md §3); this module only shows it. Its views are FXML files read by
 * {@code javafx.fxml}; the map is a page of Leaflet in a {@code WebView} ({@code javafx.web}), whose engine calls the
 * bridge of the clicks by reflection: the package {@code ch.varani.boxone.ui.map} holds it alone, open to
 * {@code javafx.web} only.</p>
 */
module ch.varani.boxone.ui {
  requires ch.varani.boxone.core;
  requires ch.varani.boxone.routing;
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.web;
  requires jdk.jsobject;

  exports ch.varani.boxone.ui to javafx.graphics;
  opens ch.varani.boxone.ui to javafx.fxml;
  opens ch.varani.boxone.ui.map to javafx.web;
}
