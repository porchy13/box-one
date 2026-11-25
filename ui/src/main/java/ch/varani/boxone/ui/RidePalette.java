package ch.varani.boxone.ui;

import ch.varani.boxone.core.WaypointKind;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * The colours of the simulated run in 8 bits (development plan, phase 7c): the tokens {@code -c-ride-*} of the token
 * style sheets (AGENTS.md §3: no colour value outside them), the same in the light and the dark themes, as the
 * roadbook drawn as on paper; read from {@value Theme#LIGHT_TOKENS}, as a canvas draws with colours, not with style
 * sheets.
 *
 * <p>Immutable, safe for concurrent use.</p>
 *
 * @param colors the colour of each token of the run.
 */
record RidePalette(Map<String, Color> colors) {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The token of the sky. */
  static final String SKY = "-c-ride-sky";
  /** The token of the hills on the horizon. */
  static final String HILL = "-c-ride-hill";
  /** The token of the grass. */
  static final String GRASS = "-c-ride-grass";
  /** The token of the darker bands of the grass. */
  static final String GRASS_DARK = "-c-ride-grass-dark";
  /** The token of the road. */
  static final String ROAD = "-c-ride-road";
  /** The token of the darker bands of the road. */
  static final String ROAD_DARK = "-c-ride-road-dark";
  /** The token of a track. */
  static final String TRACK = "-c-ride-track";
  /** The token of the darker bands of a track. */
  static final String TRACK_DARK = "-c-ride-track-dark";
  /** The token of a bump across the road. */
  static final String BUMP = "-c-ride-bump";
  /** The token of the back of a bump, in its shadow, and of the broken edge of a hole. */
  static final String BUMP_DARK = "-c-ride-bump-dark";
  /** The token of the far wall inside a hole. */
  static final String HOLE_WALL = "-c-ride-hole-wall";
  /** The token of the pixels unlit of the instruments. */
  static final String UNLIT = "-c-ride-unlit";
  /** The token of the white of the sprites and of the instruments. */
  static final String WHITE = "-c-ride-white";
  /** The token of the red of the sprites and of the instruments. */
  static final String RED = "-c-ride-red";
  /** The token of the orange of the sprites and of the mark of the speed limit. */
  static final String ORANGE = "-c-ride-orange";
  /** The token of the grey of the sprites and of the frames of the instruments. */
  static final String GREY = "-c-ride-grey";
  /** The token of the dark grey of the sprites and of the case of the trip meter. */
  static final String GREY_DARK = "-c-ride-grey-dark";
  /** The token of a hole in the road. */
  static final String HOLE = "-c-ride-hole";
  /** The token of the white edges of a tarmac road. */
  static final String EDGE = "-c-ride-edge";
  /** The token of the line in the middle of the road. */
  static final String LANE = "-c-ride-lane";
  /** The token of the dark squares of the finish line: the ink of the roadbook. */
  static final String INK = "-c-ink";
  /** The token of each letter of the pixels of a sprite ({@link RideRenderer#MOTO}). */
  static final Map<Character, String> SPRITE_TOKENS = Map.ofEntries(
      Map.entry('K', "-c-ride-black"), Map.entry('W', WHITE), Map.entry('O', ORANGE), Map.entry('B', "-c-ride-blue"),
      Map.entry('G', GREY), Map.entry('R', RED), Map.entry('L', "-c-ride-leaf"), Map.entry('T', "-c-ride-trunk"),
      Map.entry('P', "-c-ride-grey-light"), Map.entry('S', GREY_DARK), Map.entry('D', "-c-ride-red-dark"));

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Reads the colours of the run from the token style sheet of the light theme.
   *
   * @return the palette, every token of the style sheet as a colour.
   */
  static RidePalette load() {
    final Map<String, String> tokens = Theme.tokens(Theme.class.getResource(Theme.LIGHT_TOKENS));
    return new RidePalette(tokens.entrySet().stream()
                                 .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                                                                       entry -> Color.web(entry.getValue()))));
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Gives the colour of a token.
   *
   * @param token the token, {@code -c-…}.
   *
   * @return its colour.
   *
   * @throws IllegalArgumentException if the style sheet has no such token: a defect of the application.
   */
  Color color(final String token) {
    final Color color = colors.get(token);
    if (color == null) {
      throw new IllegalArgumentException("no token " + token);
    }
    return color;
  }

  /**
   * Gives the token of the colour of the disc of a waypoint, as the strip draws it (FIA, Appendix III, §5.12.3).
   *
   * @param kind the kind of the waypoint.
   *
   * @return the token: cyan for a WPM or a WPV, red for a WPS, yellow for a WPC, blue for a WPE, orange for a DZ,
   *         green for a FZ, the paper for a WPN and a WPP.
   */
  static String waypoint(final WaypointKind kind) {
    return switch (kind) {
      case WPM, WPV -> "-c-wp-cyan";
      case WPS -> "-c-wp-red";
      case WPC -> "-c-wp-yellow";
      case WPE -> "-c-wp-blue";
      case DZ -> "-c-wp-orange";
      case FZ -> "-c-wp-green";
      case WPN, WPP -> "-c-paper";
    };
  }

  /**
   * Gives the colour of a letter of the pixels of a sprite.
   *
   * @param letter the letter.
   *
   * @return its colour, or {@code null} for a transparent pixel (a letter without token, {@code '.'}).
   */
  Color sprite(final char letter) {
    final String token = SPRITE_TOKENS.get(letter);
    return token == null ? null : color(token);
  }

}
