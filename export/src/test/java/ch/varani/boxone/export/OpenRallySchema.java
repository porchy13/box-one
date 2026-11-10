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
 * reaches the network (AGENTS.md §5).</p>
 *
 * <p>Utility class without state, safe for concurrent use: each validation has its own validator.</p>
 */
final class OpenRallySchema {

  /** The folder of the schemas and of the reference files, relative to this class. */
  static final String FOLDER = "openrally/";

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
    final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar:file");
    final Schema schema = factory.newSchema(OpenRallySchema.class.getResource(FOLDER + "openrally-strict.xsd"));
    final Validator validator = schema.newValidator();
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    validator.validate(gpx);
  }

}
