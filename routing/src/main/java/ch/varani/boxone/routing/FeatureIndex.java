package ch.varani.boxone.routing;

import ch.varani.boxone.core.FeatureKind;
import ch.varani.boxone.core.GeoPoint;
import com.graphhopper.util.DistanceCalcEarth;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The places of the features of the map that a node holds (development plan, phase 5, increment 3b): the level
 * crossings and the barriers, found by {@link FeatureParser} during the import and saved beside the graph, since
 * GraphHopper keeps neither the kind of a barrier nor the node of a level crossing.
 *
 * <p><b>Algorithm</b>: each place is kept, in tenths of microdegrees (as GraphHopper stores the nodes), in a cell of
 * {@value #CELL} tenths of microdegrees (about 1 km) of latitude and longitude; a point is looked for in its cell and
 * the eight around it, and holds the feature of the first place within {@value JunctionReader#SAME_NODE} m.</p>
 *
 * <p><b>File</b> ({@value #FILE} in the folder of the graph): its {@code formatVersion} ({@value #FORMAT_VERSION}), the
 * number of places, then each place: its latitude and its longitude in tenths of microdegrees, the ordinal of its
 * {@link FeatureKind}. A graph imported before increment 3b has no file: its routes have no feature but the tracks.
 * The file is written atomically and checked when read (AGENTS.md §5).</p>
 *
 * <p>The places are those of the map, not of the user. Thread-safe: {@link #add(double, double, FeatureKind)} is called
 * by the threads of the import.</p>
 */
final class FeatureIndex {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The name of the file of the index, in the folder of the graph. */
  static final String FILE = "boxone-features";
  /** The version of the format of the file (AGENTS.md §5). */
  static final int FORMAT_VERSION = 1;
  /** The scale of the coordinates kept: tenths of microdegrees, as GraphHopper stores the nodes. */
  static final double SCALE = 1e7;
  /** The side of a cell, in tenths of microdegrees: a hundredth of a degree, about 1 km. */
  static final int CELL = 100_000;
  /** The bytes of a place in the file: two coordinates and a kind. */
  static final int PLACE_BYTES = Integer.BYTES * 2 + 1;
  /** The bytes of the head of the file: its version and its number of places. */
  static final int HEAD_BYTES = Integer.BYTES * 2;
  /** The largest latitude, in tenths of microdegrees. */
  static final int MAX_LATITUDE = 900_000_000;
  /** The largest longitude, in tenths of microdegrees. */
  static final int MAX_LONGITUDE = 1_800_000_000;
  /** The shift of the latitude of a cell in its key, the longitude in the lower half. */
  private static final int KEY_SHIFT = 32;
  /** The mask of the lower half of a key. */
  private static final long LOWER_HALF = 0xFFFF_FFFFL;

  // *******************************************************************************************************************
  // Attributes
  // *******************************************************************************************************************

  /** The places, by the key of their cell. */
  private final Map<Long, List<Place>> cells = new HashMap<>();
  /** The number of places. */
  private int size;

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates an empty index, filled by an import or read from the folder of a graph ({@link #load(Path)}).
   */
  FeatureIndex() {
    // Nothing more: the cells are added with their places.
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Adds the place of a feature, once: a node is in every road it joins, and a barrier in the stretch GraphHopper adds
   * across it.
   *
   * @param latitude the latitude of the node, in degrees.
   * @param longitude the longitude of the node, in degrees.
   * @param kind the kind of the feature.
   */
  synchronized void add(final double latitude, final double longitude, final FeatureKind kind) {
    final Place place = new Place((int) Math.round(latitude * SCALE), (int) Math.round(longitude * SCALE), kind);
    if (!cells.getOrDefault(cell(place), List.of()).contains(place)) {
      put(place);
    }
  }

  /**
   * Finds the feature at a point.
   *
   * @param point the point, of a route.
   *
   * @return the kind of the feature of the first place within {@value JunctionReader#SAME_NODE} m, or empty if none is.
   */
  synchronized Optional<FeatureKind> find(final GeoPoint point) {
    final int latitude = (int) Math.round(point.latitude() * SCALE);
    final int longitude = (int) Math.round(point.longitude() * SCALE);
    for (int row = -1; row <= 1; row++) {
      for (int column = -1; column <= 1; column++) {
        for (final Place place : cells.getOrDefault(key(Math.floorDiv(latitude, CELL) + row,
                                                        Math.floorDiv(longitude, CELL) + column), List.of())) {
          if (DistanceCalcEarth.DIST_EARTH.calcDist(point.latitude(), point.longitude(), place.latitude() / SCALE,
                                                    place.longitude() / SCALE) <= JunctionReader.SAME_NODE) {
            return Optional.of(place.kind());
          }
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the number of places.
   *
   * @return the number of places added or read.
   */
  synchronized int size() {
    return size;
  }

  /**
   * Saves the index into the folder of a graph, atomically: a temporary file of the folder, then moved.
   *
   * @param graph the folder of the graph.
   *
   * @throws IOException if the file cannot be written.
   */
  synchronized void save(final Path graph) throws IOException {
    final Path temporary = Files.createTempFile(graph, FILE, null);
    try (OutputStream file = Files.newOutputStream(temporary);
         DataOutputStream out = new DataOutputStream(new BufferedOutputStream(file))) {
      out.writeInt(FORMAT_VERSION);
      out.writeInt(size);
      for (final List<Place> places : cells.values()) {
        for (final Place place : places) {
          out.writeInt(place.latitude());
          out.writeInt(place.longitude());
          out.writeByte(place.kind().ordinal());
        }
      }
    }
    Files.move(temporary, graph.resolve(FILE), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  /**
   * Reads the index saved in the folder of a graph.
   *
   * @param graph the folder of the graph.
   *
   * @return the index read, or an empty index if the folder has no file of the index (a graph imported before
   *         increment 3b).
   *
   * @throws RoutingException {@link RoutingError#INVALID_GRAPH} if the file cannot be read, is of another version,
   *                          does not hold as many places as it says, or holds a place or a kind that is not valid.
   */
  static FeatureIndex load(final Path graph) {
    final FeatureIndex index = new FeatureIndex();
    final Path file = graph.resolve(FILE);
    if (!Files.exists(file)) {
      return index;
    }
    try (InputStream stream = Files.newInputStream(file);
         DataInputStream in = new DataInputStream(new BufferedInputStream(stream))) {
      final long length = Files.size(file);
      final int version = in.readInt();
      final int count = in.readInt();
      if (version != FORMAT_VERSION || count < 0 || length != HEAD_BYTES + (long) count * PLACE_BYTES) {
        throw new RoutingException(RoutingError.INVALID_GRAPH);
      }
      final FeatureKind[] kinds = FeatureKind.values();
      for (int i = 0; i < count; i++) {
        final int latitude = in.readInt();
        final int longitude = in.readInt();
        final int kind = in.readUnsignedByte();
        if (Math.abs(latitude) > MAX_LATITUDE || Math.abs(longitude) > MAX_LONGITUDE || kind >= kinds.length) {
          throw new RoutingException(RoutingError.INVALID_GRAPH);
        }
        index.put(new Place(latitude, longitude, kinds[kind]));
      }
    } catch (final IOException e) {
      throw new RoutingException(RoutingError.INVALID_GRAPH, e);
    }
    return index;
  }

  /**
   * Keeps a place in its cell.
   *
   * @param place the place.
   */
  private void put(final Place place) {
    cells.computeIfAbsent(cell(place), key -> new ArrayList<>()).add(place);
    size++;
  }

  /**
   * Gives the key of the cell of a place.
   *
   * @param place the place.
   *
   * @return the key of its cell ({@link #key(int, int)}).
   */
  private static long cell(final Place place) {
    return key(Math.floorDiv(place.latitude(), CELL), Math.floorDiv(place.longitude(), CELL));
  }

  /**
   * Gives the key of a cell.
   *
   * @param row the row of the cell: its latitude, in cells.
   * @param column the column of the cell: its longitude, in cells.
   *
   * @return the key: the row in the upper half, the column in the lower half.
   */
  private static long key(final int row, final int column) {
    return ((long) row << KEY_SHIFT) | (column & LOWER_HALF);
  }

  // *******************************************************************************************************************
  // Inner classes
  // *******************************************************************************************************************

  /**
   * The place of a feature.
   *
   * <p>Immutable, safe for concurrent use.</p>
   *
   * @param latitude the latitude of the node, in tenths of microdegrees.
   * @param longitude the longitude of the node, in tenths of microdegrees.
   * @param kind the kind of the feature.
   */
  private record Place(int latitude, int longitude, FeatureKind kind) {
  }

}
