package ch.varani.boxone.ui;

import ch.varani.boxone.routing.Router;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * What the main window ({@link MainController}) uses beyond JavaFX: its data folder, the thread of the long work, the
 * routing engine and the choice of the files, replaced by fakes in the tests.
 *
 * <p>Immutable, safe for concurrent use; the functions run on the thread of the long work, except {@code chooser},
 * {@code saver} and {@code opener}, which run on the JavaFX Application Thread.</p>
 *
 * @param dataFolder the data folder of the application ({@link DataFolder}).
 * @param background runs the long work (load and import of the map, routing), one after the other, off the JavaFX
 *                   Application Thread (AGENTS.md §3).
 * @param loader loads the graph of an earlier import from its folder: a router, or nothing if the folder holds no
 *               graph ({@code GraphHopperRouter::load}).
 * @param importer imports an extract of OpenStreetMap (first argument) into the folder of the graph (second
 *                 argument), and returns its router ({@code GraphHopperRouter::importMap}).
 * @param chooser asks the user for an extract of OpenStreetMap ({@link MapFiles}).
 * @param saver asks the user for the file of the roadbook to export ({@link RoadbookFiles}).
 * @param opener asks the user for the roadbook to open ({@link RoadbookFiles#opening}, issue #5 of GitLab).
 */
record Services(Path dataFolder, Executor background, Function<Path, Optional<? extends Router>> loader,
                BiFunction<Path, Path, ? extends Router> importer, Function<Window, Optional<Path>> chooser,
                Function<Window, Optional<Path>> saver, Function<Window, Optional<Path>> opener) {
}
