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
- **Terms of use at each start**: the routes are indicative, only the signs, the rules of the road and the local
  laws are binding, the user alone is responsible for the routes and the roadbooks, shared ones included, and the
  development team accepts no liability; "I accept", or "Refuse and quit". The roadbooks exported carry the terms
  in short in their description, and the README in full.
- **Roadbook for Terra Pirata**: File › Export the roadbook… writes the roadbook of the route as an OpenRally GPX
  file (GPX 1.1, extensions of OpenRally v1.0.3), to import into RDS for Terra Pirata: a box at the start, at each
  instruction of the route and at the finish, with its total distance and its heading (CAP), the track of the route
  and the attribution of OpenStreetMap; no tulip yet.
- **Detours**: Route › Generate a detour… generates a detour from the start to the finish, through waypoints drawn
  at random in an ellipse around them, by other roads than the fastest route and longer by a budget at most: 0 to
  60 minutes by steps of 5 (0 minutes: the same duration by other roads), or 5 to 100 % of the duration of the
  fastest route by steps of 5, the time it gives shown under the choice; without driving a road twice nor turning
  back, nor the roads for residents or private and the tracks elsewhere than around
  the start and the finish; the motorways, the expressways, the ferries and car-carrying trains, and the toll roads (the motorways of the
  vignette included) can be avoided, a choice kept for the next detours; of ten detours drawn, the one with the most
  boxes per kilometre is kept, for a roadbook with many tulips; it is drawn over the fastest route, and its seed, shown in the status bar,
  generates it again. The roadbook exported is the one of the route shown.
- **Open a roadbook**: File › Open a roadbook… opens a roadbook exported by Box One and shows its route as it was
  exported, with its start and finish and, for a detour, its seed and its budget, so that the detour can be generated
  again; the roadbooks exported hold them for this purpose (format version 1). A file that cannot be opened is
  reported in a dialog saying why, and the route shown stays.
- **No route lost by a click**: once the start and the finish are placed, a click on the map changes nothing; the
  start and the finish are moved by dragging their marker, or by the context menu of the map (right-click, "Place the
  start here", "Place the finish here"). Edit › Undo and Edit › Redo bring back the itineraries, the routes and the
  detours shown before (the last 50 changes).
