package ch.varani.boxone.catalog;

import ch.varani.boxone.core.Abbreviation;
import ch.varani.boxone.core.Symbol;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The elements of a catalogue easily taken one for another (development plan, phase 7a: the mode "confusions" of the
 * quiz mixes the symbols that look alike; decision of the maintainer, 2026-11-20: groups of similar symbols written
 * into the catalogue by hand, the confusions between the FIM and the FIA waiting for a catalogue of the FIM): groups
 * of symbols (a bump, a dip, a compression…) and groups of abbreviations ("kpL", "kpR", "kpS"…), chosen for Box One
 * after the lexicon of the FIA, not given by its rulebook.
 *
 * <p><b>Preconditions</b>: each group holds at least {@value #MIN_GROUP} elements, and an element is in one group at
 * most; an element in no group is easily told from the others. The groups are copied.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param symbols the groups of similar symbols; unmodifiable.
 * @param abbreviations the groups of similar abbreviations; unmodifiable.
 */
public record Confusions(List<Set<Symbol>> symbols, List<Set<Abbreviation>> abbreviations) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The smallest group: an element is confused with another one at least. */
  public static final int MIN_GROUP = 2;
  /** No confusion: every element easily told from the others. */
  public static final Confusions NONE = new Confusions(List.of(), List.of());

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates the confusions of a catalogue.
   *
   * @param symbols the groups of similar symbols.
   * @param abbreviations the groups of similar abbreviations.
   *
   * @throws NullPointerException if a group or an element is {@code null}.
   * @throws IllegalArgumentException if a group holds less than {@value #MIN_GROUP} elements, or an element is in two
   *                                  groups.
   */
  public Confusions {
    symbols = groups(symbols);
    abbreviations = groups(abbreviations);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the group of a symbol.
   *
   * @param symbol the symbol.
   *
   * @return the symbols similar to it, itself included, or an empty set if it is in no group.
   */
  public Set<Symbol> of(final Symbol symbol) {
    return group(symbols, symbol);
  }

  /**
   * Gives the group of an abbreviation.
   *
   * @param abbreviation the abbreviation.
   *
   * @return the abbreviations similar to it, itself included, or an empty set if it is in no group.
   */
  public Set<Abbreviation> of(final Abbreviation abbreviation) {
    return group(abbreviations, abbreviation);
  }

  /**
   * Copies groups of elements and checks them.
   *
   * @param <T> the type of the elements.
   * @param groups the groups.
   *
   * @return the groups, copied; unmodifiable.
   *
   * @throws NullPointerException if a group or an element is {@code null}.
   * @throws IllegalArgumentException if a group holds less than {@value #MIN_GROUP} elements, or an element is in two
   *                                  groups.
   */
  private static <T> List<Set<T>> groups(final List<Set<T>> groups) {
    final List<Set<T>> copied = groups.stream().map(Set::copyOf).toList();
    final Set<T> seen = new HashSet<>();
    for (final Set<T> group : copied) {
      if (group.size() < MIN_GROUP) {
        throw new IllegalArgumentException("a group of one element");
      }
      for (final T element : group) {
        if (!seen.add(element)) {
          throw new IllegalArgumentException("an element in two groups");
        }
      }
    }
    return copied;
  }

  /**
   * Finds the group of an element.
   *
   * @param <T> the type of the elements.
   * @param groups the groups.
   * @param element the element.
   *
   * @return its group, or an empty set if it is in none.
   */
  private static <T> Set<T> group(final List<Set<T>> groups, final T element) {
    return groups.stream().filter(group -> group.contains(element)).findFirst().orElse(Set.of());
  }

}
