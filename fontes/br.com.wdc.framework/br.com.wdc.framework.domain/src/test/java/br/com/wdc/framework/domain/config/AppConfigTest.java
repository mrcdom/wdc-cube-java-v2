package br.com.wdc.framework.domain.config;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

public class AppConfigTest {

    private static AppConfig of(String toml) throws IOException {
        var dir = Files.createTempDirectory("app-config");
        var file = dir.resolve("application.toml");
        Files.writeString(file, toml);
        try {
            return AppConfig.load(file);
        } finally {
            Files.delete(file);
            Files.delete(dir);
        }
    }

    @Test
    public void listOnOneLine() throws IOException {
        var config = of("""
                [cluster]
                peers = ["ws://store-a:8080", 'ws://store-b:8080' ,"ws://store-c:8080"]
                """);
        assertEquals(List.of("ws://store-a:8080", "ws://store-b:8080", "ws://store-c:8080"),
                config.getList("cluster.peers"));
    }

    @Test
    public void listAsCommaSeparatedString() throws IOException {
        var config = of("peers = \"ws://a:8080, ws://b:8080\"\n");
        assertEquals(List.of("ws://a:8080", "ws://b:8080"), config.getList("peers"));
    }

    @Test
    public void missingOrEmptyList() throws IOException {
        var config = of("empty = []\n");
        assertEquals(List.of(), config.getList("empty"));
        assertEquals(List.of(), config.getList("missing"));
    }
}
