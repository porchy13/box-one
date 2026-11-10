[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.me/porchy13)

---

# Box One

Desktop application generating **training roadbooks** for navigation rallies. From a start and a finish, it computes
a road route with a random detour bounded in time, turns it into a roadbook drawn with several catalogues of symbols
(FIM, FIA…), exports it for Terra Pirata (OpenRally GPX), as a PDF strip and as a plain GPX, and offers a training
mode. The name refers to the very first box of a roadbook.

The project is at its beginning: the application shows a map, computes the fastest route between two places and
exports its roadbook for Terra Pirata, without drawings yet; the detours, the tulips, the editor, the other exports
and the training mode come in the next versions (see [`changelog.md`](changelog.md)).

Licensed under the [Apache License, Version 2.0](LICENSE) (see also [`NOTICE`](NOTICE)). Routing data © OpenStreetMap
contributors (ODbL); background maps © swisstopo or © MapTiler.

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
- **Roadbook for Terra Pirata**: File › Export the roadbook… (Shortcut+E) writes the roadbook of the route shown as a
  GPX file with the extensions of OpenRally, the format read by Terra Pirata: one box at the start, one at each
  instruction of the route (turn, roundabout, road of another name…) and one at the finish, each with its total
  distance and its heading (CAP) (OpenRally carries no partial distance: the reader computes it), and the track of
  the route. Import the file into RDS to read it in the application Terra Pirata. The tulips are not drawn yet.

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
| `routing` | The fastest route by car between two points, computed by GraphHopper on an extract of OpenStreetMap, behind an interface of its own, with its maneuvers. |
| `roadbook` | A route turned into the boxes of its roadbook: total and partial distances, heading. |
| `export` | The roadbook written for Terra Pirata (GPX 1.1 with the extensions of OpenRally v1.0.3); later the PDF strip and the plain GPX. |
| `ui` | JavaFX user interface (map in a `WebView` with Leaflet), entry point and installers. |

The detour generator, the catalogues of symbols and the training mode become modules of their own as they are
developed.

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
