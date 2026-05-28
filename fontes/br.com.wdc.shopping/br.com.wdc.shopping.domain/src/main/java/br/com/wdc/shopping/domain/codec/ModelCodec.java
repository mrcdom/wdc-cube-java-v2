package br.com.wdc.shopping.domain.codec;

import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;

/**
 * Contrato de serialização/deserialização para entidades e critérios de um repositório.
 * <p>
 * Usa {@link ExtensibleObjectOutput}/{@link ExtensibleObjectInput} para serialização
 * streaming, compatível com JVM e TeaVM sem dependência de Gson/reflection.
 *
 * @param <E> tipo da entidade
 * @param <C> tipo do critério de pesquisa
 */
public interface ModelCodec<E, C> {

    /**
     * Escreve a entidade como um objeto JSON completo (beginObject + campos + endObject).
     */
    void writeEntity(ExtensibleObjectOutput out, E entity);

    /**
     * Escreve apenas os campos da entidade indicados pela projeção (campos não-nulos na projeção).
     * O ID é sempre incluído.
     */
    default void writeEntityProjected(ExtensibleObjectOutput out, E newEntity, E oldEntity, E projection) {
        writeEntity(out, newEntity);
    }

    /**
     * Lê uma entidade a partir da posição atual (espera beginObject).
     */
    E readEntity(ExtensibleObjectInput in);

    /**
     * Lê uma lista de entidades (espera beginArray).
     */
    default List<E> readEntityList(ExtensibleObjectInput in) {
        var list = new ArrayList<E>();
        in.beginArray();
        while (in.hasNext()) {
            list.add(readEntity(in));
        }
        in.endArray();
        return list;
    }

    /**
     * Escreve os campos do critério no objeto corrente (sem begin/endObject).
     */
    void writeCriteriaFields(ExtensibleObjectOutput out, C criteria);

    /**
     * Retorna a projeção associada ao critério (pode ser null).
     */
    E getProjection(C criteria);

    /**
     * Define o ID gerado pelo servidor na entidade após insert.
     */
    void setGeneratedId(E entity, long id);
}
