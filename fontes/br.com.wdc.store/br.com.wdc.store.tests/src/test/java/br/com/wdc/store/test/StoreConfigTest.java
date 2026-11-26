package br.com.wdc.store.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import br.com.wdc.store.domain.StoreConfig;

/** The hosts' explicit work directory: from the argument or the variable, never guessed. */
public class StoreConfigTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void theArgumentWins_overTheVariable() throws IOException {
        var fromArg = folder.newFolder("arg").toPath();
        var fromEnv = folder.newFolder("env").toPath();
        assertEquals(fromArg, StoreConfig.resolveWorkDir(new String[] { "8080", "--workdir=" + fromArg },
                Map.of(StoreConfig.WORKDIR_ENV, fromEnv.toString())));
        assertEquals(fromEnv, StoreConfig.resolveWorkDir(new String[] { "8080" },
                Map.of(StoreConfig.WORKDIR_ENV, fromEnv.toString())));
    }

    @Test
    public void withoutOne_orWithAFolderThatDoesNotExist_theHostDoesNotStart() {
        var none = assertThrows(IllegalStateException.class, () -> StoreConfig.resolveWorkDir(new String[0], Map.of()));
        assertTrue(none.getMessage(), none.getMessage().contains("--workdir=") && none.getMessage().contains("STORE_WORKDIR"));

        var missing = folder.getRoot().toPath().resolve("nowhere");
        var e = assertThrows(IllegalStateException.class,
                () -> StoreConfig.resolveWorkDir(new String[] { "--workdir=" + missing }, Map.of()));
        assertTrue(e.getMessage(), e.getMessage().contains("does not exist"));
    }

    @Test
    public void theHostReadsItsOwnArguments_withoutTheWorkDirectory() {
        assertArrayEquals(new String[] { "8080" }, StoreConfig.withoutWorkDir(new String[] { "--workdir=/x", "8080" }));
    }

    @Test
    public void loadingReadsConfig_andMakesTheRuntimeFolders() throws IOException {
        var dir = folder.newFolder("work").toPath();
        Files.createDirectories(dir.resolve("config"));
        Files.writeString(dir.resolve("config/application.toml"), "[server]\nport = 9123\n");

        var config = StoreConfig.Internals.load(dir);
        assertEquals(9123, config.getInt("server.port", 0));
        assertEquals(dir.resolve("data"), StoreConfig.getDataDir());
        assertEquals(dir.resolve("frontend"), StoreConfig.getFrontendDir());
        assertTrue(Files.isDirectory(dir.resolve("data")) && Files.isDirectory(dir.resolve("log"))
                && Files.isDirectory(dir.resolve("temp")));

        var empty = folder.newFolder("empty").toPath();
        var e = assertThrows(IllegalStateException.class, () -> StoreConfig.Internals.load(empty));
        assertTrue(e.getMessage(), e.getMessage().contains("application.toml"));
    }
}
