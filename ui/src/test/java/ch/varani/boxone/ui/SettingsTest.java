package ch.varani.boxone.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of {@link Settings}: the roadbooks opened last, the last one first and each once, as many as the settings
 * keep, from 5 to 20; a roadbook removed from them; the other settings kept.
 */
final class SettingsTest {

  /** A roadbook. */
  private static final Path FIRST = Path.of("first.gpx");
  /** Another roadbook. */
  private static final Path SECOND = Path.of("second.gpx");
  /** More roadbooks than the settings can keep. */
  private static final int MANY = 30;

  /**
   * A roadbook opened goes first, once; beyond the number kept, the oldest one is dropped; a roadbook removed is no
   * longer in the list; the kinds of road forbidden keep the list.
   */
  @Test
  @DisplayName("Recent roadbooks: the last one first, each once, as many as kept, a roadbook removed dropped")
  void recentTest() {
    final Settings opened = Settings.DEFAULT.withRecent(FIRST).withRecent(SECOND).withRecent(FIRST);
    Settings full = Settings.DEFAULT;
    for (int i = 0; i < Settings.MIN_RECENT + 1; i++) {
      full = full.withRecent(Path.of(i + ".gpx"));
    }
    final Settings kept = full;
    assertAll(
        () -> assertEquals(List.of(FIRST, SECOND), opened.recent()),
        () -> assertEquals(List.of(SECOND), opened.withoutRecent(FIRST).recent()),
        () -> assertEquals(Settings.MIN_RECENT, kept.recent().size()),
        () -> assertEquals(Path.of(Settings.MIN_RECENT + ".gpx"), kept.recent().getFirst()),
        () -> assertEquals(List.of(FIRST, SECOND), opened.withAvoided(Set.of()).recent()));
  }

  /**
   * The number of roadbooks kept is 5 at least and 20 at most, and the list is cut to it.
   */
  @Test
  @DisplayName("Recent roadbooks: 5 kept by default and at least, 20 at most")
  void countTest() {
    final List<Path> many = IntStream.range(0, MANY).mapToObj(i -> Path.of(i + ".gpx")).toList();
    final Settings most = new Settings(TileProvider.SWISSTOPO, "", ZoomSensitivity.MEDIUM, Set.of(), MANY, many);
    final Settings least = new Settings(TileProvider.SWISSTOPO, "", ZoomSensitivity.MEDIUM, Set.of(), 1, many);
    assertAll(
        () -> assertEquals(Settings.MIN_RECENT, Settings.DEFAULT.recentCount()),
        () -> assertEquals(Settings.MAX_RECENT, most.recentCount()),
        () -> assertEquals(Settings.MAX_RECENT, most.recent().size()),
        () -> assertEquals(Settings.MIN_RECENT, least.recentCount()),
        () -> assertEquals(many.subList(0, Settings.MIN_RECENT), least.recent()));
  }

}
