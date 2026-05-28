package br.com.wdc.shopping.scripts.sgbd;

import java.sql.DriverManager;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.Configuration;
import org.jooq.meta.jaxb.Database;
import org.jooq.meta.jaxb.Generator;
import org.jooq.meta.jaxb.Jdbc;
import org.jooq.meta.jaxb.Target;

/**
 * Generates jOOQ classes from the project's own schema definition.
 *
 * <p>Strategy: creates an in-memory H2, runs all DDL via {@link DBCreate},
 * then invokes jOOQ's code generator against that database.</p>
 *
 * <p>Run from the {@code fontes/} directory:</p>
 * <pre>
 * mvn exec:java -pl br.com.wdc.shopping/br.com.wdc.shopping.scripts \
 *     -Dexec.mainClass="br.com.wdc.shopping.scripts.sgbd.GenerateJooqSchema"
 * </pre>
 */
public class GenerateJooqSchema {

	private static final String JDBC_URL = "jdbc:h2:mem:jooq_codegen;DB_CLOSE_DELAY=-1";
	private static final String OUTPUT_PACKAGE = "br.com.wdc.shopping.persistence.jooq";
	private static final String OUTPUT_DIR = "br.com.wdc.shopping/br.com.wdc.shopping.persistence/src/main/java";

	public static void main(String[] args) throws Exception {
		System.out.println("=== jOOQ Schema Generation ===");
		System.out.println("Working directory: " + System.getProperty("user.dir"));

		// 1. Create in-memory H2 and build the full schema
		System.out.println("Creating in-memory database and applying schema...");
		try (var connection = DriverManager.getConnection(JDBC_URL, "sa", "")) {
			new DBCreate()
					.withConnection(connection)
					.run();
		}

		// Resolve output dir (allow override via system property)
		var outputDir = System.getProperty("jooq.output.dir", OUTPUT_DIR);

		// 2. Run jOOQ code generation against the populated H2
		System.out.println("Generating jOOQ classes to: " + outputDir);
		var config = new Configuration()
				.withJdbc(new Jdbc()
						.withDriver("org.h2.Driver")
						.withUrl(JDBC_URL)
						.withUser("sa")
						.withPassword(""))
				.withGenerator(new Generator()
						.withDatabase(new Database()
								.withName("org.jooq.meta.h2.H2Database")
								.withInputSchema("PUBLIC")
								.withIncludes(".*")
								.withExcludes("EN_MIGRATION_LOG"))
						.withTarget(new Target()
								.withPackageName(OUTPUT_PACKAGE)
								.withDirectory(outputDir)));

		GenerationTool.generate(config);

		System.out.println("=== Done! Generated to: " + outputDir + " (" + OUTPUT_PACKAGE + ") ===");
	}
}
