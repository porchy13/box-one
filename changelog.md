# Changelog

All notable changes of the project are documented in this file.

## [Unreleased]

### Added
- **Project**: multi-module Maven project (modules `core` and `ui`), quality gate (Checkstyle, tests, 100 % Jacoco
  coverage, Javadoc), installers built by `jpackage` (`mvn -P installer clean verify`) and smoke test of the packaged
  application (`--smoke-test`).
- **Main window**: the frame of the application, in French or English following the system, with the light or dark
  theme of the system and its colours: the accent, the background and the text chosen by the user for the system,
  followed when they change.
- **Map**: the national maps of swisstopo (default), or the maps of MapTiler with a personal key typed in the
  settings; the attributions of the provider and of OpenStreetMap on the map; the sensitivity of the zoom to the mouse
  wheel and to the trackpad, set in the settings.
- **Fastest route**: an extract of OpenStreetMap (e.g. of Geofabrik) is imported with File › Open a map…; the start
  and the finish clicked on the map give the fastest route by car, drawn on the map with its distance and duration.
- **Roadbook for Terra Pirata**: File › Export the roadbook… writes the roadbook of the route as an OpenRally GPX
  file (GPX 1.1, extensions of OpenRally v1.0.3), to import into RDS for Terra Pirata: a box at the start, at each
  instruction of the route and at the finish, with its total distance and its heading (CAP), the track of the route
  and the attribution of OpenStreetMap; no tulip yet.
- **Detours**: Route › Generate a detour… generates a detour from the start to the finish, through waypoints drawn
  at random in an ellipse around them, by other roads than the fastest route and longer by a budget at most: 0 to
  60 minutes by steps of 5 (0 minutes: the same duration by other roads), or 5 to 100 % of the duration of the
  fastest route by steps of 5, the time it gives shown under the choice; without driving a road twice nor turning
  back, nor the roads for residents or private and the tracks elsewhere than around
  the start and the finish; it is drawn over the fastest route, and its seed, shown in the status bar,
  generates it again. The roadbook exported is the one of the route shown.
