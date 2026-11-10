package ch.varani.boxone.routing;

/**
 * A kind of road the user may forbid to the detours (decision of the maintainer, issue #11 of GitLab): the routes
 * computed while it is forbidden never drive it.
 *
 * <p>The kinds follow the tags of OpenStreetMap: a toll road is a road tagged {@code toll=yes}, the motorways of the
 * countries of the vignette (Switzerland, Austria…) included, as OpenStreetMap tags them so.</p>
 *
 * <p>Immutable, safe for concurrent use.</p>
 */
public enum RoadKind {

  /** The motorways and their slip roads ({@code highway=motorway}, {@code motorway_link}). */
  MOTORWAY,
  /** The expressways, the semi-motorways of Switzerland ({@code highway=trunk}, {@code trunk_link}). */
  TRUNK,
  /** The ferries ({@code route=ferry}). */
  FERRY,
  /** The toll roads ({@code toll=yes}), the motorways of the vignette included. */
  TOLL

}
