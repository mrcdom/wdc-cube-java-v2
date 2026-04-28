package br.com.wdc.shopping.business.impl.sgbd.dsl;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings({ "java:S1214", "java:S1845", "java:S100" })
public interface SqlKeywords {
    SqlKeywords INSTANCE = new SqlKeywords() {};

    String WITH = "WITH";
    String SELECT = "SELECT";
    String UPDATE = "UPDATE";
    String DELETE = "DELETE";
    String INSERT_INTO = "INSERT INTO";
    String VALUES = "VALUES";
    String SET = "SET";
    String COUNT = "COUNT";
    String DISTINCT = "DISTINCT";
    String FROM = "FROM";
    String JOIN = "JOIN";
    String WHERE = "WHERE";
    String WHERE_TRUE = "WHERE 1=1";
    String ON = "ON";
    String IN = "IN";
    String BETWEEN = "BETWEEN";
    String AND = "AND";
    String OR = "OR";
    String LIKE = "LIKE";
    String ORDER_BY = "ORDER BY";
    String ASC = "ASC";
    String DESC = "DESC";
    String HAVING = "HAVING";
    String LIMIT = "LIMIT";
    String OFFSET = "OFFSET";
    String NOT = "NOT";
    String AS = "AS";
    String UNION = "UNION";
    String UNION_ALL = "UNION ALL";

    String EQUAL = "=";
    String DIFFERENT = "<>";
    String GREATER_THAN = ">";
    String GREATER_OR_EQUAL = ">=";
    String LESS_THAN = "<";
    String LESS_OR_EQUAL = "<=";

    default String COUNT(Object... itens) {
        return "COUNT(" + StringUtils.join(itens, ' ') + ")";
    }

    default String IN(Object... itens) {
        return "IN(" + StringUtils.join(itens, ' ') + ")";
    }

    default String IN(SqlList sql) {
        return "IN(\n" + sql.toText("  ") + "  )";
    }

    default String IN(Consumer<SqlList> builder) {
        return "IN(\n" + SqlList.create(builder).toText("    ") + ")";
    }

    default String ON(Object... itens) {
        return "ON(" + StringUtils.join(itens, ' ') + ")";
    }

    default String BETWEEN(Object a, Object b) {
        return "BETWEEN " + a + " AND " + b;
    }

    default String EXISTS(SqlList sql) {
        return "EXISTS(\n" + sql.toText("    ") + ")";
    }

    default String EXISTS(Consumer<SqlList> builder) {
        return "EXISTS(\n" + SqlList.create(builder).toText("    ") + ")";
    }

    default String ORDER_BY(Object... itens) {
        return "ORDER BY " + StringUtils.join(itens, ", ");
    }

}
