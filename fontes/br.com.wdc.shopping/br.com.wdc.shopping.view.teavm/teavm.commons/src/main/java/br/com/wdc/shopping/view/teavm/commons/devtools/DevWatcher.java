package br.com.wdc.shopping.view.teavm.commons.devtools;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Pure Java replacement for {@code fswatch -o}.
 * <p>
 * Watches directories for file changes and prints a line to stdout for each batch
 * of changes detected (after debouncing). Designed to be piped into a {@code while read}
 * loop in a shell script, just like {@code fswatch -o}.
 * <p>
 * Usage: {@code mvn exec:java -Pwatch} (configured via system properties)
 * <p>
 * System properties:
 * <ul>
 *   <li>{@code devwatch.dirs} — comma-separated directories to watch (relative to CWD)</li>
 *   <li>{@code devwatch.exclude} — regex for filenames to ignore (default: {@code \.css$})</li>
 * </ul>
 */
public class DevWatcher {

    private static final long DEBOUNCE_MS = 500;

    public static void main(String[] args) throws Exception {
        String dirsProperty = System.getProperty("devwatch.dirs", "src");
        String excludePattern = System.getProperty("devwatch.exclude", "\\.css$");

        var excludeRegex = java.util.regex.Pattern.compile(excludePattern);
        var watchDirs = parseDirs(dirsProperty);

        System.err.println("[DevWatcher] Watching: " + watchDirs);
        System.err.println("[DevWatcher] Exclude: " + excludePattern);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (Path dir : watchDirs) {
                registerRecursive(watchService, dir);
            }

            long lastEventTime = 0;

            while (true) {
                WatchKey key = watchService.take();
                Path watchedDir = (Path) key.watchable();

                boolean relevantChange = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    Path changed = watchedDir.resolve((Path) event.context());
                    String filename = changed.getFileName().toString();

                    if (excludeRegex.matcher(filename).find()) {
                        continue;
                    }

                    relevantChange = true;

                    // Register new directories
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(changed)) {
                        registerRecursive(watchService, changed);
                    }
                }

                key.reset();

                if (!relevantChange) {
                    continue;
                }

                // Debounce
                long now = System.currentTimeMillis();
                if (now - lastEventTime < DEBOUNCE_MS) {
                    continue;
                }
                Thread.sleep(DEBOUNCE_MS);
                lastEventTime = System.currentTimeMillis();

                // Emit a line (like fswatch -o)
                System.out.println("1");
                System.out.flush();
            }
        }
    }

    private static List<Path> parseDirs(String dirsProperty) {
        var dirs = new ArrayList<Path>();
        for (String d : dirsProperty.split(",")) {
            Path p = Path.of(d.trim()).toAbsolutePath().normalize();
            if (Files.isDirectory(p)) {
                dirs.add(p);
            } else {
                System.err.println("[DevWatcher] WARNING: directory not found: " + p);
            }
        }
        return dirs;
    }

    private static void registerRecursive(WatchService watchService, Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isDirectory).forEach(dir -> {
                try {
                    dir.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_DELETE);
                } catch (IOException e) {
                    System.err.println("[DevWatcher] Could not watch: " + dir + " — " + e.getMessage());
                }
            });
        }
    }
}
