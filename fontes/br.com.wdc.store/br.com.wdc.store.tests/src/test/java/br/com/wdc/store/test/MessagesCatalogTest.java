package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import br.com.wdc.store.i18n.Message;
import br.com.wdc.store.i18n.Messages;
import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.scripts.codegen.GenerateMessages;

/**
 * The UI text catalog: every language has every key with the same placeholders ({@link GenerateMessages#read} checks
 * it), the generated code of every technology matches the catalog, and a message is shown in the user's language.
 */
public class MessagesCatalogTest {

    /** {@code fontes/}, from this module's folder. */
    static final Path FONTES = Path.of("../..").toAbsolutePath().normalize();

    @Test
    public void theGeneratedCodeMatchesTheCatalog() throws IOException {
        for (var file : GenerateMessages.render(FONTES).entrySet()) {
            var onDisk = Files.readString(FONTES.resolve(file.getKey()), StandardCharsets.UTF_8);
            assertEquals(file.getKey() + " is stale: run GenerateMessages", file.getValue(), onDisk);
        }
    }

    @Test
    public void everyLanguageHasEveryKey() {
        var catalog = GenerateMessages.read(FONTES.resolve(GenerateMessages.CATALOG));
        var keys = catalog.get("en-US").keySet();
        assertTrue(keys.size() > 50);
        assertEquals(keys, catalog.get("pt-BR").keySet());
    }

    @Test
    public void aMessageIsShownInTheUsersLanguage() {
        Message message = Messages.errorSigninTooManySeconds(30);
        assertEquals("error.signin.tooManySeconds", message.key());
        assertEquals("30", message.args().get("seconds"));
        assertEquals("Too many failed sign-ins. Try again in 30 s.", message.in("en-US"));
        assertEquals("Muitas tentativas de entrada sem sucesso. Tente de novo em 30 s.", message.in("pt-BR"));
        assertEquals("a language from its family", message.in("pt-BR"), message.in("pt-PT"));
        assertEquals("the default for any other", message.in("en-US"), message.in("fr-FR"));
    }

    @Test
    public void languagesAreChosenFromTags() {
        assertEquals("pt-BR", StoreMessages.language("pt"));
        assertEquals("pt-BR", StoreMessages.language("pt_BR"));
        assertEquals("en-US", StoreMessages.language("en-GB"));
        assertEquals("en-US", StoreMessages.language(null));
        assertEquals("dd/MM/yyyy", StoreMessages.text("pt-BR", StoreMessages.FORMAT_DATE));
        assertEquals("a key the catalog lacks shows as itself", "no.such.key", StoreMessages.text("en-US", "no.such.key"));
    }
}
