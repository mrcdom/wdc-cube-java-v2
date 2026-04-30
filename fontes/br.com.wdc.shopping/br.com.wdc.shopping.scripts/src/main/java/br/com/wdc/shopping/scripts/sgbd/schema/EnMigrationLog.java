package br.com.wdc.shopping.scripts.sgbd.schema;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.StringBuilderWriter;

import br.com.wdc.shopping.persistence.schema.support.DbField;
import br.com.wdc.shopping.persistence.schema.support.DbTable;

public class EnMigrationLog extends DbTable {

	public static final EnMigrationLog INSTANCE = new EnMigrationLog("");

	public final DbField id;
	public final DbField scriptName;
	public final DbField stepName;
	public final DbField executedAt;

	private final List<DbField> fields;

	public EnMigrationLog(String alias) {
		super(alias);

		this.id = mkBigint("ID", false);
		this.scriptName = mkVarChar("SCRIPT_NAME", 255, false);
		this.stepName = mkVarChar("STEP_NAME", 255, false);
		this.executedAt = mkTimestamp("EXECUTED_AT", false);

		this.fields = Arrays.asList(id, scriptName, stepName, executedAt);
	}

	@Override
	public String tableName() {
		return "EN_MIGRATION_LOG";
	}

	@Override
	public List<DbField> fields() {
		return this.fields;
	}

	@Override
	public String createTableSql() {
		var sql = new StringBuilderWriter();

		try (var out = new PrintWriter(sql)) {
			out.println("CREATE TABLE IF NOT EXISTS " + this.tableName() + " (");
			out.println(" " + this.id.declaration());
			out.println("," + this.scriptName.declaration());
			out.println("," + this.stepName.declaration());
			out.println("," + this.executedAt.declaration());
			out.println(",CONSTRAINT PK_MIGRATION_LOG PRIMARY KEY (" + this.id.name() + ")");
			out.println(")");
		}

		return sql.toString();
	}

	@Override
	public String createSequeceSql() {
		return "CREATE SEQUENCE IF NOT EXISTS SQ_MIGRATION_LOG START WITH 1 INCREMENT BY 1";
	}
}
