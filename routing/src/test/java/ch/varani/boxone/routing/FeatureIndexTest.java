package ch.varani.boxone.routing;

import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link FeatureIndex} (development plan, phase 5, increment 3b): the places of the features of the map, kept
 * once, found within a metre, saved and read back; a file that is not valid is refused.
 */
final class FeatureIndexTest {

  /** A place of the tests: the station of Yverdon-les-Bains, at the corner of four cells. */
  private static final GeoPoint STATION = new GeoPoint(46.78, 6.64);
  /** Half a metre from the station, in the cell south-west of it. */
  private static final GeoPoint NEAR = new GeoPoint(46.779_996, 6.639_996);
  /** Ten metres from the station. */
  private static final GeoPoint FAR = new GeoPoint(46.7801, 6.64);
  /** A file of a place, valid: its version, one place, its coordinates and its kind. */
  private static final int[] VALID = {FeatureIndex.FORMAT_VERSION, 1, 467_800_000, 66_400_000, 0};
  /** The index of the version in {@link #VALID}. */
  private static final int VERSION = 0;
  /** The index of the number of places in {@link #VALID}. */
  private static final int COUNT = 1;
  /** The index of the latitude in {@link #VALID}. */
  private static final int LATITUDE = 2;
  /** The index of the longitude in {@link #VALID}. */
  private static final int LONGITUDE = 3;
  /** The index of the kind in {@link #VALID}. */
  private static final int KIND = 4;

  /**
   * A place added twice is kept once; it is found within a metre, even across the edge of its cell, not ten metres
   * away; saved and read back, the index holds the same places; a folder without file gives an empty index.
   *
   * @param folder the folder of the graph.
   *
   * @throws IOException if the index cannot be saved.
   */
  @Test
  @DisplayName("Phase 5: a place of a feature is kept once, found within a metre, the same saved and read back")
  void placesTest(@TempDir final Path folder) throws IOException {
    final FeatureIndex index = new FeatureIndex();
    index.add(STATION.latitude(), STATION.longitude(), FeatureKind.CATTLE_GRID);
    index.add(STATION.latitude(), STATION.longitude(), FeatureKind.CATTLE_GRID);
    final FeatureIndex empty = FeatureIndex.load(folder);
    index.save(folder);
    final FeatureIndex read = FeatureIndex.load(folder);
    assertAll(
        () -> assertEquals(1, index.size()),
        () -> assertEquals(Optional.of(FeatureKind.CATTLE_GRID), index.find(NEAR)),
        () -> assertEquals(Optional.empty(), index.find(FAR)),
        () -> assertEquals(0, empty.size()),
        () -> assertEquals(1, read.size()),
        () -> assertEquals(Optional.of(FeatureKind.CATTLE_GRID), read.find(STATION)));
  }

  /**
   * A file of another version, of a negative number of places, of a length that is not the one of its places, with a
   * latitude, a longitude or a kind that is not valid, or cut short, is refused.
   *
   * @param folder the folder of the graphs, one per file.
   *
   * @throws IOException if a file cannot be written.
   */
  @Test
  @DisplayName("Phase 5: a file of the places of the features that is not valid is refused")
  void invalidTest(@TempDir final Path folder) throws IOException {
    final int[] wrong = {2, -1, 900_000_001, -1_800_000_001, FeatureKind.values().length};
    for (int defect = 0; defect < wrong.length; defect++) {
      final int[] values = VALID.clone();
      values[defect] = wrong[defect];
      assertRefused(Files.createDirectory(folder.resolve("defect-" + defect)), file(values, false));
    }
    assertRefused(Files.createDirectory(folder.resolve("longer")), file(VALID, true));
    assertRefused(Files.createDirectory(folder.resolve("cut")), new byte[] {0, 0, 0});
  }

  /**
   * Writes the content of a file of the index.
   *
   * @param values the version, the number of places, then the latitude, the longitude and the kind of a place.
   * @param longer whether a byte is added at the end.
   *
   * @return the bytes of the file.
   *
   * @throws IOException never: the bytes are written in memory.
   */
  private static byte[] file(final int[] values, final boolean longer) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bytes)) {
      out.writeInt(values[VERSION]);
      out.writeInt(values[COUNT]);
      out.writeInt(values[LATITUDE]);
      out.writeInt(values[LONGITUDE]);
      out.writeByte(values[KIND]);
      if (longer) {
        out.writeByte(0);
      }
    }
    return bytes.toByteArray();
  }

  /**
   * Asserts that a file of the index is refused.
   *
   * @param folder the folder of the graph.
   * @param content the content of its file of the index.
   *
   * @throws IOException if the file cannot be written.
   */
  private static void assertRefused(final Path folder, final byte[] content) throws IOException {
    Files.write(folder.resolve(FeatureIndex.FILE), content);
    assertEquals(RoutingError.INVALID_GRAPH,
                 assertThrows(RoutingException.class, () -> FeatureIndex.load(folder)).getError());
  }

}
