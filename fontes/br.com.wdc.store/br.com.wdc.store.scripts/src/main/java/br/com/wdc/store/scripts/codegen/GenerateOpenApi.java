package br.com.wdc.store.scripts.codegen;

import java.nio.file.Path;
import java.util.Map;

import br.com.wdc.store.persistence.rest.doc.RepositoryApiDocs;

/**
 * Writes the REST API's OpenAPI document ({@link RepositoryApiDocs}) to {@code docs/openapi.json}: the contract of the
 * API, committed, so that every change to it — a field removed from the domain and from its documentation at once
 * included — shows in a diff and is accepted on purpose. {@code OpenApiSpecTest} fails while the file does not match
 * what the code describes.
 *
 * <pre>
 * mvn exec:java -pl br.com.wdc.store/br.com.wdc.store.scripts \
 *     -Dexec.mainClass="br.com.wdc.store.scripts.codegen.GenerateOpenApi"
 * </pre>
 */
public final class GenerateOpenApi {

    /** The document, from {@code fontes/}. */
    public static final Path TARGET = Path.of("../docs/openapi.json");

    private GenerateOpenApi() {
    }

    public static void main(String[] args) {
        var fontes = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        for (var file : render(fontes).entrySet()) {
            GenerateMessages.write(fontes.resolve(file.getKey()).normalize(), file.getValue());
        }
    }

    /** The document (path relative to {@code fontes/} → content), for the API at {@code /api}. */
    public static Map<Path, String> render(Path fontes) {
        return Map.of(TARGET, RepositoryApiDocs.toJson("/api") + "\n");
    }
}
