package br.com.wdc.framework.jooq;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.SQLDialect;

import br.com.wdc.framework.jooq.dialect.DuckDbJsonDialect;
import br.com.wdc.framework.jooq.dialect.GenericJsonDialect;
import br.com.wdc.framework.jooq.dialect.H2JsonDialect;
import br.com.wdc.framework.jooq.dialect.MySqlJsonDialect;
import br.com.wdc.framework.jooq.dialect.PostgresJsonDialect;
import br.com.wdc.framework.jooq.dialect.SQLiteJsonDialect;
import br.com.wdc.framework.jooq.dialect.SqlServerJsonDialect;

/**
 * Abstraction for generating JSON through each database's native SQL.
 *
 * <p>
 * Each implementation uses the native JSON functions of its database (e.g. {@code JSON_OBJECT},
 * {@code json_build_object}, etc.), supporting any number of fields.
 * </p>
 *
 * <p>
 * Usage:
 * <pre>{@code
 * var dialect = JsonDialect.of(ctx.dsl().dialect());
 * Field<String> json = dialect.jsonObject(entries);
 * }</pre>
 */
public interface JsonDialect {

    /**
     * Builds an SQL field that produces a JSON object from the given entries.
     * Each entry holds the key, the raw field and the logical type.
     *
     * @param entries list of fields to include in the JSON object
     * @return Field that, when evaluated, produces a JSON string
     */
    Field<String> jsonObject(List<JsonFieldEntry> entries);

    /**
     * Builds an aggregation expression that produces a JSON array from multiple rows.
     * Used for 1:N relations (correlated subselect).
     *
     * @param jsonElement expression that produces one JSON object per row
     * @return expression that aggregates into {@code '[' || elem1 || ',' || elem2 || ... || ']'}
     */
    Field<String> jsonArrayAgg(Field<String> jsonElement);

    /**
     * Same, with the order defined <b>inside</b> the aggregation.
     *
     * <p>
     * The order has to go in here, not as the query's {@code ORDER BY}: the collection comes out of a subquery
     * correlated with the parent row, and wrapping it in a derived table — where an {@code ORDER BY} would fit — puts the
     * correlation out of reach, because a derived table does not see the outer scope.
     * </p>
     *
     * <p>
     * The default returns the aggregation <b>without order</b>: not every database can order inside the aggregate, and
     * emitting SQL it would refuse would be worse than returning the collection in the database's order. The dialects
     * where the construct exists override this method — H2 and PostgreSQL, the two the application uses.
     * </p>
     */
    default Field<String> jsonArrayAgg(Field<String> jsonElement, List<SortField<?>> order) {
        return jsonArrayAgg(jsonElement);
    }

    /**
     * Whether this dialect can order inside the aggregation — that is, whether {@link #jsonArrayAgg(Field, List)} honors
     * the order.
     */
    default boolean supportsOrderedAggregation() {
        return false;
    }

    /**
     * Initialization hook (e.g. registering custom SQL functions in H2).
     * Called once during bootstrap.
     *
     * @param conn open JDBC connection
     */
    default void initialize(Connection conn) throws SQLException {
        // Default: nothing to do
    }

    /**
     * Gets the {@link JsonDialect} implementation suited to the given jOOQ dialect.
     */
    static JsonDialect of(SQLDialect dialect) {
        return switch (dialect.family()) {
            case H2, HSQLDB -> H2JsonDialect.INSTANCE;
            case POSTGRES, YUGABYTEDB -> PostgresJsonDialect.INSTANCE;
            case MYSQL, MARIADB -> MySqlJsonDialect.INSTANCE;
            case SQLITE -> SQLiteJsonDialect.INSTANCE;
            case DUCKDB -> DuckDbJsonDialect.INSTANCE;
            default -> {
                // SQL Server (SQLSERVER) is available only in commercial jOOQ.
                // We detect it by the family name so as not to depend on the pro enum.
                String family = dialect.family().name();
                if (family.equals("SQLSERVER")) {
                    yield SqlServerJsonDialect.INSTANCE;
                }
                yield GenericJsonDialect.INSTANCE;
            }
        };
    }
}
