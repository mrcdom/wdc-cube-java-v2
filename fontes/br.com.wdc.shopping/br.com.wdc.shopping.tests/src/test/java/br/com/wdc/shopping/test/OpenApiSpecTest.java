package br.com.wdc.shopping.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.wdc.shopping.persistence.rest.doc.RepositoryApiDocs;

/**
 * Guarda-corpo do documento OpenAPI.
 *
 * <p>
 * A especificação é montada em código, e código que só descreve não quebra quando o que ele descreve muda — foi assim
 * que o formato do critério e o da coleção projetada mudaram sem que a documentação acusasse nada. Estes testes não
 * conferem a prosa, que continua por conta de quem edita; conferem o que é verificável: que o documento é JSON válido,
 * que nenhuma referência aponta para um esquema inexistente, e que os esquemas do formato corrente estão lá.
 * </p>
 */
public class OpenApiSpecTest {

    private static final Pattern REF = Pattern.compile("\"\\$ref\"\\s*:\\s*\"#/components/schemas/([^\"]+)\"");

    private static JsonObject spec() {
        return JsonParser.parseString(RepositoryApiDocs.toJson("/api")).getAsJsonObject();
    }

    @Test
    public void documentIsValidJson() {
        var spec = spec();
        assertTrue("o documento precisa declarar a versão do OpenAPI", spec.has("openapi"));
        assertTrue(spec.has("paths"));
        assertTrue(spec.has("components"));
    }

    @Test
    public void everyReferencePointsToAnExistingSchema() {
        var json = RepositoryApiDocs.toJson("/api");
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        var broken = new ArrayList<String>();
        var m = REF.matcher(json);
        while (m.find()) {
            if (!schemas.has(m.group(1))) {
                broken.add(m.group(1));
            }
        }
        assertTrue("referências para esquemas inexistentes: " + broken, broken.isEmpty());
    }

    @Test
    public void criteriaShapeIsDocumented() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        // O critério trafega como objeto de pedidos; documentar só o valor solto descreveria uma API que não existe.
        assertTrue("falta o esquema Criterion", schemas.has("Criterion"));
        assertTrue("falta o esquema CriterionPredicate", schemas.has("CriterionPredicate"));

        var criterion = schemas.getAsJsonObject("Criterion").getAsJsonObject("properties");
        assertTrue("Criterion precisa expor a disjunção", criterion.has("or"));
        assertTrue("Criterion precisa expor os pedidos", criterion.has("p"));

        var operators = schemas.getAsJsonObject("CriterionPredicate")
                .getAsJsonObject("properties").getAsJsonObject("o").getAsJsonArray("enum");
        var names = new ArrayList<String>();
        operators.forEach(e -> names.add(e.getAsString()));
        assertTrue("os operadores documentados devem cobrir os que a API aceita",
                names.containsAll(List.of("EQ", "NE", "GT", "GE", "LT", "LE",
                        "LIKE", "ILIKE", "BETWEEN", "IN", "IS_NULL", "IS_NOT_NULL")));
    }

    @Test
    public void projectedCollectionShapeIsDocumented() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        assertTrue("falta o esquema ProjectedCollection", schemas.has("ProjectedCollection"));
        var props = schemas.getAsJsonObject("ProjectedCollection").getAsJsonObject("properties");
        assertTrue(props.has("shape"));
        assertTrue(props.has("where"));
        assertTrue(props.has("limit"));
        assertTrue(props.has("offset"));
    }

    @Test
    public void oneToManyFieldDocumentsBothItsForms() {
        // items é array na resposta e envelope na projeção — descrever só uma das formas induziria ao erro.
        var items = spec().getAsJsonObject("components").getAsJsonObject("schemas")
                .getAsJsonObject("Purchase").getAsJsonObject("properties").getAsJsonObject("items");

        assertTrue("items precisa documentar as duas formas", items.has("oneOf"));
        var forms = items.getAsJsonArray("oneOf").toString();
        assertTrue("falta a forma de resposta (array)", forms.contains("PurchaseItem"));
        assertTrue("falta a forma de projeção (envelope)", forms.contains("ProjectedCollection"));
    }

    @Test
    public void descriptionsAreFreeOfPlaceholders() {
        var json = RepositoryApiDocs.toJson("/api");
        assertFalse("sobrou marcador de rascunho na documentação", json.contains("TODO"));
        assertFalse(json.contains("FIXME"));
    }
}
