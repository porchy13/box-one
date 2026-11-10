package ch.varani.boxone.ui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests of {@link SettingsDialog} (DESIGN.md §7): the field of the key is enabled for MapTiler only, Save is disabled
 * while MapTiler has no key, and the key saved is stripped of its spaces.
 */
@ExtendWith(ApplicationExtension.class)
final class SettingsDialogTest {

  /**
   * The rules of the fields, as the user changes the provider, types the key and chooses the sensitivity of the zoom.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: MapTiler cannot be saved without its key, which is stripped of its spaces")
  void rulesTest(final FxRobot robot) {
    final List<Boolean> keyDisabled = new ArrayList<>();
    final List<Boolean> saveDisabled = new ArrayList<>();
    final AtomicReference<Settings> result = new AtomicReference<>();
    robot.interact(() -> {
      final Stage owner = new Stage();
      owner.setScene(new Scene(new StackPane()));
      owner.show();
      final Dialog<Settings> dialog = new SettingsDialog(owner, Languages.bundle(Languages.FRENCH), Settings.DEFAULT,
                                                         List.of()).getDialog();
      dialog.show();
      @SuppressWarnings("unchecked")
      final ComboBox<String> tiles = (ComboBox<String>) dialog.getDialogPane().lookup("#" + SettingsDialog.TILES_ID);
      @SuppressWarnings("unchecked")
      final ComboBox<String> zoom = (ComboBox<String>) dialog.getDialogPane().lookup("#" + SettingsDialog.ZOOM_ID);
      final PasswordField key = (PasswordField) dialog.getDialogPane().lookup(".password-field");
      final Node save = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().stream()
          .filter(type -> type.getButtonData() == ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());
      keyDisabled.add(key.isDisable());
      saveDisabled.add(save.isDisable());
      tiles.getSelectionModel().select(1);
      keyDisabled.add(key.isDisable());
      saveDisabled.add(save.isDisable());
      key.setText("  my-key ");
      zoom.getSelectionModel().select(0);
      saveDisabled.add(save.isDisable());
      dialog.setResult(dialog.getResultConverter().call(dialog.getDialogPane().getButtonTypes().getFirst()));
      result.set(dialog.getResult());
      dialog.close();
    });
    assertAll(
        () -> assertEquals(List.of(true, false), keyDisabled),
        () -> assertEquals(List.of(false, true, false), saveDisabled),
        () -> assertEquals(new Settings(TileProvider.MAPTILER, "my-key", ZoomSensitivity.LOW), result.get()));
  }

  /**
   * The dialog shows the current settings, with the names of the providers.
   *
   * @param robot the TestFX robot.
   */
  @Test
  @DisplayName("Phase 1: the settings show the current provider and key")
  void currentTest(final FxRobot robot) {
    final AtomicReference<Dialog<Settings>> shown = new AtomicReference<>();
    robot.interact(() -> {
      final Stage owner = new Stage();
      owner.setScene(new Scene(new StackPane()));
      owner.show();
      shown.set(new SettingsDialog(owner, Languages.bundle(Languages.FRENCH),
                                   new Settings(TileProvider.MAPTILER, "kept", ZoomSensitivity.VERY_HIGH),
                                   List.of()).getDialog());
    });
    final Dialog<Settings> dialog = shown.get();
    @SuppressWarnings("unchecked")
    final ComboBox<String> tiles = (ComboBox<String>) dialog.getDialogPane().lookup("#" + SettingsDialog.TILES_ID);
    @SuppressWarnings("unchecked")
    final ComboBox<String> zoom = (ComboBox<String>) dialog.getDialogPane().lookup("#" + SettingsDialog.ZOOM_ID);
    final PasswordField key = (PasswordField) dialog.getDialogPane().lookup(".password-field");
    final ButtonType cancel = dialog.getDialogPane().getButtonTypes().getLast();
    assertAll(
        () -> assertEquals("Réglages", dialog.getTitle()),
        () -> assertEquals(List.of("Cartes nationales (swisstopo)", "MapTiler (OpenStreetMap)"), tiles.getItems()),
        () -> assertEquals(1, tiles.getSelectionModel().getSelectedIndex()),
        () -> assertEquals("kept", key.getText()),
        () -> assertEquals(List.of("Faible", "Moyenne", "Forte", "Très forte"), zoom.getItems()),
        () -> assertEquals(ZoomSensitivity.VERY_HIGH.ordinal(), zoom.getSelectionModel().getSelectedIndex()),
        () -> assertFalse(key.isDisable()),
        () -> assertNull(dialog.getResultConverter().call(cancel)));
  }

}
