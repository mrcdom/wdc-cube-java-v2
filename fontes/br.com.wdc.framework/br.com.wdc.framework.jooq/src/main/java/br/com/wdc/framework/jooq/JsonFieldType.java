package br.com.wdc.framework.jooq;

/**
 * Logical type of a field in the JSON mapping.
 */
public enum JsonFieldType {
    /** BIGINT, INT, DOUBLE, DECIMAL — unquoted in the JSON. */
    NUMBER,
    /** VARCHAR, CHAR, enum — quoted and escaped in the JSON. */
    STRING,
    /** BOOLEAN — true/false in the JSON. */
    BOOLEAN,
    /** TIMESTAMP, TIMESTAMPTZ — serialized as an ISO-8601 string. */
    DATETIME,
    /** BINARY/BLOB — serialized as a Base64 string. */
    BINARY,
    /** Already valid JSON (relation subqueries). Included without further formatting. */
    RAW_JSON
}
