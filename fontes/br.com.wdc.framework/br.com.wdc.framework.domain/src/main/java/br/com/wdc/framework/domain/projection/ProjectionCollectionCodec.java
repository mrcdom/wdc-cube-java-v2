package br.com.wdc.framework.domain.projection;

import java.util.Collection;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;

/**
 * Como uma <b>coleção projetada</b> de relação 1:N trafega.
 *
 * <p>
 * Uma coleção de projeção não é uma lista de resultados: é <b>uma</b> forma de item mais o que fazer com a coleção —
 * o critério que a filtra ({@link HasCriteria}) e o recorte a aplicar ({@link HasSlice}). Esses três dados precisam
 * chegar ao outro lado, ou a coleção volta inteira e desordenada — foi o que faltava no transporte REST.
 * </p>
 *
 * <p>
 * <b>A forma no fio distingue projeção de resultado por si só.</b> A projeção é um objeto; o resultado, o array de
 * sempre:
 * </p>
 *
 * <pre>
 * // projeção (cliente → servidor)
 * "items": { "shape": {…um item…}, "where": {…critério…}, "limit": 5, "offset": 0 }
 *
 * // resultado (servidor → cliente) — inalterado
 * "items": [ {…}, {…}, {…} ]
 * </pre>
 *
 * <p>
 * O leitor decide pelo token: objeto é projeção, array é resultado. Assim o mesmo campo {@code items} serve os dois
 * sentidos sem um segundo nome, e o resultado segue exatamente como era.
 * </p>
 *
 * <p>
 * O critério embutido usa a mesma estrutura expressiva do critério de topo — é o codec da entidade filha quem o
 * escreve e lê, através dos callbacks. Os dois codecs colaboram: este cuida do envelope da coleção, aquele do
 * conteúdo do critério.
 * </p>
 */
public final class ProjectionCollectionCodec {

    /** Escreve a forma de um item da coleção. */
    @FunctionalInterface
    public interface ShapeWriter<E> {
        void write(ExtensibleObjectOutput out, E item);
    }

    /** Lê a forma de um item da coleção. */
    @FunctionalInterface
    public interface ShapeReader<E> {
        E read(ExtensibleObjectInput in);
    }

    /** Escreve o critério embutido — objeto completo, {@code begin/endObject} inclusos. */
    @FunctionalInterface
    public interface CriteriaWriter {
        void write(ExtensibleObjectOutput out, Object criteria);
    }

    /** Lê o critério embutido — objeto completo, {@code begin/endObject} inclusos. */
    @FunctionalInterface
    public interface CriteriaReader {
        Object read(ExtensibleObjectInput in);
    }

    private ProjectionCollectionCodec() {
        // NOOP
    }

    /**
     * Escreve a coleção projetada como envelope {@code { shape, where?, limit?, offset? }}.
     *
     * <p>
     * Só a forma é obrigatória — é o primeiro elemento da coleção. Critério e recorte entram quando a coleção os
     * carrega; ausentes, a chave nem aparece.
     * </p>
     */
    public static <E> void write(ExtensibleObjectOutput out, String name, Collection<E> collection,
            ShapeWriter<E> shapeWriter, CriteriaWriter criteriaWriter) {

        var shape = collection.iterator().next();

        out.name(name).beginObject();

        out.name("shape");
        shapeWriter.write(out, shape);

        if (collection instanceof HasCriteria hc && hc.getCriteria() != null && criteriaWriter != null) {
            out.name("where");
            criteriaWriter.write(out, hc.getCriteria());
        }
        if (collection instanceof HasSlice hs) {
            if (hs.getLimit() != null) {
                out.name("limit").value(hs.getLimit().longValue());
            }
            if (hs.getOffset() != null) {
                out.name("offset").value(hs.getOffset().longValue());
            }
        }

        out.endObject();
    }

    /** Se o valor corrente do campo é uma coleção projetada (objeto), e não uma lista de resultados (array). */
    public static boolean isProjectionEnvelope(ExtensibleObjectInput in) {
        return in.peek() == br.com.wdc.framework.commons.serialization.SerializationToken.BEGIN_OBJECT;
    }

    /**
     * Lê o envelope e devolve a coleção de projeção pronta — forma, critério e recorte reidratados.
     */
    public static <E> ProjectionList<E> read(ExtensibleObjectInput in, ShapeReader<E> shapeReader,
            CriteriaReader criteriaReader) {

        E shape = null;
        Object criteria = null;
        Integer limit = null;
        Integer offset = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
            case "shape" -> shape = shapeReader.read(in);
            case "where" -> criteria = criteriaReader.read(in);
            case "limit" -> limit = InputCoerceUtils.asInteger(in);
            case "offset" -> offset = InputCoerceUtils.asInteger(in);
            default -> in.skipValue();
            }
        }
        in.endObject();

        var list = new ProjectionList<E>(shape, criteria);
        if (limit != null) {
            list.withLimit(limit);
        }
        if (offset != null) {
            list.withOffset(offset);
        }
        return list;
    }
}
