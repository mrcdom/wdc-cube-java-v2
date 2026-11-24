package br.com.wdc.store.test;

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
import br.com.wdc.store.domain.product.ProductCriteria;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.persistence.rest.doc.RepositoryApiDocs;

/**
 * Guardrail for the OpenAPI document.
 *
 * <p>
 * The specification is built in code, and code that only describes does not break when what it describes changes. These
 * tests do not check the prose, which remains up to whoever edits it; they check what is verifiable: that the document
 * is valid JSON, that no reference points to a nonexistent schema, and that the schemas of the current format are
 * there.
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
        assertTrue("the document must declare the OpenAPI version", spec.has("openapi"));
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
        assertTrue("references to nonexistent schemas: " + broken, broken.isEmpty());
    }

    @Test
    public void criteriaShapeIsDocumented() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        // The criterion travels as an object of requests; documenting only the bare value would describe an API that does not exist.
        assertTrue("missing the Criterion schema", schemas.has("Criterion"));
        assertTrue("missing the CriterionPredicate schema", schemas.has("CriterionPredicate"));

        var criterion = schemas.getAsJsonObject("Criterion").getAsJsonObject("properties");
        assertTrue("Criterion must expose the disjunction", criterion.has("or"));
        assertTrue("Criterion must expose the requests", criterion.has("p"));

        var operators = schemas.getAsJsonObject("CriterionPredicate")
                .getAsJsonObject("properties").getAsJsonObject("o").getAsJsonArray("enum");
        var names = new ArrayList<String>();
        operators.forEach(e -> names.add(e.getAsString()));
        assertTrue("the documented operators must cover the ones the API accepts",
                names.containsAll(List.of("EQ", "NE", "GT", "GE", "LT", "LE",
                        "LIKE", "ILIKE", "BETWEEN", "IN", "IS_NULL", "IS_NOT_NULL")));
    }

    @Test
    public void projectedCollectionShapeIsDocumented() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        assertTrue("missing the ProjectedCollection schema", schemas.has("ProjectedCollection"));
        var props = schemas.getAsJsonObject("ProjectedCollection").getAsJsonObject("properties");
        assertTrue(props.has("shape"));
        assertTrue(props.has("where"));
        assertTrue(props.has("limit"));
        assertTrue(props.has("offset"));
    }

    @Test
    public void oneToManyFieldDocumentsBothItsForms() {
        // items is an array in the response and an envelope in the projection — describing only one of the forms would mislead.
        var items = spec().getAsJsonObject("components").getAsJsonObject("schemas")
                .getAsJsonObject("Purchase").getAsJsonObject("properties").getAsJsonObject("items");

        assertTrue("items must document both forms", items.has("oneOf"));
        var forms = items.getAsJsonArray("oneOf").toString();
        assertTrue("missing the response form (array)", forms.contains("PurchaseItem"));
        assertTrue("missing the projection form (envelope)", forms.contains("ProjectedCollection"));
    }

    @Test
    public void descriptionsAreFreeOfPlaceholders() {
        var json = RepositoryApiDocs.toJson("/api");
        assertFalse("a draft marker was left in the documentation", json.contains("TODO"));
        assertFalse(json.contains("FIXME"));
    }

    /**
     * The documented criteria fields are the ones the domain actually has.
     *
     * <p>
     * It does not check against a list written here — it checks against {@code criterions()}. A fixed list in the test
     * would go stale along with the documentation, and the two would end up agreeing on what is no longer true.
     * </p>
     */
    @Test
    public void criteriaFieldsMatchTheDomain() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        record TestCase(String schema, Criteria criteria, Enum<?>[] orderings) { }
        var testCases = List.of(
                new TestCase("ProductCriteria", new ProductCriteria(), ProductCriteria.OrderBy.values()),
                new TestCase("UserCriteria", new UserCriteria(), UserCriteria.OrderBy.values()),
                new TestCase("PurchaseCriteria", new PurchaseCriteria(), PurchaseCriteria.OrderBy.values()),
                new TestCase("PurchaseItemCriteria", new PurchaseItemCriteria(), PurchaseItemCriteria.OrderBy.values()));

        for (var testCase : testCases) {
            assertTrue("missing the schema " + testCase.schema(), schemas.has(testCase.schema()));
            var props = schemas.getAsJsonObject(testCase.schema()).getAsJsonObject("properties");

            for (var criterion : testCase.criteria().criterions()) {
                assertTrue(testCase.schema() + " does not document the field " + criterion.name(),
                        props.has(criterion.name()));
            }

            var documented = new ArrayList<>(props.keySet());
            documented.remove("orderBy");
            assertEquals(testCase.schema() + " documents fields the criteria do not have",
                    testCase.criteria().criterions().size(), documented.size());
        }
    }

    /** The documented orderings are exactly the constants of the entity's enum. */
    @Test
    public void orderingsMatchTheDomain() {
        var schemas = spec().getAsJsonObject("components").getAsJsonObject("schemas");

        record TestCase(String schema, Enum<?>[] orderings) { }
        var testCases = List.of(
                new TestCase("ProductCriteria", ProductCriteria.OrderBy.values()),
                new TestCase("UserCriteria", UserCriteria.OrderBy.values()),
                new TestCase("PurchaseCriteria", PurchaseCriteria.OrderBy.values()),
                new TestCase("PurchaseItemCriteria", PurchaseItemCriteria.OrderBy.values()));

        for (var testCase : testCases) {
            var orderBy = schemas.getAsJsonObject(testCase.schema())
                    .getAsJsonObject("properties").getAsJsonObject("orderBy");
            assertTrue(testCase.schema() + " does not document the orderBy values", orderBy.has("enum"));

            var documentedOrderings = new ArrayList<String>();
            orderBy.getAsJsonArray("enum").forEach(e -> documentedOrderings.add(e.getAsString()));

            var expected = Arrays.stream(testCase.orderings()).map(Enum::name).toList();
            assertEquals(testCase.schema() + ": documented orderings differ from the enum", expected, documentedOrderings);
        }
    }

    /** Each query operation points to its entity's body, not to the generic one. */
    @Test
    public void queryOperationsPointToTheirEntityBody() {
        var json = RepositoryApiDocs.toJson("/api");

        for (var entity : List.of("Product", "User", "Purchase", "PurchaseItem")) {
            assertTrue("missing the query body for " + entity,
                    json.contains(entity + "FetchRequest"));
        }
    }
}
