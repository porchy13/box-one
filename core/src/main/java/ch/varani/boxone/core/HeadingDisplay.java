package ch.varani.boxone.core;

/**
 * Whether a box shows its heading (CAP): as the catalogue of symbols decides, or as the user chose in the editor of the
 * roadbook (development plan, phase 5: a box can be corrected).
 *
 * <p>The FIA asks for the necessary headings only (2026 Cross-Country Rally Sporting Regulations, Appendix III,
 * §5.11.1); the rule of the catalogue gives them, and the user may show or hide a heading where the rule is wrong for
 * a junction.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum HeadingDisplay {

  /** The heading is shown where the rule of the catalogue asks for it. */
  RULE,
  /** The heading is shown, whatever the rule. */
  SHOWN,
  /** The heading is hidden, whatever the rule. */
  HIDDEN

}
