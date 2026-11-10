package ch.varani.boxone.ui;

import ch.varani.boxone.core.Route;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * The summary of a route in the status bar (DESIGN.md §14): its distance and its duration, in the formats of the
 * language of the application, e.g. "12,4 km · 1 h 05" in French.
 *
 * <p><b>Algorithm</b>: the distance in kilometres with one decimal; the duration rounded to the minute, in minutes
 * below one hour ({@value #MINUTES_PATTERN}), in hours and minutes from one hour on ({@value #HOURS_PATTERN}).</p>
 *
 * <p>Utility class without state, safe for concurrent use.</p>
 */
final class RouteText {

  // *******************************************************************************************************************
  // Constants
  // *******************************************************************************************************************

  /** The key of the summary: the distance, then the duration. */
  static final String SUMMARY_PATTERN = "route.summary";
  /** The key of the distance, in kilometres. */
  static final String DISTANCE_PATTERN = "route.distance";
  /** The key of a duration below one hour, in minutes. */
  static final String MINUTES_PATTERN = "route.minutes";
  /** The key of a duration of one hour or more, in hours and minutes. */
  static final String HOURS_PATTERN = "route.hours";
  /** The number of metres in a kilometre. */
  private static final double METRES_PER_KILOMETRE = 1000;
  /** The number of minutes in an hour. */
  private static final long MINUTES_PER_HOUR = 60;
  /** Half a minute, for the rounding of the duration to the minute. */
  private static final Duration HALF_MINUTE = Duration.ofSeconds(30);

  // *******************************************************************************************************************
  // Construction & Initialization
  // *******************************************************************************************************************

  /**
   * Prevents the instantiation of this utility class.
   */
  private RouteText() {
  }

  // *******************************************************************************************************************
  // Methods
  // *******************************************************************************************************************

  /**
   * Writes the summary of a route.
   *
   * @param route the route.
   * @param texts the texts of the user interface.
   * @param locale the locale of the application, which gives the formats (e.g. {@code fr-CH}).
   *
   * @return the distance and the duration of the route.
   */
  static String summary(final Route route, final ResourceBundle texts, final Locale locale) {
    final String distance = format(texts, DISTANCE_PATTERN, locale, route.distance() / METRES_PER_KILOMETRE);
    final long minutes = route.duration().plus(HALF_MINUTE).toMinutes();
    final String duration = minutes < MINUTES_PER_HOUR
        ? format(texts, MINUTES_PATTERN, locale, minutes)
        : format(texts, HOURS_PATTERN, locale, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR);
    return format(texts, SUMMARY_PATTERN, locale, distance, duration);
  }

  /**
   * Formats a text of the bundle with its arguments.
   *
   * @param texts the texts of the user interface.
   * @param key the key of the pattern.
   * @param locale the locale of the formats.
   * @param arguments the arguments of the pattern.
   *
   * @return the text.
   */
  private static String format(final ResourceBundle texts, final String key, final Locale locale,
                               final Object... arguments) {
    return new MessageFormat(texts.getString(key), locale).format(arguments);
  }

}
