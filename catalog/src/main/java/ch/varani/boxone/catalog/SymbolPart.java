package ch.varani.boxone.catalog;

import java.awt.Color;
import java.util.Objects;

/**
 * A part of the drawing of a symbol ({@link SymbolDrawing}): an SVG path, stroked with a width or filled, in a colour.
 *
 * <p><b>Preconditions</b>, checked by the reader of the catalogues: a valid path ({@link SvgPath}); a positive, finite
 * width, or 0 for a part filled.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param path the SVG path of the part, in pixels around the origin of the symbol.
 * @param colour the colour of the part.
 * @param width the width of its line, in pixels; 0 if the part is filled.
 */
public record SymbolPart(String path, Color colour, double width) {

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Creates a part of a symbol.
   *
   * @param path the SVG path of the part.
   * @param colour the colour of the part.
   * @param width the width of its line, in pixels, or 0 if it is filled.
   *
   * @throws NullPointerException if the path or the colour is {@code null}.
   */
  public SymbolPart {
    Objects.requireNonNull(path);
    Objects.requireNonNull(colour);
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Tells whether the part is filled rather than stroked.
   *
   * @return {@code true} if its width is 0.
   */
  public boolean filled() {
    return width == 0;
  }

}
