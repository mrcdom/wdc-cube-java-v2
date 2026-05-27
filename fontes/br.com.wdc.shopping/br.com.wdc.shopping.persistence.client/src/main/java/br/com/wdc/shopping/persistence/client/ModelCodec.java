package br.com.wdc.shopping.persistence.client;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Contrato de serialização/deserialização para entidades e critérios de um repositório.
 * <p>
 * Permite abstrair a tecnologia de serialização (Gson reflection vs. parsing manual)
 * mantendo uma única implementação de repositório HTTP ({@link HttpRepository}).
 *
 * @param <E> tipo da entidade
 * @param <C> tipo do critério de pesquisa
 */
public interface ModelCodec<E, C> {

    JsonObject entityToJson(E entity);

    E entityFromJson(JsonObject json);

    List<E> entityListFromJson(JsonArray array);

    JsonObject criteriaToJson(C criteria);

    E getProjection(C criteria);

    void addProjection(JsonObject body, E projection);

    void setGeneratedId(E entity, long id);
}
