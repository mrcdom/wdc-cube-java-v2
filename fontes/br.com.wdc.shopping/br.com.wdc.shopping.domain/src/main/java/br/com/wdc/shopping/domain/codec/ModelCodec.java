package br.com.wdc.shopping.domain.codec;

import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.EntityGraph;
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
     * Resultado do parse de entidade para update: entidade + projeção (campos presentes no JSON).
     */
    record UpdateData<T>(T entity, T projection) {
    }

    /**
     * Escreve a entidade como um objeto JSON completo (beginObject + campos + endObject).
     */
    void writeEntity(ExtensibleObjectOutput out, E entity);

    /**
     * Escreve a entidade com rastreamento de grafo — entidades já vistas são escritas apenas com a chave.
     * Implementações devem usar {@link EntityGraph#track(br.com.wdc.framework.commons.serialization.KeyedEntity)} para
     * detectar repetições antes de serializar campos aninhados.
     */
    void writeEntity(ExtensibleObjectOutput out, E entity, EntityGraph graph);

    /**
     * Escreve apenas os campos da entidade indicados pela projeção (campos não-nulos na projeção).
     * O ID é sempre incluído.
     */
    void writeEntityProjected(ExtensibleObjectOutput out, E newEntity, E oldEntity, E projection);

    /**
     * Lê uma entidade a partir da posição atual (espera beginObject).
     */
    E readEntity(ExtensibleObjectInput in);

    /**
     * Lê uma entidade para update, retornando simultaneamente a entidade e a projeção
     * (campos presentes no JSON recebem marker não-nulo na projeção).
     */
    UpdateData<E> readEntityForUpdate(ExtensibleObjectInput in);

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
     * Lê os campos do critério a partir do nome do campo corrente (sem begin/endObject).
     * Retorna true se o campo foi consumido, false se não é um campo de critério reconhecido.
     */
    boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, C criteria);

    /**
     * Retorna a projeção associada ao critério (pode ser null).
     */
    E getProjection(C criteria);

    /**
     * Define o ID gerado pelo servidor na entidade após insert.
     */
    void setGeneratedId(E entity, long id);
}
