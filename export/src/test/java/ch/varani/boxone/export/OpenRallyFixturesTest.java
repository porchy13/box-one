package ch.varani.boxone.export;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.transform.stream.StreamSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests of the reference OpenRally files of the test resources ({@code openrally/}): the files written by hand for
 * the import test in Terra Pirata (development plan, phase 0), the references of the export, stay valid GPX 1.1 files
 * with the OpenRally extensions v1.0.3, checked strictly ({@link OpenRallySchema}).
 */
final class OpenRallyFixturesTest {

  /**
   * A reference file is valid against the strict OpenRally schema.
   *
   * @param file the name of the reference file.
   */
  @ParameterizedTest
  @ValueSource(strings = {"openrally-minimal.gpx", "openrally-complete.gpx", "openrally-long-minimal.gpx",
                          "openrally-long-complete.gpx", "openrally-long-png-complete.gpx"})
  @DisplayName("Phase 0: the OpenRally test files for Terra Pirata are valid against the OpenRally schema v1.0.3")
  void validTest(final String file) {
    final String gpx = OpenRallyFixturesTest.class.getResource(OpenRallySchema.FOLDER + file).toExternalForm();
    assertDoesNotThrow(() -> OpenRallySchema.validate(new StreamSource(gpx)));
  }

}
