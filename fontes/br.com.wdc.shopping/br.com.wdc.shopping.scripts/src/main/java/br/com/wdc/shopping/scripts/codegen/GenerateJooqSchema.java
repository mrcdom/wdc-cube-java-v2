package br.com.wdc.shopping.scripts.codegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.DriverManager;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;

/**
 * Generates jOOQ classes from the project's own schema definition.
 *
 * <p>
 * Strategy: creates an in-memory H2, runs all DDL via {@link DBCreate}, then invokes jOOQ's code generator against that
 * database.
 * </p>
 *
 * <p>
 * Run from the {@code fontes/} directory:
 * </p>
 * 
 * <pre>
 * mvn exec:java -pl br.com.wdc.shopping/br.com.wdc.shopping.scripts \
 *     -Dexec.mainClass="br.com.wdc.shopping.scripts.codegen.GenerateJooqSchema"
 * </pre>
 */
@SuppressWarnings({
        // ephemeral in-memory H2 for code generation only — no real credentials
        "java:S2068",
        "java:S6437"
})
public class GenerateJooqSchema {

    private static final Log LOG = Log.getLogger(GenerateJooqSchema.class.getName());

    private static final String JDBC_URL = "jdbc:h2:mem:jooq_codegen;DB_CLOSE_DELAY=-1";
    private static final String JDBC_PASSWORD = "codegen";

    public static void main(String[] args) throws Exception {
        LOG.info("=== jOOQ Schema Generation ===");
        LOG.info("Working directory: " + System.getProperty("user.dir"));

        // 1. Create in-memory H2 and build the full schema
        LOG.info("Creating in-memory database and applying schema...");
        try (var connection = DriverManager.getConnection(JDBC_URL, "sa", JDBC_PASSWORD)) {
            new DBCreate()
                    .withConnection(connection)
                    .withSkipReset()
                    .run();
        }

        // Resolve output dir (allow override via system property)
        var refClass = br.com.wdc.shopping.persistence.impl.scheme.Tables.class;
        var packageName = refClass.getPackageName();
        var outputDir = resolveOutputDir(refClass);

        // 2. Run jOOQ code generation against the populated H2
        LOG.info("Generating jOOQ classes to: " + outputDir);
        var config = new Configuration()
                .withJdbc(new Jdbc()
                        .withDriver("org.h2.Driver")
                        .withUrl(JDBC_URL)
                        .withUser("sa")
                        .withPassword(JDBC_PASSWORD))
                .withGenerator(new Generator()
                        .withDatabase(new Database()
                                .withName("org.jooq.meta.h2.H2Database")
                                .withInputSchema("PUBLIC")
                                .withIncludes(".*")
                                .withExcludes("EN_MIGRATION_LOG")
                                .withIncludeForeignKeys(false))
                        .withGenerate(new Generate()
                                .withDeprecated(false)
                                .withRecords(false)
                                .withPojos(false)
                                .withDaos(false)
                                .withInterfaces(false)
                                // Gera apenas o essencial: campos, schema, nome de tabela e sequências.
                                // Chaves (PK/UK) e índices são excluídos para manter as classes compactas.
                                .withSequences(true)
                                .withKeys(false)
                                .withRelations(false)
                                .withIndexes(false)
                                .withComments(false)
                                .withJavadoc(false))
                        .withTarget(new Target()
                                .withPackageName(packageName)
                                .withDirectory(outputDir.toAbsolutePath().toString())));

        GenerationTool.generate(config);
        prependFormatterOff(outputDir, packageName);

        LOG.info("=== Done! Generated to: " + outputDir + " (" + packageName + ") ===");
    }

    /**
     * Prepende {@code // @formatter:off} na primeira linha de cada {@code .java} gerado sob {@code outputDir}.
     * Evita que o formatter do Eclipse reformate as classes geradas pelo JOOQ.
     */
    private static void prependFormatterOff(Path outputDir, String pkgName) throws IOException {
        Path schemaDir = outputDir.resolve(pkgName.replace('.', '/'));
        try (var paths = Files.walk(schemaDir)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                    .forEach(file -> {
                        try {
                            String content = Files.readString(file);
                            if (!content.startsWith("// @formatter:off")) {
                                Files.writeString(file, "// @formatter:off\n" + content);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    /**
     * Calcula o diretório de saída percorrendo a árvore de diretórios de forma ascendente a partir do
     * {@code target/classes} da classe de referência ({@code refClass}) até encontrar o {@code src/main/java} do módulo
     * {@code persistence.impl}.
     *
     * <p>
     * Não depende de profundidade fixa (ex: {@code ../../}), portanto sobrevive a refatorações de estrutura de módulos
     * Maven.
     * </p>
     *
     * @throws IllegalStateException se {@code src/main/java} não for encontrado
     */
    private static Path resolveOutputDir(Class<?> refClass) throws Exception {
        URI location = refClass
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI();
        Path dir = Paths.get(location).toAbsolutePath().normalize();
        while (dir != null) {
            Path candidate = dir.resolve("src/main/java");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "[jOOQ Codegen] src/main/java não encontrado subindo a partir de: " +
                        Paths.get(location));
    }
}
