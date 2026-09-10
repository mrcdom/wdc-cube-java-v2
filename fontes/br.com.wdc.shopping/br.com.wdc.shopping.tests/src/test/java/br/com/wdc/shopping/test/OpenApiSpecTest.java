package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.shopping.domain.product.ProductCriteria;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.user.UserCriteria;
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

    /**
     * Os campos de critério documentados são os que o domínio realmente tem.
     *
     * <p>
     * Não confere contra uma lista escrita aqui — confere contra {@code criterions()}. Uma lista fixa no teste
     * envelheceria junto com a documentação, e os dois passariam a concordar sobre o que já não é verdade.
     * </p>
     */
    @Test
    public void criteriaFieldsMatchTheDomain() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        record Caso(String schema, Criteria criteria, Enum<?>[] orderings) { }
        var casos = List.of(
                new Caso("ProductCriteria", new ProductCriteria(), ProductCriteria.OrderBy.values()),
                new Caso("UserCriteria", new UserCriteria(), UserCriteria.OrderBy.values()),
                new Caso("PurchaseCriteria", new PurchaseCriteria(), PurchaseCriteria.OrderBy.values()),
                new Caso("PurchaseItemCriteria", new PurchaseItemCriteria(), PurchaseItemCriteria.OrderBy.values()));

        for (var caso : casos) {
            assertTrue("falta o esquema " + caso.schema(), schemas.has(caso.schema()));
            var props = schemas.getAsJsonObject(caso.schema()).getAsJsonObject("properties");

            for (var criterion : caso.criteria().criterions()) {
                assertTrue(caso.schema() + " não documenta o campo " + criterion.name(),
                        props.has(criterion.name()));
            }

            var documentados = new ArrayList<>(props.keySet());
            documentados.remove("orderBy");
            assertEquals(caso.schema() + " documenta campos que o critério não tem",
                    caso.criteria().criterions().size(), documentados.size());
        }
    }

    /** As ordenações documentadas são exatamente as constantes do enum da entidade. */
    @Test
    public void orderingsMatchTheDomain() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        record Caso(String schema, Enum<?>[] orderings) { }
        var casos = List.of(
                new Caso("ProductCriteria", ProductCriteria.OrderBy.values()),
                new Caso("UserCriteria", UserCriteria.OrderBy.values()),
                new Caso("PurchaseCriteria", PurchaseCriteria.OrderBy.values()),
                new Caso("PurchaseItemCriteria", PurchaseItemCriteria.OrderBy.values()));

        for (var caso : casos) {
            var orderBy = schemas.getAsJsonObject(caso.schema())
                    .getAsJsonObject("properties").getAsJsonObject("orderBy");
            assertTrue(caso.schema() + " não documenta os valores de orderBy", orderBy.has("enum"));

            var documentadas = new ArrayList<String>();
            orderBy.getAsJsonArray("enum").forEach(e -> documentadas.add(e.getAsString()));

            var esperadas = Arrays.stream(caso.orderings()).map(Enum::name).toList();
            assertEquals(caso.schema() + ": ordenações documentadas diferem do enum", esperadas, documentadas);
        }
    }

    /** Cada operação de consulta aponta para o corpo da sua entidade, e não para o genérico. */
    @Test
    public void queryOperationsPointToTheirEntityBody() {
        var json = RepositoryApiDocs.toJson("/api");

        for (var entidade : List.of("Product", "User", "Purchase", "PurchaseItem")) {
            assertTrue("falta o corpo de consulta de " + entidade,
                    json.contains(entidade + "FetchRequest"));
        }
    }
}
