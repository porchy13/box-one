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
