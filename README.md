[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/porchy13)

---

# Box One

Desktop application generating **training roadbooks** for navigation rallies. From a start and a finish, it computes
a road route with a random detour bounded in time, turns it into a roadbook drawn with several catalogues of symbols
(FIM, FIA…), exports it for Terra Pirata (OpenRally GPX), as a PDF strip and as a plain GPX, and offers a training
mode. The name refers to the very first box of a roadbook.

The project is at its beginning: the application shows a map, computes the fastest route between two places,
generates detours within a budget of time, and exports the roadbook of the route for Terra Pirata, without drawings
yet; the tulips, the editor, the other exports and the training mode come in the next versions (see
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
language. Its interface follows [`DESIGN.md`](DESIGN.md): native menus (the global menu bar on macOS), light or dark
theme, accent and colours following the system.

## Features

- **Terms of use at each start**: the terms of the [disclaimer](#disclaimer), to accept ("I accept") or to refuse
  ("Refuse and quit", or closing the dialog), which quits the application. The roadbooks exported carry them in
  short, in their description.
- **Map**: the national maps of swisstopo by default (free, no key), or the maps of MapTiler, drawn from
  OpenStreetMap, with a free personal key typed in the settings (File › Settings… on macOS, Tools › Options…
  elsewhere). To get a key, create a free account on [MapTiler Cloud](https://cloud.maptiler.com), then a new key
  under API keys: a name and a description are enough. The key is kept on the computer only, in the settings file
  of the data folder, readable by the user only (macOS, Linux). The same settings set the sensitivity of the zoom to the mouse
  wheel and to the trackpad.
- **Map extract**: the routes are computed offline on an extract of OpenStreetMap, e.g.
  `switzerland-latest.osm.pbf` from [Geofabrik](https://download.geofabrik.de/europe/switzerland.html). Open it with
  File › Open a map…: it is imported once (a few minutes for a country), then loaded at each start.
- **Fastest route**: click on the map to place the start, click again to place the finish; the fastest route by car
  is drawn as a dashed line, with its distance and its duration in a card at the top right of the map. A third click
  begins a new route.
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
- **Roadbook for Terra Pirata**: File › Export the roadbook… (Shortcut+E) writes the roadbook of the route shown (the
  fastest route or the detour) as a GPX file with the extensions of OpenRally, the format read by Terra Pirata: one
  box at the start, one at each instruction of the route (turn, roundabout, road of another name…) and one at the
  finish, each with its total distance and its heading (CAP) (OpenRally carries no partial distance: the reader
  computes it), and the track of the route. Import the file into RDS to read it in the application Terra Pirata. The
  tulips are not drawn yet.

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
| `core` | Domain model (geographic points, routes and their maneuvers, roadbooks and their boxes; then the waypoints and the speed zones). No dependency outside the JDK. |
| `graphhopper` | GraphHopper as one JPMS module (`graphhopper-core`, `graphhopper-web-api` and `osm-legal-default-speeds-jvm` regrouped, as they cannot go on the module path apart). No code of its own. |
| `routing` | The fastest route by car between two points, possibly through waypoints, computed by GraphHopper on an extract of OpenStreetMap, behind an interface of its own, with its maneuvers. |
| `detour` | Detours: routes through random waypoints drawn in an ellipse around the start and the finish, within a budget of time, reproducible from their seed. |
| `roadbook` | A route turned into the boxes of its roadbook: total and partial distances, heading. |
| `export` | The roadbook written for Terra Pirata (GPX 1.1 with the extensions of OpenRally v1.0.3); later the PDF strip and the plain GPX. |
| `ui` | JavaFX user interface (map in a `WebView` with Leaflet), entry point and installers. |

The catalogues of symbols and the training mode become modules of their own as they are developed.

## Dependencies

| Dependency | Role | Scope |
|------------|------|-------|
| OpenJFX (`javafx-controls`, `javafx-fxml`, `javafx-web`) | User interface, map | compile (`ui`) |
| Leaflet | Map, bundled in the resources of `ui` (no CDN) | resource (`ui`) |
| GraphHopper (`graphhopper-core`, `graphhopper-web-api`) and its libraries (Jackson, HPPC, JTS, Janino, Kotlin standard library, osmosis-osm-binary, Protocol Buffers, XML Graphics Commons, SLF4J) | Routing on OpenStreetMap | compile (`graphhopper`, `routing`) |
| SLF4J JDK14 provider | Messages of GraphHopper to `java.util.logging` | runtime (`routing`) |
| JUnit Jupiter | Tests | test |
| TestFX, Monocle | Headless user interface tests | test (`ui`) |

The versions are in the `pom.xml` files.
