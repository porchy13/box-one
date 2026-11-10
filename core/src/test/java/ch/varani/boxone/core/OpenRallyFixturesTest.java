package ch.varani.boxone.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests of the reference OpenRally files of the test resources ({@code openrally/}): the files written by hand for
 * the import test in Terra Pirata (development plan, phase 0) stay valid GPX 1.1 files with the OpenRally extensions
 * v1.0.3, checked strictly (every extension must be an element of the OpenRally schema).
 *
 * <p>The schemas are those of the OpenRally project ({@code openrally.xsd}, CC BY 4.0) and of GPX 1.1
 * ({@code gpx.xsd}), with the strict wrapper of the OpenRally project ({@code gpx-strict.xsd},
 * {@code openrally-strict.xsd}), all local: the validation never reaches the network (AGENTS.md §5). These files
 * become the references of the export of phase 2 (module {@code export}), where they move.</p>
 */
final class OpenRallyFixturesTest {

  /** The folder of the reference files, relative to this class. */
  private static final String FOLDER = "openrally/";

  /**
   * A reference file is valid against the strict OpenRally schema.
   *
   * @param file the name of the reference file.
   *
   * @throws SAXException if the schemas cannot be read.
   */
  @ParameterizedTest
  @ValueSource(strings = {"openrally-minimal.gpx", "openrally-complete.gpx", "openrally-long-minimal.gpx",
                          "openrally-long-complete.gpx", "openrally-long-png-complete.gpx"})
  @DisplayName("Phase 0: the OpenRally test files for Terra Pirata are valid against the OpenRally schema v1.0.3")
  void validTest(final String file) throws SAXException {
    final Validator validator = schema().newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    final URL gpx = OpenRallyFixturesTest.class.getResource(FOLDER + file);
    assertDoesNotThrow(() -> validate(validator, gpx));
  }

  /**
   * Reads the strict OpenRally schema of the test resources; its imports may only read local files.
   *
   * @return the schema.
   *
   * @throws SAXException if a schema cannot be read or is invalid.
   */
  private static Schema schema() throws SAXException {
    final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
    return factory.newSchema(OpenRallyFixturesTest.class.getResource(FOLDER + "openrally-strict.xsd"));
  }

  /**
   * Validates a file.
   *
   * @param validator the validator of the schema.
   * @param gpx the address of the file.
   *
   * @throws SAXException if the file is not valid.
   * @throws IOException if the file cannot be read.
   */
  private static void validate(final Validator validator, final URL gpx) throws SAXException, IOException {
    validator.validate(new StreamSource(gpx.toExternalForm()));
  }

}
