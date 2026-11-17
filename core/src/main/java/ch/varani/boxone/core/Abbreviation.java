package ch.varani.boxone.core;

/**
 * An abbreviation of the lexicon of the roadbooks, written in the description of a box (development plan, phase 5:
 * notes; decision of the maintainer, 2026-11-14: the lexicon of the FIA).
 *
 * <p>The abbreviations are those of the lexicon of the FIA (2026 Cross-Country Rally Sporting Regulations, Appendix
 * III, §5.14: the tracks and the abbreviations), the same in the lexicon of the FIM and of the Lamas Rally. The
 * abbreviation is named here by its meaning; the catalogue of symbols gives how it is written ("kpL", "FRO"…), which
 * differs from one federation to another. "Off track forbidden", a crossed-out HP, is not an abbreviation that can be
 * written as text, and is left out, as are "MODIF" and "NEW NOTE", the marks of the opening crew (§5.7), which a
 * roadbook of training has not.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum Abbreviation {

  /** Track ("P"). */
  TRACK,
  /** Principal track ("PP"). */
  PRINCIPAL_TRACK,
  /** Road ("RO"). */
  ROAD,
  /** Parallel track ("P//"). */
  PARALLEL_TRACK,
  /** Off track, sight driving ("HP"). */
  OFF_TRACK,
  /** Follow the principal track ("FPP"). */
  FOLLOW_PRINCIPAL_TRACK,
  /** Follow the road ("FRO"). */
  FOLLOW_ROAD,
  /** Sandy ("SA"). */
  SANDY,
  /** Gravel ("GV"). */
  GRAVEL,
  /** Vegetation ("VG"). */
  VEGETATION,
  /** Left and right ("L/R"). */
  LEFT_AND_RIGHT,
  /** Right and left ("R/L"). */
  RIGHT_AND_LEFT,
  /** On the left ("onL"). */
  ON_LEFT,
  /** On the right ("onR"). */
  ON_RIGHT,
  /** Keep to the left ("kpL"). */
  KEEP_LEFT,
  /** Keep to the right ("kpR"). */
  KEEP_RIGHT,
  /** Keep straight ("kpS"). */
  KEEP_STRAIGHT,
  /** Less visible ("-V"). */
  LESS_VISIBLE,
  /** More or less visible ("±V"). */
  MORE_OR_LESS_VISIBLE,
  /** Alternance ("ALT"). */
  ALTERNANCE,
  /** During ("/"). */
  DURING,
  /** In, into ("IN"). */
  INTO,
  /** And, next ("ET"). */
  AND_NEXT,
  /** At ("A"). */
  AT,
  /** Stone, stony ("CX"). */
  STONY,
  /** Narrow ("E3"). */
  NARROW,
  /** Quit, leave ("QT"). */
  QUIT,
  /** Always ("TJS"). */
  ALWAYS,
  /** Many ("NBX"). */
  MANY,
  /** Imperative ("IMP"). */
  IMPERATIVE,
  /** Collapsed ("EFF"). */
  COLLAPSED,
  /** Ruts ("ORN"). */
  RUTS,
  /** Bad ("BAD"). */
  BAD,
  /** Rejoin ("RP"). */
  REJOIN,
  /** Between ("BTW"). */
  BETWEEN,
  /** Big, written as text ("BIG"). */
  BIG,
  /** Small, written as text ("SMALL"). */
  SMALL,
  /** Follow, written as text ("FOLLOW"). */
  FOLLOW

}
