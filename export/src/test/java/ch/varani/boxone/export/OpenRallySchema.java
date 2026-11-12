package ch.varani.boxone.export;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;

/**
 * The strict OpenRally schema of the tests: GPX 1.1 with the extensions of OpenRally v1.0.3, every extension being
 * an element of the OpenRally schema.
 *
 * <p>The schemas are those of the OpenRally project ({@code openrally.xsd}, CC BY 4.0) and of GPX 1.1
 * ({@code gpx.xsd}), with the strict wrapper of the OpenRally project ({@code gpx-strict.xsd},
 * {@code openrally-strict.xsd}), all local, in the folder {@value #FOLDER} of the test resources: the validation never
 * reaches the network (AGENTS.md §5). The roadbooks exported by Box One are validated against the strict schema of
 * Box One ({@value #BOX_ONE_STRICT}): the same, with the route of Box One in the extensions of the metadata
 * ({@code boxone/boxone.xsd}, issue #5 of GitLab).</p>
 *
 * <p>Utility class without state, safe for concurrent use: each validation has its own validator.</p>
 */
final class OpenRallySchema {

  /** The folder of the schemas and of the reference files, relative to this class. */
  static final String FOLDER = "openrally/";
  /** The strict schema of OpenRally, relative to this class. */
  static final String OPENRALLY_STRICT = FOLDER + "openrally-strict.xsd";
  /** The strict schema of the roadbooks exported by Box One, relative to this class. */
  static final String BOX_ONE_STRICT = "boxone/boxone-strict.xsd";

  /**
   * Prevents the instantiation of this utility class.
   */
  private OpenRallySchema() {
  }

  /**
   * Validates a file against the strict OpenRally schema.
   *
   * @param gpx the file.
   *
   * @throws SAXException if the file is not valid, or if the schemas cannot be read.
   * @throws IOException if the file cannot be read.
   */
  static void validate(final Source gpx) throws SAXException, IOException {
    validate(gpx, OPENRALLY_STRICT);
  }

  /**
   * Validates a roadbook exported by Box One against the strict schema of Box One: OpenRally and the route of Box One.
   *
   * @param gpx the file.
   *
   * @throws SAXException if the file is not valid, or if the schemas cannot be read.
   * @throws IOException if the file cannot be read.
   */
  static void validateExport(final Source gpx) throws SAXException, IOException {
    validate(gpx, BOX_ONE_STRICT);
  }

  /**
   * Validates a file against a schema of the test resources.
   *
   * @param gpx the file.
   * @param schemaFile the schema, relative to this class.
   *
   * @throws SAXException if the file is not valid, or if the schemas cannot be read.
   * @throws IOException if the file cannot be read.
   */
  private static void validate(final Source gpx, final String schemaFile) throws SAXException, IOException {
    final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
    final Schema schema = factory.newSchema(OpenRallySchema.class.getResource(schemaFile));
    final Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    validator.validate(gpx);
  }

}
