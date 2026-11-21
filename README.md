[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/porchy13)

---

# Box One

Desktop application generating **training roadbooks** for navigation rallies. From a start and a finish, it computes
a road route with a random detour bounded in time, turns it into a roadbook drawn with several catalogues of symbols
(FIM, FIA…), exports it for Terra Pirata (OpenRally GPX), as a PDF strip and as a plain GPX, and offers a training
mode. The name refers to the very first box of a roadbook.

The project is at its beginning: the application shows a map, computes the fastest route between two places, generates
detours within a budget of time, and exports the roadbook of the route for Terra Pirata, with the tulip of each
junction drawn after the lexicon of the FIA; its boxes, their tulips, their notes, their waypoints and the speed zones
can be corrected; it exports a PDF strip and a plain GPX too, a quiz revises the symbols of the catalogue, and the
boxes of random junctions are read; the rest of the training mode comes in the next versions (see
[`changelog.md`](changelog.md)).

Licensed under the [Apache License, Version 2.0](LICENSE) (see also [`NOTICE`](NOTICE)). Routing data © OpenStreetMap
contributors (ODbL); background maps © swisstopo or © MapTiler.

## Disclaimer

The routes are generated automatically from the data of OpenStreetMap, incomplete or wrong in places: they may take
roads that are forbidden, closed or dangerous, and their durations are estimated without traffic. Always follow the
signs, the rules of the road and the local laws: they alone are binding.

**You alone are responsible for the use you make of the routes and of the roadbooks, including when you share them
with other people. The development team of Box One can in no case be held liable for any misuse of the software,
nor for the roadbooks generated, nor for the data used.** The software is provided "as is", without warranty of any
kind (Apache License 2.0, sections 7 and 8). The application asks you to accept these terms at each start, and the
roadbooks exported carry them in short.

## Requirements

- JDK 25
- Apache Maven 3.9.0 or later

## Build and test

```bash
mvn clean verify
```

The build checks the style (Checkstyle, `box_one_java_checks.xml`), runs the unit tests and the user interface tests
(headless, with TestFX and Monocle), requires a test coverage of 100 % in every module (Jacoco), and generates the
Javadoc without warning.

## Run

```bash
mvn clean install -DskipTests
mvn -pl ui javafx:run
```

The application speaks the language of the system: French (formats of `fr-CH`), or English (`en-GB`) for any other
language. The numbers follow the format chosen in the settings of macOS or Windows (e.g. a decimal point in Swiss
French) when the system is in the language of the application. Its interface follows [`DESIGN.md`](DESIGN.md):
native menus (the global menu bar on macOS), light or dark theme, accent and colours following the system.

## Features

- **Terms of use at each start**: the terms of the [disclaimer](#disclaimer), to accept ("I accept") or to refuse
  ("Refuse and quit", or closing the dialog), which quits the application. The roadbooks exported carry them in
  short, in their description.
- **Map**: the national maps of swisstopo by default (free, no key), or the maps of MapTiler, drawn from
  OpenStreetMap, with a free personal key typed in the settings (File › Settings… on macOS, Tools › Options…
  elsewhere). To get a key, create a free account on [MapTiler Cloud](https://cloud.maptiler.com), then a new key
  under API keys: a name and a description are enough. The key is kept on the computer only, in the settings file
  of the data folder, readable by the user only (macOS, Linux; on Windows, in the folder of the profile of the
  user), not in the keychain of the system. It gives access to the tiles of MapTiler only; if the computer is lost
  or shared, delete the key on MapTiler Cloud and create a new one. The same settings set the sensitivity of the zoom to the mouse
  wheel and to the trackpad.
- **Map extract**: the routes are computed offline on an extract of OpenStreetMap, e.g.
  `switzerland-latest.osm.pbf` from [Geofabrik](https://download.geofabrik.de/europe/switzerland.html). Open it with
  File › Open a map…: it is imported once (a few minutes for a country), then loaded at each start.
- **Fastest route**: click on the map to place the start, click again to place the finish; the fastest route by car
  is drawn as a dashed line, with its distance and its duration in a card at the top right of the map. Once both are
  placed, a click changes nothing, so that a click by mistake does not lose the route: drag the start or the finish to
  move it, or right-click on the map and choose "Place the start here" or "Place the finish here".
- **Undo and Redo**: Edit › Undo (Shortcut+Z) brings back the start, the finish and the route or the detour shown
  before each change (a click, a marker moved, a detour generated, a roadbook opened), Edit › Redo (Shortcut+Shift+Z,
  or Ctrl+Y on Windows and Linux) the changes undone; the last 50 changes are kept, for the session only.
- **Detour**: once the fastest route is shown, Route › Generate a detour… (Shortcut+D) asks for a budget, in minutes
  (0 to 60, by steps of 5) or in percent of the duration of the fastest route (5 to 100 %, by steps of 5, the time it
  gives shown under the choice), the kinds of road to avoid (motorways, expressways, ferries and car-carrying trains, toll roads — the
  motorways of the vignette, as in Switzerland, are toll roads in OpenStreetMap; all allowed by default, the choice
  kept in the settings for the next detours), and a seed, a new one proposed each time. The detour, and the fastest
  route it is compared with, never drive the kinds avoided; avoiding the toll roads needs a map imported by this
  version (File › Open a map…). The detour passes through waypoints drawn
  at random around the start and the finish, drives other roads than the fastest route and takes longer by the budget
  at most (with 0 minutes, the same duration by other roads, seldom found), drives no road twice nor
  turns back, and drives the roads for residents or private and the tracks (forest roads) only around the start
  and the finish; of ten detours drawn from the seed, the one with the most boxes per kilometre is kept, so that
  the roadbook has many tulips to read; no waypoint lies on them nor on a service road (driveway, parking aisle). It is drawn as a solid line over the fastest route, with its distance
  and duration in the card, and its seed at the right of the status bar, where it can be selected and copied: the
  same seed, with the same start, finish, budget, kinds avoided and map, gives the same detour again. The roadbook exported is then the one of the detour.
  Its start or its finish moved, the detour is generated again between them with its budget, its kinds avoided and
  its seed; if none is found, the fastest route is shown, and the status bar says why.
- **Roadbook for Terra Pirata**: File › Save the roadbook as… (Shortcut+Shift+S) writes the roadbook of the route shown (the
  fastest route or the detour) as a GPX file with the extensions of OpenRally, the format read by Terra Pirata: one
  box at the start, one at each instruction of the route (turn, roundabout, road of another name…) and one at the
  finish, each with its total distance (OpenRally carries no partial distance: the reader computes it), and the
  track of the route. Each box of a junction carries its **tulip**, drawn after the lexicon of the FIA (the same as
  the one of the FIM): the route followed in blue with its arrow, from a pin at the bottom, the other roads in black,
  a tarmac road as a double line, a track (a road classified as a track, or without a hard surface) as a single line;
  a roundabout as a ring, its exits around it (a divided road, its two halves named alike, counted once) and the part
  of the ring driven in blue. The heading (CAP) is the one of the road taken, read once out of the junction: for a
  roundabout, from its exit. As the FIA asks, it is given only where it is needed: at the start, at a junction of four roads or more, and when the road taken
  is a track. A map imported with an earlier version of Box One knows the tracks by their class only: open it again
  (File › Open a map…) to draw the gravel roads as tracks. Import the file into RDS to read it in the application
  Terra Pirata. The file also holds what Box One needs to open it again: the start and the finish
  clicked and, for a detour, its seed, its budget and the kinds of road it avoids.
- **PDF strip**: File › Export as PDF… writes the roadbook as a PDF to print and to read in a roadbook holder, after
  the rulebook of the FIA: A5 pages, with 15 mm left at the top for the binding, five boxes on each, a header line
  (the number of the page, the name and the length of the roadbook, the kilometres from the first box of the page to
  the finish), and three columns as the roadbook beside the map: the distances (the total, the partial in its frame,
  the waypoint in its disc), the diagram (the tulip, its symbols and its dangers), the description (the heading where
  it is shown, the abbreviations, the text, the sign of a speed limit, the number of the waypoint). A box less than
  300 m before the next one has a vertical arrow and the next one its distances in green; a box of a danger of level
  3 is framed in red, the boxes of a speed zone in orange. Each page ends with the reminder that the route is
  indicative and the attribution of OpenStreetMap.
- **Plain GPX**: File › Export as GPX… writes the route shown as a plain GPX file, for other applications than Terra
  Pirata (navigation applications, GPS): the track of the route and one point per box of the roadbook, named by its
  number and its total distance ("Box 3 – 1.27 km"), with the reminder that the route is indicative and the
  attribution of OpenStreetMap. It is an export only: Box One opens the roadbooks it saved, not these files.
- **Roadbook beside the map**: the roadbook of the route shown is drawn at the right of the map, box after box, as on
  paper: the total and partial distances, the tulip and, where needed, the heading. Selecting a box, with the mouse or
  the arrows of the keyboard, shows its place on the map, to check the roadbook against the roads.
- **Open a roadbook**: File › Open a roadbook… (Shortcut+Shift+O) opens a roadbook exported by Box One and shows it
  as it was exported: its start and finish, its route (a detour is drawn alone, without the fastest route), its
  distance and duration and, for a detour, its seed, its budget and the duration of the fastest route; the budget is
  proposed for the next detour. It can be exported again, and once a map is open, Route › Generate a detour… with its
  seed, budget and kinds of road generates the same detour again. Only the roadbooks exported by Box One can be
  opened; a file that cannot be opened is reported in a dialog saying why, and the route shown stays.
- **Save, recent roadbooks**: File › Save the roadbook (Shortcut+S) writes the roadbook again into its file, the
  one opened or saved last, without asking; Save the roadbook as… (Shortcut+Shift+S) chooses another file. The title
  of the window gives the name of the file and says "(modified)" while the roadbook differs from it. Closing with
  changes not saved asks whether to save them (Save, Don't Save, Cancel), and a roadbook being saved is saved before
  Box One closes. File › Open a recent roadbook opens one of the roadbooks opened or saved last: 5 by default, up to
  20 in the settings (Roadbooks section); a roadbook moved or deleted since is removed from the list, with a message.
- **Correct the roadbook**: the box selected in the roadbook is corrected in the editor at the right of the map: its total distance (empty for the
  distance computed; between the box before and the box after), its heading (empty for the heading computed) and
  whether the heading is shown (where the FIA asks for it, always or never); Reset the box removes its corrections,
  Remove the box, or the key Delete (Backspace on macOS) in the roadbook, removes it (not the start nor the finish). A right-click on the route, Add a box here, adds a box at
  the nearest point of the route, within 25 m of the click. The box selected, but the start and the finish, is moved
  by dragging its circle on the map: the circle follows the route, and where it is dropped, the box takes the
  distances, the tulip and the heading of its new place (its distance and heading typed are dropped, the display of
  its heading kept). Each change can be undone with Edit › Undo, the map staying where it is. The changes
  are kept by their places when the start or the finish moves or a detour is generated, wherever the new route still
  passes; they are exported with the roadbook and opened with it.
- **Correct the tulips**: under the fields of the box selected, its tulip (not at the start nor at the finish) is
  drawn with a handle at the end of each branch and of the arrival. Click a handle to select its branch, drag it to
  turn the branch, between its neighbours; or choose the branch in the list and type its angle, in degrees from
  straight ahead (negative to the left). Each branch, the arrival too, is a tarmac road or a track. Take this branch
  makes it the route, Add a branch adds a road of the kind of the arrival in the widest space of the tulip, Remove the branch removes it
  (not the route), Reset the tulip brings back the tulip read on the map. The tulip corrected is drawn in the roadbook
  and exported; it is kept while the route crosses the junction the same way (from the same road, to the same road):
  if the route changes there, the tulip read on the map comes back.
- **Notes**: under the tulip of the box selected, its notes, after the lexicon of the FIA: the danger (none, or one to
  three exclamation marks, drawn in red in the tulip); the abbreviations of the lexicon, those of the dunes included
  (at most 6, added one after the other in the order they are read, the one selected removed), and a line of free
  text in English (at most 32
  characters), written under the heading in the roadbook; the symbols of the lexicon that stand on the road (rail road
  of a level crossing, bump, dip hole, compression, ditch, summit, over or under a bridge, step up or down, fence,
  cattle or wall gate, wading, concrete pass, tunnel, up or down hill, cut danger, crest), before or after the
  junction, at most 2 on each side, drawn in the tulip (not at the start nor at the finish); the symbols beside the
  road (the other symbols of the lexicon of the FIA: buildings, church, village, pylons, trees, water, rocks, dunes,
  signs…), at most 4, placed freely: added in a free corner of the tulip, then dragged in the tulip of the box with
  the mouse, or their position typed in percent from the centre (at the start and at the finish too). The symbols and
  the abbreviations are chosen in lists with a search (case and accents ignored), each symbol drawn beside its name;
  a cross at the end of a line, or Del, removes it.
- **Notes from the map**: the level crossings (the rail road), the gates and cattle grids (the fence or cattle gate),
  the tunnels (the tunnel), the bridges the route drives over (over a bridge), the fords (wading) and the places where the route leaves the tarmac for a track (the abbreviation of the track, "P" in the catalogue of
  the FIA, and a danger of level 1) are noted from the map, in the box of the junction within 50 m of them, before or after it, or in a box of
  their own; they are changed or removed as the other notes, and "Reset the box" brings them back. A map imported
  before this feature must be imported again for the level crossings, the barriers and the fords on a node.
- **Waypoints**: the seven waypoints of the FIA (masked WPM, navigation WPN, safety WPS, control WPC, visible WPV,
  eclipse WPE, precise WPP), placed by the context menu of the map ("Add a waypoint here": on the box within 25 m, or
  on a box added there) or in the tab Box of the editor, with the radii of opening and of validation of the FIA,
  changed there; a box with a danger of level 3 is a WPS. Each is marked in the roadbook by its letter in a circle, in
  the colours of the FIA (but the WPP), and exported for Terra Pirata with its radii.
- **Speed zones**: the start (DZ) and the finish (FZ) of a speed zone, placed by the context menu of the map ("Start
  a speed zone here", "Finish the speed zone here") or in the tab Box of the editor, with the radii of the FIA and the
  speed limit of the DZ (50 km/h by default, 10 to 130 km/h); a DZ inside a zone finishes it at the same box and
  starts another one, its box showing both the FZ and the DZ, and both speeds. In the roadbook,
  the DZ in an orange circle, the FZ in a green one, the boxes of the zone framed in orange, and the sign of the speed
  limit in the description of the start and of the finish (struck out); exported for Terra Pirata with the speed of
  the DZ and the sign in the image of the notes.
- **Speed limits of the map**: the tab Box of the editor tells the speed limit given by OpenStreetMap where the route
  leaves the box (none where the map gives none); a DZ placed on the box takes it as its speed. The limits are an
  information only: no speed zone is placed automatically.
- **Layout**: the roadbook at the left of the map, the editor of the box selected at its right, its tulip at the top
  and its fields in five tabs: Box, Tulip, Symbols, Abbreviations, Notes (the danger and the free text); a symbol or an
  abbreviation is chosen first, the elements of the box listed under the choice. The notes are exported: the danger as the danger of OpenRally, the
  tulip with its symbols and danger marks, the abbreviations and the text as the image of the notes, which Terra
  Pirata shows.

- **Symbol quiz**: Training › Symbol quiz (Shortcut+T) shows a quiz in the centre of the window, over the roadbook,
  which stays as it is (Training › Back to the roadbook, the button of the quiz, or any item of the File, Edit and
  Route menus brings it back). Choose what to revise — the 80 symbols, the 44 abbreviations, the 8 marks of the
  waypoints, the 3 danger marks of the catalogue of the FIA — and, with the mode "confusions", only the elements that
  look alike (groups of the catalogue), with their neighbours as choices. A session asks each element once, twenty at
  most (three questions for the three danger marks): an element drawn as on paper and its meaning to find among four,
  or a meaning and its element; answer with the buttons or the keys 1 to 4, then Enter. The right answer is shown at
  once, the score at the end. Nothing is kept from one session to the next yet.

- **Reading of boxes**: Training › Reading of boxes (Shortcut+Shift+T) draws routes at random in the area of the map
  seen, the shortest on every road, tracks included (place the map on the roads to train on first; a map must be
  imported) and asks up to twenty of their junctions, each once, in two exercises, both checked by default: the tulip
  of the junction and the direction to take among four in words ("Slightly right", "At the roundabout, take the 2nd
  exit"…), answered with the buttons or the keys 1 to 4; or the box, its tulip and its heading, with the map centred
  on the junction, north up, the route arriving at it drawn and the rest hidden, and a click on the road to take, the
  route through the junction then drawn. Only the junctions where the direction is clear in words are read from their
  tulip, and only the crossings of three roads or more are found on the map. The right answer is shown at once, the
  score at the end.

The settings and the imported map are kept in the data folder of the user: `~/Library/Application Support/Box One`
on macOS, `%LOCALAPPDATA%\Box One` on Windows, `$XDG_DATA_HOME/box-one` (or `~/.local/share/box-one`) on Linux.

## Package

```bash
mvn -P installer clean verify
```

On each system, the build makes the installer of the system in `ui/target/installer/dist/`: `.dmg` on macOS,
`.msi` on Windows (WiX Toolset required), `.deb` on Linux (`fakeroot` required). The runtime is linked by `jlink`
from the JDK and JavaFX; the native libraries of JavaFX (WebKit included) are shipped unpacked with the application.
The packaged application checks itself (texts, routing on a small extract of OpenStreetMap, export of its roadbook,
page of the map) with:

```bash
"ui/target/installer/image/Box One.app/Contents/MacOS/Box One" --smoke-test
```

## Modules

| Module | Role |
|--------|------|
| `core` | Domain model (geographic points, routes and their maneuvers, the geometry of their junctions, roadbooks, their boxes, their tulips and their notes, the changes of the user to a roadbook, its waypoints and its speed zones). No dependency outside the JDK. |
| `graphhopper` | GraphHopper as one JPMS module (`graphhopper-core`, `graphhopper-web-api` and `osm-legal-default-speeds-jvm` regrouped, as they cannot go on the module path apart). No code of its own. |
| `routing` | The fastest route by car between two points, possibly through waypoints, computed by GraphHopper on an extract of OpenStreetMap, behind an interface of its own, with its maneuvers and the roads of the junction of each (roundabouts included). |
| `detour` | Detours: routes through random waypoints drawn in an ellipse around the start and the finish, within a budget of time, reproducible from their seed. |
| `roadbook` | A route turned into the boxes of its roadbook: total and partial distances, heading, and the tulip of each junction, simplified from its geometry; the changes of the user applied to it, and kept by their places when the route changes. |
| `catalog` | The catalogues of symbols, one JSON file each with SVG paths (the FIA first): the tulips, the danger marks, the symbols on the roads, how the abbreviations are written, and which symbols and abbreviations look alike (for the quiz); the drawing of the diagram of a box (its tulip and its notes) and of its description with them, as PNG. Symbols redrawn after the road book lexicon of the FIA, whose icons are provided by rallynavigator.com (see `NOTICE`). |
| `export` | The roadbook written for Terra Pirata (GPX 1.1 with the extensions of OpenRally v1.0.3, the tulips and the notes as PNG), as a PDF strip and as a plain GPX. |
| `training` | The training mode: the questions of the symbol quiz drawn from a seed, the elements that look alike mixed on demand; the junctions of random routes to read, the directions in words, the branch of a click on the map; the sessions and their score. |
| `ui` | JavaFX user interface (map in a `WebView` with Leaflet), entry point and installers. |

## Dependencies

| Dependency | Role | Scope |
|------------|------|-------|
| OpenJFX (`javafx-controls`, `javafx-fxml`, `javafx-web`) | User interface, map | compile (`ui`) |
| Leaflet | Map, bundled in the resources of `ui` (no CDN) | resource (`ui`) |
| GraphHopper (`graphhopper-core`, `graphhopper-web-api`) and its libraries (Jackson, HPPC, JTS, Janino, Kotlin standard library, osmosis-osm-binary, Protocol Buffers, XML Graphics Commons, SLF4J) | Routing on OpenStreetMap | compile (`graphhopper`, `routing`) |
| Jackson (`jackson-core`, `jackson-databind`) | Reading the JSON files of the catalogues of symbols | compile (`catalog`) |
| Apache PDFBox (`pdfbox`, with `fontbox`, `pdfbox-io` and Apache Commons Logging) | Writing the PDF strip of the roadbook | compile (`export`) |
| SLF4J JDK14 provider | Messages of GraphHopper to `java.util.logging` | runtime (`routing`) |
| JUnit Jupiter | Tests | test |
| TestFX, Monocle | Headless user interface tests | test (`ui`) |

The versions are in the `pom.xml` files.
