package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

import br.com.wdc.store.design.StoreTokens;
import br.com.wdc.store.scripts.codegen.GenerateDesignTokens;

/** The design tokens: the generated code of every technology matches {@code tokens.json}. */
public class DesignTokensTest {

    @Test
    public void theGeneratedCodeMatchesTheTokens() throws IOException {
        for (var file : GenerateDesignTokens.render(MessagesCatalogTest.FONTES).entrySet()) {
            var onDisk = Files.readString(MessagesCatalogTest.FONTES.resolve(file.getKey()), StandardCharsets.UTF_8);
            assertEquals(file.getKey() + " is stale: run GenerateDesignTokens", file.getValue(), onDisk);
        }
    }

    @Test
    public void theBrandIsTheWebShellsBlue() {
        assertEquals(0x0d66d0, StoreTokens.COLOR_PRIMARY);
        assertEquals("#0d66d0", StoreTokens.COLOR_PRIMARY_HEX);
        assertEquals(16, StoreTokens.SPACE_MD);
        assertTrue(StoreTokens.CSS_VARIABLES.contains("--store-color-primary: #0d66d0;"));
    }
}
