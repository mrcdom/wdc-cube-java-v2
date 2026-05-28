package br.com.wdc.shopping.scripts.sgbd;

import java.sql.DriverManager;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generate;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;

import br.com.wdc.framework.commons.log.Log;

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
 *     -Dexec.mainClass="br.com.wdc.shopping.scripts.sgbd.GenerateJooqSchema"
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
    private static final String OUTPUT_PACKAGE = "br.com.wdc.shopping.persistence.jooq";
    private static final String OUTPUT_DIR = "br.com.wdc.shopping/br.com.wdc.shopping.persistence/src/main/java";

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
        var outputDir = System.getProperty("jooq.output.dir", OUTPUT_DIR);

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
                                .withExcludes("EN_MIGRATION_LOG"))
                        .withGenerate(new Generate()
                                .withRecords(false))
                        .withTarget(new Target()
                                .withPackageName(OUTPUT_PACKAGE)
                                .withDirectory(outputDir)));

        GenerationTool.generate(config);

        LOG.info("=== Done! Generated to: " + outputDir + " (" + OUTPUT_PACKAGE + ") ===");
    }
}
