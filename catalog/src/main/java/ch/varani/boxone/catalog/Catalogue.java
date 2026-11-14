package ch.varani.boxone.catalog;

import java.io.InputStream;
import java.util.Objects;

/**
 * A catalogue of symbols (development plan, phase 4): how the roadbooks of a federation, or of a rally, draw their
 * elements — for now the tulips and which boxes show their heading — with the source of its rules and the provenance of
 * its symbols. One catalogue is one JSON file of this module ({@link CatalogueReader}).
 *
 * <p>The symbols of the catalogues are redrawn for Box One, never copied from the plates of the federations (AGENTS.md
 * §2): the provenance says after which lexicon they were drawn, and the application shows it with the catalogue.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param id the identifier of the catalogue, its file name without the extension.
 * @param name the name of the catalogue, as shown to the user (the name of the federation, not translated).
 * @param source the rules the catalogue follows: the rulebook, its articles and its date.
 * @param provenance after which lexicon the symbols were redrawn, and who provides its icons.
 * @param tulip how the catalogue draws a tulip.
 * @param headings which boxes show their heading.
 */
public record Catalogue(String id, String name, String source, String provenance, TulipStyle tulip,
                        HeadingRule headings) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The file of the catalogue of the FIA, in this module. */
  static final String FIA_FILE = "fia.json";

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a catalogue.
   *
   * @param id the identifier of the catalogue.
   * @param name the name of the catalogue.
   * @param source the rules the catalogue follows.
   * @param provenance after which lexicon the symbols were redrawn.
   * @param tulip how the catalogue draws a tulip.
   * @param headings which boxes show their heading.
   *
   * @throws NullPointerException if a value is {@code null}.
   */
  public Catalogue {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(source);
    Objects.requireNonNull(provenance);
    Objects.requireNonNull(tulip);
    Objects.requireNonNull(headings);
  }

  /**
   * Reads the catalogue of the FIA: the lexicon of the roadbooks of the 2026 Cross-Country Rally Sporting Regulations,
   * Appendix III, the one of the FIM and of other rallies too, for the tulips.
   *
   * @return the catalogue of the FIA.
   *
   * @throws CatalogueException if the file of the catalogue, part of the application, cannot be read: a defect.
   */
  public static Catalogue fia() {
    final InputStream input = Catalogue.class.getResourceAsStream(FIA_FILE);
    return CatalogueReader.read(Objects.requireNonNull(input, FIA_FILE));
  }

}
