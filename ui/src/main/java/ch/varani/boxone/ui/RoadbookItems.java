package ch.varani.boxone.ui;

import javafx.scene.control.MenuItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The items of the menus that act on the roadbook (open a map, open a roadbook, the recent roadbooks, save, export,
 * undo and redo, generate a detour): enabled only while the roadbook editor is shown, greyed out on the welcome page
 * and in the training (asked by the maintainer on 2026-11-21: on macOS, the global menu bar stays the one of the
 * window while the welcome page is shown, and these items acted on the roadbook hidden behind it).
 *
 * <p>Each item keeps its own state besides (no route to save, nothing to undo, an import in progress…), given by
 * {@link #enable}: an item is enabled when its own state allows it and the roadbook is shown. Their own state is at
 * first the one of the view (the {@code disable} attributes of {@code main.fxml}).</p>
 *
 * <p>Confined to the JavaFX Application Thread.</p>
 */
final class RoadbookItems {

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** Whether each item may be used for its own reasons, in the order of the menus. */
  private final Map<MenuItem, Boolean> usable = new LinkedHashMap<>();
  /** Whether the roadbook editor is shown. */
  private boolean shown = true;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Takes the items, their own state the one they have, the roadbook shown.
   *
   * @param items the items that act on the roadbook.
   */
  RoadbookItems(final List<MenuItem> items) {
    items.forEach(item -> usable.put(item, !item.isDisable()));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Sets the own state of an item, and enables it if the roadbook is shown too.
   *
   * @param item one of the items.
   * @param enabled whether it may be used for its own reasons.
   *
   * @throws IllegalArgumentException if it is not one of the items.
   */
  void enable(final MenuItem item, final boolean enabled) {
    if (!usable.containsKey(item)) {
      throw new IllegalArgumentException("not an item of the roadbook");
    }
    usable.put(item, enabled);
    item.setDisable(!(enabled && shown));
  }

  /**
   * Tells whether the roadbook editor is shown, and enables or greys out the items accordingly.
   *
   * @param roadbook {@code true} while the roadbook editor is shown, {@code false} on the welcome page and in the
   *                 training.
   */
  void show(final boolean roadbook) {
    shown = roadbook;
    usable.forEach((item, enabled) -> item.setDisable(!(enabled && roadbook)));
  }

}
