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
- **Icon**: the icon of the application, a junction of a tulip and the box number 1, in the installers and in the
  window.
- **Map**: the national maps of swisstopo (default), or the maps of MapTiler with a personal key typed in the
  settings; the attributions of the provider and of OpenStreetMap on the map; the sensitivity of the zoom to the mouse
  wheel and to the trackpad, set in the settings.
- **Fastest route**: an extract of OpenStreetMap (e.g. of Geofabrik) is imported with File › Open a map…; the start
  and the finish clicked on the map give the fastest route by car, drawn on the map with its distance and duration.
- **Terms of use at each start**: the routes are indicative, only the signs, the rules of the road and the local
  laws are binding, the user alone is responsible for the routes and the roadbooks, shared ones included, and the
  development team accepts no liability; "I accept", or "Refuse and quit". The roadbooks exported carry the terms
  in short in their description, and the README in full.
- **Roadbook for Terra Pirata**: File › Save the roadbook as… writes the roadbook of the route as an OpenRally GPX
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
- **Tulips**: each box of a junction of the roadbook exported for Terra Pirata carries its tulip, drawn after the
  lexicon of the FIA (2026 Cross-Country Rally Sporting Regulations, the same as the one of the FIM): the route
  followed in blue with its arrow from a pin at the bottom, the other roads in black, a tarmac road as a double line,
  a track as a single line, a roundabout as a ring with its exits and the part driven, a divided road meeting it
  counted once. The symbols are redrawn for Box One after that lexicon,
  whose icons are provided by rallynavigator.com.
- **Roadbook beside the map**: the roadbook of the route shown is drawn at the right of the map, its boxes as on
  paper (number, distances, tulip, heading); the box selected, with the mouse or the arrows of the keyboard, is shown on the
  map, and stays black on white in the dark theme, framed in the accent.
- **Headings where needed**: as the FIA asks, the roadbook exported gives the heading (CAP) only at the start, at
  junctions of four roads or more, and where the road taken is a track. At a roundabout, the heading is the one of
  the road taken out of it, no longer the direction of the ring at its entry. Open a map again to tell the gravel roads
  from the tarmac roads.
- **Numbers as the system writes them**: the distances and the durations follow the number format chosen in the
  settings of macOS or Windows, e.g. a decimal point in Swiss French.
- **Save and recent roadbooks**: File › Save the roadbook writes the roadbook again into its file without asking,
  Save the roadbook as… chooses another one (it replaces Export the roadbook…); the title of the window names the
  file and says when the roadbook is modified; closing with changes not saved asks whether to save them, and waits
  for a roadbook being saved. File › Open a recent roadbook opens the roadbooks opened or saved last, 5 by default
  and up to 20 in the settings; one moved or deleted since is removed from the list, with a message.
- **Correct the roadbook**: the box selected in the roadbook beside the map is corrected at the right of the map: its total
  distance, its heading and whether the heading is shown (where the FIA asks for it, always or never); a box is
  removed (Delete, or Backspace on macOS), or added with a right-click on the route (Add a box here), and selected
  then; the box selected is moved by dragging its circle on the map, which follows the route, its distances, its
  tulip and its heading computed again where it is dropped. Each change can be undone, the map staying where it is and at its zoom, is kept where the route still passes
  when the start or the finish moves or a detour is generated, and is exported with the roadbook and opened with it.
- **Correct the tulips**: the tulip of the box selected is drawn under its fields, a handle at the end of each
  branch: a branch is selected by its handle and turned by dragging it, between its neighbours; or chosen in the form,
  its angle typed. A branch is a tarmac road or a track, the road of the arrival too; Take this branch makes it the
  route, Add a branch adds a road of the kind of the arrival in the widest space, Remove the branch removes it (not the route), and Reset the
  tulip brings back the tulip read on the map. The tulip corrected is drawn in the roadbook and exported; it is kept
  while the route crosses the junction the same way, the tulip read on the map coming back if the route crosses it
  otherwise. Each change can be undone.
- **Notes of a box**: under the tulip of the box selected, its notes after the lexicon of the FIA: a danger of one to
  three exclamation marks, drawn in red in the tulip; the abbreviations of the lexicon ("kpL", "FRO", "ET"…), in the
  order they are read, and a line of free text in English, written under the heading; the symbols of the lexicon on
  the road before or after the junction (the rail road of a level crossing, a bump, a ditch, a step, a bridge, a gate,
  a ford, a tunnel…), drawn in the tulip; the symbols beside the road (a village, a church, a pylon, a tree, a lake,
  the dunes… the whole lexicon of the FIA), placed freely in the tulip: dragged with the mouse, or their position
  typed, at the start and at the finish too. The symbols and the abbreviations are chosen in lists with a search,
  each symbol drawn beside its name, and removed by the cross of their line.
- **Window in three columns**: the roadbook at the left, the map in the middle, the editor of the box selected at the
  right, on the full height: the tulip of the box at its top, then the tabs Box, Tulip, Symbols, Abbreviations and
  Notes; in Symbols and Abbreviations, the choice first, the elements of the box under it. The notes are shown in the roadbook, exported for Terra Pirata (the danger,
  the tulip with its symbols, the description as an image) and kept with the roadbook when it is saved and opened
  again. Each change can be undone.
- **Notes from the map**: the level crossings, the gates and cattle grids, the tunnels, the bridges, the fords, and the
  places where the route leaves the tarmac for a track are noted in the roadbook from the map: the rail road, the gate
  or the cattle gate, the tunnel, over a bridge or wading on the road,
  and the abbreviation of the track with a danger of level 1, in the box of the junction within 50 m of them, or in a
  box of their own. They
  are changed and removed as the other notes, and "Reset the box" brings them back; a box of a feature removed by the
  user stays removed. The map must be imported again (File › Open a map…) for the level crossings and the barriers.
- **Waypoints**: the seven waypoints of the FIA (WPM, WPN, WPS, WPC, WPV, WPE, WPP) placed by the context menu of
  the map ("Add a waypoint here", on the box near the place or on a box added there) or in the tab Box of the editor,
  their radii of opening and of validation those of the FIA, changed by the user; a danger of level 3 is a WPS. They
  are marked in the roadbook by their letter in a circle, in the colours of the FIA, but the WPP, and exported for
  Terra Pirata with their radii.
- **Speed zones**: the start (DZ) and the finish (FZ) of a speed zone, two more kinds of waypoint, placed by the
  context menu of the map ("Start a speed zone here", "Finish the speed zone here") or in the tab Box of the editor;
  the speed limit of a DZ, 50 km/h by default, from 10 to 130 km/h; a DZ inside a zone finishes it at the same box,
  shown and exported as a FZ then a DZ (a zone at 30 km/h to box 2, then at 40 km/h). In the roadbook, DZ in an orange circle, FZ in a
  green one, the boxes of the zone framed in orange, and the sign of the speed limit in the description of the start
  and of the finish (struck out); exported for Terra Pirata with the speed of the DZ (`speed`) and the sign in the
  image of the notes.
- **Abbreviations of the dunes**: the abbreviations of the dunes of the lexicon of the FIA, DN (dune), DNX (many
  dunes), DNT (small dune) and L1 to L3 (difficulty level of the dunes), among the abbreviations of a box.
- **No route lost by a click**: once the start and the finish are placed, a click on the map changes nothing; the
  start and the finish are moved by dragging their marker, or by the context menu of the map (right-click, "Place the
  start here", "Place the finish here"); the start or the finish of a detour moved generates the detour again, with
  its budget, its kinds of road avoided and its seed, or gives the fastest route if no detour is found. Edit › Undo
  and Edit › Redo bring back the itineraries, the routes and the detours shown before (the last 50 changes).
