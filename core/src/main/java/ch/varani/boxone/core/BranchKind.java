package ch.varani.boxone.core;

/**
 * The kind of a branch of a junction, as a tulip draws it: the lexicon of the roadbooks of the FIA tells a tarmac road
 * from a track (2026 Cross-Country Rally Sporting Regulations, Appendix III, §5.14, "Tracks": a double line for a
 * tarmac road, a single line for a track).
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum BranchKind {

  /** A tarmac road: a road with a hard surface (asphalt, concrete, paving stones…). */
  TARMAC,
  /** A track (French <i>piste</i>): a road classified as a track, or a road without a hard surface (gravel, dirt…). */
  TRACK

}
