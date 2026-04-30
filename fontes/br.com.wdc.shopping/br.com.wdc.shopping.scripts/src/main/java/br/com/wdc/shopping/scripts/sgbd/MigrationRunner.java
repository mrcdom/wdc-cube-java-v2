package br.com.wdc.shopping.scripts.sgbd;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.persistence.sql.SqlKeywords;
import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.sql.SqlUtils;
import br.com.wdc.shopping.scripts.sgbd.schema.EnMigrationLog;

/**
 * Executes migration scripts and records each completed step in EN_MIGRATION_LOG.
 *
 * <p>Usage:</p>
 * <pre>
 * new MigrationRunner(connection)
 *     .run(new Migration_0001_InitialSetup(connection))
 *     .run(new Migration_0002_AddStatus(connection));
 * </pre>
 *
 * <p>Each public method whose name starts with "step" (case-insensitive) is treated as a migration step.
 * Steps are sorted alphabetically and executed in order. Already-executed steps are skipped.</p>
 */
public class MigrationRunner implements SqlKeywords {

	private static final Logger LOG = LoggerFactory.getLogger(MigrationRunner.class);

	private static final String STEP_PREFIX = "step";

	private final Connection connection;

	public MigrationRunner(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Runs all step methods of the given migration script that haven't been executed yet.
	 */
	public MigrationRunner run(Object migrationScript) throws SQLException {
		var scriptName = migrationScript.getClass().getSimpleName();
		var executedSteps = loadExecutedSteps(scriptName);

		var stepMethods = Arrays.stream(migrationScript.getClass().getDeclaredMethods())
				.filter(m -> Modifier.isPublic(m.getModifiers()))
				.filter(m -> m.getParameterCount() == 0)
				.filter(m -> m.getName().toLowerCase().startsWith(STEP_PREFIX))
				.sorted(Comparator.comparingInt(MigrationRunner::extractStepNumber))
				.toList();

		for (var method : stepMethods) {
			var stepName = method.getName();

			if (executedSteps.contains(stepName)) {
				LOG.debug("Skipping already executed step: {}.{}", scriptName, stepName);
				continue;
			}

			LOG.info("Executing migration step: {}.{}", scriptName, stepName);
			try {
				method.invoke(migrationScript);
				recordStep(scriptName, stepName);
				LOG.info("Completed migration step: {}.{}", scriptName, stepName);
			} catch (Exception e) {
				throw new SQLException("Migration step failed: " + scriptName + "." + stepName, e);
			}
		}

		return this;
	}

	private Set<String> loadExecutedSteps(String scriptName) throws SQLException {
		var en = EnMigrationLog.INSTANCE;

		var sql = new SqlList();
		sql.ln(SELECT);
		var fStepName = sql.strColumn(en.stepName);
		sql.ln(FROM, en.tableName());
		sql.ln(WHERE, en.scriptName, EQUAL, "?");

		var steps = new HashSet<String>();
		try (var ps = connection.prepareStatement(sql.toText())) {
			ps.setString(1, scriptName);
			try (var rs = ps.executeQuery()) {
				while (rs.next()) {
					steps.add(fStepName.apply(rs));
				}
			}
		}
		return steps;
	}

	private void recordStep(String scriptName, String stepName) throws SQLException {
		var en = EnMigrationLog.INSTANCE;
		var nextId = SqlUtils.nextSequence(connection, "SQ_MIGRATION_LOG");

		var sql = new SqlList();
		sql.ln(INSERT_INTO, en.tableName(), "(");
		sql.ln(" ", en.id);
		sql.ln(",", en.scriptName);
		sql.ln(",", en.stepName);
		sql.ln(",", en.executedAt);
		sql.ln(")");
		sql.ln(VALUES);
		sql.ln("(?, ?, ?, ?)");

		try (var ps = connection.prepareStatement(sql.toText())) {
			ps.setLong(1, nextId);
			ps.setString(2, scriptName);
			ps.setString(3, stepName);
			ps.setTimestamp(4, Timestamp.from(Instant.now()));
			ps.executeUpdate();
		}
	}

	private static int extractStepNumber(Method method) {
		var name = method.getName();
		var afterPrefix = name.substring(STEP_PREFIX.length());
		var digits = new StringBuilder();
		for (var ch : afterPrefix.toCharArray()) {
			if (Character.isDigit(ch)) {
				digits.append(ch);
			} else {
				break;
			}
		}
		return digits.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(digits.toString());
	}
}
