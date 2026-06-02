package br.com.wdc.framework.jooq;

import org.jooq.Field;

/**
 * Entrada de campo para montagem de JSON object pelo {@link JsonDialect}.
 *
 * @param key   nome da chave JSON
 * @param field campo jOOQ raw (não pré-formatado)
 * @param type  tipo lógico para serialização
 */
public record JsonFieldEntry(String key, Field<?> field, JsonFieldType type) {
}
