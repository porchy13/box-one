package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Symbol;
import ch.varani.boxone.core.WaypointKind;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A catalogue of symbols (development plan, phase 4): how the roadbooks of a federation, or of a rally, draw their
 * elements — the tulips, which boxes show their heading, and the notes: the danger marks, the symbols on the roads and
 * how the abbreviations are written (phase 5), the marks of the waypoints and the strip on paper (phase 6) — with the
 * source of its rules and the provenance of its symbols. One catalogue is one JSON file of this module
 * ({@link CatalogueReader}).
 *
 * <p>The symbols of the catalogues are redrawn for Box One, never copied from the plates of the federations (AGENTS.md
 * §2): the provenance says after which lexicon they were drawn, and the application shows it with the catalogue.</p>
 *
 * <p><b>Preconditions</b>: every abbreviation written, every symbol drawn, every kind of waypoint marked in the
 * roadbook ({@link WaypointKind#isMarked()}) and no other. Otherwise the catalogue is rejected. The maps are
 * copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param id the identifier of the catalogue, its file name without the extension.
 * @param name the name of the catalogue, as shown to the user (the name of the federation, not translated).
 * @param source the rules the catalogue follows: the rulebook, its articles and its date.
 * @param provenance after which lexicon the symbols were redrawn, and who provides its icons.
 * @param tulip how the catalogue draws a tulip.
 * @param headings which boxes show their heading.
 * @param notes how the notes of a box are drawn.
 * @param abbreviations how each abbreviation is written; unmodifiable.
 * @param symbols how each symbol is drawn; unmodifiable.
 * @param waypoints how each kind of waypoint marked in the roadbook is marked; unmodifiable.
 * @param strip how the strip of the roadbook is drawn on paper.
 */
public record Catalogue(String id, String name, String source, String provenance, TulipStyle tulip,
                        HeadingRule headings, NoteStyle notes, Map<Abbreviation, String> abbreviations,
                        Map<Symbol, SymbolDrawing> symbols, Map<WaypointKind, WaypointMark> waypoints,
                        StripStyle strip) {

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
   * @param notes how the notes of a box are drawn.
   * @param abbreviations how each abbreviation is written, all of them.
   * @param symbols how each symbol is drawn, all of them.
   * @param waypoints how each kind of waypoint marked in the roadbook is marked, all of them and no other.
   * @param strip how the strip of the roadbook is drawn on paper.
   *
   * @throws NullPointerException if a value is {@code null}.
   * @throws IllegalArgumentException if an abbreviation is not written, a symbol not drawn, or the kinds of waypoint
   *                                  marked are not those marked in the roadbook.
   */
  public Catalogue {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(source);
    Objects.requireNonNull(provenance);
    Objects.requireNonNull(tulip);
    Objects.requireNonNull(headings);
    Objects.requireNonNull(notes);
    Objects.requireNonNull(strip);
    abbreviations = Map.copyOf(abbreviations);
    symbols = Map.copyOf(symbols);
    waypoints = Map.copyOf(waypoints);
    if (abbreviations.size() != Abbreviation.values().length || symbols.size() != Symbol.values().length) {
      throw new IllegalArgumentException("every abbreviation and every symbol");
    }
    if (!waypoints.keySet().equals(marked())) {
      throw new IllegalArgumentException("every kind of waypoint marked");
    }
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

  /**
   * Gives the kinds of waypoint marked in the roadbook, each of which a catalogue marks.
   *
   * @return the kinds marked ({@link WaypointKind#isMarked()}): all but the WPP; unmodifiable.
   */
  static Set<WaypointKind> marked() {
    return Arrays.stream(WaypointKind.values()).filter(WaypointKind::isMarked)
                 .collect(Collectors.toUnmodifiableSet());
  }

}
