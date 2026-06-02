package br.com.wdc.framework.jooq;

/**
 * Tipo lógico de um campo no mapeamento JSON.
 */
public enum JsonFieldType {
    /** BIGINT, INT, DOUBLE, DECIMAL — sem aspas no JSON. */
    NUMBER,
    /** VARCHAR, CHAR, enum — com aspas e escape no JSON. */
    STRING,
    /** BOOLEAN — true/false no JSON. */
    BOOLEAN,
    /** TIMESTAMP, TIMESTAMPTZ — serializado como string ISO-8601. */
    DATETIME,
    /** BINARY/BLOB — serializado como string Base64. */
    BINARY,
    /** Já é JSON válido (subqueries de relações). Incluído sem formatação adicional. */
    RAW_JSON
}
