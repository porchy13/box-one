package ch.varani.boxone.ui;

import javafx.scene.control.MenuItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of {@link RoadbookItems}: the items of the roadbook enabled when their own state allows it and the roadbook is
 * shown, greyed out otherwise.
 */
@ExtendWith(ApplicationExtension.class)
final class RoadbookItemsTest {

  /**
   * The items keep their own state while the roadbook is hidden, greyed out, and get it back when it is shown again;
   * an item changed while hidden stays greyed out until then.
   */
  @Test
  @DisplayName("Welcome: the items of the roadbook greyed out while it is hidden, their own state kept")
  void showTest() {
    final MenuItem open = new MenuItem();
    final MenuItem save = new MenuItem();
    save.setDisable(true);
    final RoadbookItems items = new RoadbookItems(List.of(open, save));
    items.show(false);
    final boolean openGreyed = open.isDisable();
    items.enable(save, true);
    final boolean saveGreyed = save.isDisable();
    items.show(true);
    final boolean openEnabled = !open.isDisable();
    final boolean saveEnabled = !save.isDisable();
    items.enable(open, false);
    assertAll(
        () -> assertTrue(openGreyed),
        () -> assertTrue(saveGreyed),
        () -> assertTrue(openEnabled),
        () -> assertTrue(saveEnabled),
        () -> assertTrue(open.isDisable()),
        () -> assertFalse(save.isDisable()));
  }

  /**
   * An item that does not act on the roadbook is refused.
   */
  @Test
  @DisplayName("Welcome: an item that does not act on the roadbook refused")
  void unknownTest() {
    final RoadbookItems items = new RoadbookItems(List.of(new MenuItem()));
    final MenuItem other = new MenuItem();
    assertThrows(IllegalArgumentException.class, () -> items.enable(other, true));
  }

}
