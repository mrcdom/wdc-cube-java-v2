package br.com.wdc.framework.commons.serialization;

/**
 * Contrato para entidades que possuem uma chave de identidade.
 * <p>
 * Permite que mecanismos de serialização detectem referências repetidas no grafo de objetos
 * e substituam por stubs contendo apenas a chave, evitando recursão infinita.
 */
public interface KeyedEntity {

    /**
     * Retorna a chave de identidade da entidade (tipicamente o ID).
     * Pode ser {@code null} para entidades ainda não persistidas.
     */
    Object key();
}
