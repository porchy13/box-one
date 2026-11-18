package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of {@link RoadFeature} and {@link FeatureKind} (development plan, phase 5, increment 3b): a feature of the map
 * at a point of a route.
 */
final class RoadFeatureTest {

  /** The index of the point of the feature of the tests. */
  private static final int INDEX = 3;

  /**
   * A feature keeps its point and its kind.
   */
  @Test
  @DisplayName("Phase 5: a feature of the map keeps its point and its kind")
  void valuesTest() {
    final RoadFeature feature = new RoadFeature(INDEX, FeatureKind.LEVEL_CROSSING);
    assertAll(
        () -> assertEquals(INDEX, feature.index()),
        () -> assertEquals(FeatureKind.LEVEL_CROSSING, feature.kind()),
        () -> assertEquals(FeatureKind.TRACK, FeatureKind.valueOf("TRACK")),
        () -> assertEquals(0, new RoadFeature(0, FeatureKind.GATE).index()));
  }

  /**
   * A feature before the first point of the route is rejected; a feature without kind is a programming error.
   */
  @Test
  @DisplayName("Phase 5: a feature before the start of the route, or without kind, is rejected")
  void invalidTest() {
    assertAll(
        () -> assertEquals(ValidationError.INVALID_MANEUVER,
                           assertThrows(DomainException.class,
                                        () -> new RoadFeature(-1, FeatureKind.CATTLE_GRID)).getError()),
        () -> assertThrows(NullPointerException.class, () -> new RoadFeature(INDEX, null)));
  }

}
