package br.com.wdc.store.persistence.rest.doc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.TextCriterion;
import br.com.wdc.store.domain.product.ProductCriteria;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.persistence.rest.AuthApiController;
import br.com.wdc.store.persistence.rest.ProductApiController;
import br.com.wdc.store.persistence.rest.PurchaseApiController;
import br.com.wdc.store.persistence.rest.PurchaseItemApiController;
import br.com.wdc.store.persistence.rest.UserApiController;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * Assembles the complete OpenAPI 3.0 document for the Repository REST API.
 * <p>
 * Path descriptions live <em>inside each controller</em> as private {@code *Doc()} static methods placed next to the
 * handler they describe. This class only provides:
 * <ul>
 * <li>Shared component schemas (entities, common request/response shapes)</li>
 * <li>Global metadata (info, tags, security scheme)</li>
 * <li>Assembly — calls {@link OpenApiFragment#contribute(OpenAPI)} on every controller</li>
 * </ul>
 *
 * <h3>Serving the document</h3> {@link br.com.wdc.store.persistence.rest.RepositoryApiRoutes} registers
 * {@code GET /openapi.json} which returns {@link #toJson()}.
 */
public final class RepositoryApiDocs {

    private RepositoryApiDocs() {
    }

    /** Builds the full OpenAPI document by collecting fragments from all controllers. */
    public static OpenAPI build(String prefix) {
        var api = new OpenAPI()
                .info(info())
                .components(components())
                .tags(tags());

        AuthApiController.openApi(api, prefix);
        ProductApiController.openApi(api, prefix);
        UserApiController.openApi(api, prefix);
        PurchaseApiController.openApi(api, prefix);
        PurchaseItemApiController.openApi(api, prefix);

        return api;
    }

    /** Returns the document serialized as pretty-printed JSON. */
    public static String toJson(String prefix) {
        return Json.pretty(build(prefix));
    }

    // =========================================================================
    // :: Global metadata
    // =========================================================================

    private static Info info() {
        return new Info()
                .title("WDC Store — Repository API")
                .description("""
                        CRUD and query endpoints for the WDC Store data model.

                        **Authentication**: write operations require a Bearer JWT.
                        Obtain tokens via `POST /api/auth/login` after requesting a
                        challenge nonce from `GET /api/auth/challenge`.

                        **Projection**: fetch operations accept an optional `projection`
                        field containing a partial entity — only the specified fields are
                        returned in the response.

                        **Criteria**: each filterable field is sent as a `Criterion` object
                        carrying one or more comparison requests — see the `Criterion`
                        schema. Requests on the same field combine with `AND` unless the
                        field sets `"or": true`; different fields always combine with `AND`.

                        **Projected collections**: a one-to-many field in a *projection*
                        is an object (`ProjectedCollection`) declaring the item shape plus
                        the criteria, ordering and slice to apply to the collection. In a
                        *response* the same field is the plain array of rows.
                        """)
                .version("1.0.0")
                .contact(new Contact().name("WeDoCode"));
    }

    private static List<Tag> tags() {
        return List.of(
                new Tag().name("auth").description("Challenge-response authentication — login, refresh, logout"),
                new Tag().name("product").description("Products"),
                new Tag().name("user").description("Users"),
                new Tag().name("purchase").description("Purchases"),
                new Tag().name("purchase-item").description("Purchase line items"));
    }

    // =========================================================================
    // :: Component schemas
    // =========================================================================

    private static Components components() {
        return new Components()
                // Security schemes
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token from POST /api/auth/login"))
                // Common response envelopes
                .addSchemas("ErrorResponse", errorResponse())
                .addSchemas("InsertResult", insertResult())
                .addSchemas("MutationResult", mutationResult())
                .addSchemas("CountResult", countResult())
                // Criteria shapes (every entity-specific criteria field uses these)
                .addSchemas("CriterionPredicate", criterionPredicate())
                .addSchemas("Criterion", criterion())
                .addSchemas("ProjectedCollection", projectedCollection())
                // Generic request shapes (criteria fields are entity-specific — pass alongside these)
                .addSchemas("FetchRequest", fetchRequest())
                .addSchemas("PageRequest", pageRequest())
                .addSchemas("FetchByIdRequest", fetchByIdRequest())
                // Per-entity criteria — derived from the domain, see criteriaSchema
                .addSchemas("ProductCriteria",
                        criteriaSchema("Product", new ProductCriteria(), ProductCriteria.OrderBy.values()))
                .addSchemas("UserCriteria",
                        criteriaSchema("User", new UserCriteria(), UserCriteria.OrderBy.values()))
                .addSchemas("PurchaseCriteria",
                        criteriaSchema("Purchase", new PurchaseCriteria(), PurchaseCriteria.OrderBy.values()))
                .addSchemas("PurchaseItemCriteria",
                        criteriaSchema("PurchaseItem", new PurchaseItemCriteria(), PurchaseItemCriteria.OrderBy.values()))
                // Per-entity query bodies: pagination + criteria
                .addSchemas("ProductFetchRequest", requestOf("FetchRequest", "ProductCriteria"))
                .addSchemas("ProductPageRequest", requestOf("PageRequest", "ProductCriteria"))
                .addSchemas("UserFetchRequest", requestOf("FetchRequest", "UserCriteria"))
                .addSchemas("UserPageRequest", requestOf("PageRequest", "UserCriteria"))
                .addSchemas("PurchaseFetchRequest", requestOf("FetchRequest", "PurchaseCriteria"))
                .addSchemas("PurchasePageRequest", requestOf("PageRequest", "PurchaseCriteria"))
                .addSchemas("PurchaseItemFetchRequest", requestOf("FetchRequest", "PurchaseItemCriteria"))
                .addSchemas("PurchaseItemPageRequest", requestOf("PageRequest", "PurchaseItemCriteria"))
                // Auth shapes
                .addSchemas("ChallengeRequest", challengeRequest())
                .addSchemas("ChallengeResponse", challengeResponse())
                .addSchemas("LoginRequest", loginRequest())
                .addSchemas("TokenResponse", tokenResponse())
                .addSchemas("RefreshRequest", refreshRequest())
                // Entity schemas (all fields optional — used as projections too)
                .addSchemas("Product", product())
                .addSchemas("User", user())
                .addSchemas("Purchase", purchase())
                .addSchemas("PurchaseItem", purchaseItem())
                // Fetch response envelopes
                .addSchemas("ProductFetchResponse", fetchResponse("Product"))
                .addSchemas("ProductPageResponse", pageResponse("Product"))
                .addSchemas("UserFetchResponse", fetchResponse("User"))
                .addSchemas("UserPageResponse", pageResponse("User"))
                .addSchemas("PurchaseFetchResponse", fetchResponse("Purchase"))
                .addSchemas("PurchasePageResponse", pageResponse("Purchase"))
                .addSchemas("PurchaseItemFetchResponse", fetchResponse("PurchaseItem"))
                .addSchemas("PurchaseItemPageResponse", pageResponse("PurchaseItem"));
    }

    // -- Common response envelopes --

    private static Schema<?> errorResponse() {
        return new ObjectSchema()
                .description("Error response body")
                .addProperty("error", new StringSchema().description("Human-readable error message"))
                .addProperty("retryAfter", new IntegerSchema().format("int64")
                        .description("On a 429: the seconds to wait before trying again, as in the Retry-After header"))
                .addRequiredItem("error");
    }

    private static Schema<?> insertResult() {
        return new ObjectSchema()
                .description("Result of an insert operation")
                .addProperty("success", new BooleanSchema())
                .addProperty("id", new IntegerSchema().format("int64").description("Generated entity ID"))
                .addRequiredItem("success").addRequiredItem("id");
    }

    private static Schema<?> mutationResult() {
        return new ObjectSchema()
                .description("Result of an update operation")
                .addProperty("success", new BooleanSchema())
                .addRequiredItem("success");
    }

    private static Schema<?> countResult() {
        return new ObjectSchema()
                .description("Result of a delete or count operation")
                .addProperty("count", new IntegerSchema().description("Number of affected or matching rows"))
                .addRequiredItem("count");
    }

    // -- Criteria shapes --

    /**
     * One comparison request: the operator and the values it takes.
     *
     * <p>
     * Arity follows the operator — {@code BETWEEN} takes two values, {@code IN} takes many, {@code IS_NULL} takes
     * none. That is why a criterion cannot be reduced to a bare value on the wire.
     * </p>
     */
    private static Schema<?> criterionPredicate() {
        return new ObjectSchema()
                .description("A single comparison request within a criterion.")
                // The typed methods come before description(): in swagger-models description() returns a raw Schema,
                // and whatever is chained after it loses the type.
                .addProperty("o", new StringSchema()
                        ._enum(List.of("EQ", "NE", "GT", "GE", "LT", "LE",
                                "LIKE", "ILIKE", "BETWEEN", "IN", "IS_NULL", "IS_NOT_NULL"))
                        .description("Operator"))
                .addProperty("v", new ArraySchema()
                        .items(new Schema<>())
                        .description("""
                                Values for the operator — one for EQ/NE/GT/GE/LT/LE/LIKE/ILIKE, \
                                two for BETWEEN, many for IN, omitted for IS_NULL/IS_NOT_NULL. \
                                LIKE and ILIKE take the wildcards as part of the value, as in SQL."""))
                .addRequiredItem("o");
    }

    /**
     * A filterable field: the comparison requests made on it, and how they combine.
     */
    private static Schema<?> criterion() {
        return new ObjectSchema()
                .description("""
                        A filterable field. Every criteria field of every entity uses this shape — pass it \
                        alongside the pagination parameters, keyed by the field name (`productId`, `userName`, …).

                        Requests accumulate: `{"p":[{"o":"GE","v":[10]},{"o":"LE","v":[20]}]}` means a range. \
                        They combine with `AND` unless `or` is true. A bare value is also accepted and read \
                        as equality.""")
                .addProperty("or", new BooleanSchema()
                        ._default(false)
                        .description("Combine this field's requests with OR instead of AND. Defaults to false."))
                .addProperty("p", new ArraySchema()
                        .items(new Schema<>().$ref("#/components/schemas/CriterionPredicate"))
                        .description("The comparison requests, in the order they were made"));
    }

    /**
     * A one-to-many field as it appears in a <em>projection</em>: item shape plus what to do with the collection.
     */
    private static Schema<?> projectedCollection() {
        return new ObjectSchema()
                .description("""
                        A one-to-many field inside a projection. Declares the shape of one item plus the criteria, \
                        ordering and slice to apply to the collection.

                        In a **response** the same field is the plain array of rows instead — the two are told \
                        apart by their JSON type.""")
                .addProperty("shape", new ObjectSchema()
                        .description("One partial item declaring which fields of each row to return"))
                .addProperty("where", new ObjectSchema()
                        .description("""
                                Criteria for the child entity — same shape as a top-level criteria object, \
                                with `Criterion` fields plus an optional `orderBy`."""))
                .addProperty("limit", new IntegerSchema()
                        .description("Maximum child rows to return. Order the collection too, or the cut is arbitrary.")
                        .minimum(BigDecimal.ZERO))
                .addProperty("offset", new IntegerSchema()
                        .description("Child rows to skip. Order the collection too, or the skip is arbitrary.")
                        .minimum(BigDecimal.ZERO))
                .addRequiredItem("shape");
    }

    // -- Per-entity criteria --

    /**
     * An entity's criteria schema, <b>derived from the domain itself</b>.
     *
     * <p>
     * The fields come from {@link Criteria#criterions()} and the orderings from the entity's {@code OrderBy}. Writing
     * those lists by hand here would duplicate them, and duplicated documentation goes stale without warning. Derived,
     * adding a field to the criteria makes it show up in the documentation in the same build.
     * </p>
     */
    private static Schema<?> criteriaSchema(String entity, Criteria criteria, Enum<?>[] orderings) {
        var schema = new ObjectSchema().description("""
                Criteria fields for %s. Each one is a `Criterion` — an object with the comparison requests — \
                and different fields combine with `AND`. Send them alongside the pagination parameters.""".formatted(entity));

        for (var criterion : criteria.criterions()) {
            schema.addProperty(criterion.name(), criterionRef(family(criterion)));
        }

        schema.addProperty("orderBy", new StringSchema()
                ._enum(Arrays.stream(orderings).map(Enum::name).toList())
                .description("""
                        Provisioned ordering. Each value is a complete ordering whose name states the resulting \
                        effect — not a field with a direction. When omitted, the order is up to the database."""));

        return schema;
    }

    /**
     * The field's family, which decides which operators it accepts.
     *
     * <p>
     * Read from the {@code Criterion} class, not from a parallel table: it is the same distinction the compiler enforces
     * on the Java side.
     * </p>
     */
    private static String family(Criterion<?, ?> criterion) {
        if (criterion instanceof TextCriterion) {
            return "Text — also accepts LIKE and ILIKE, besides the ordering comparisons.";
        }
        if (criterion instanceof ComparableCriterion) {
            return "Comparable — also accepts GT, GE, LT, LE and BETWEEN.";
        }
        return "Identity — EQ, NE, IN, IS_NULL and IS_NOT_NULL.";
    }

    /**
     * A reference to {@code Criterion} with its own description.
     *
     * <p>
     * In OpenAPI 3.0 a {@code $ref} next to other keys is ignored; wrapping it in {@code allOf} is what allows adding the
     * description without losing the reference.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    private static Schema<?> criterionRef(String description) {
        List<Schema> ref = List.of(new Schema<>().$ref("#/components/schemas/Criterion"));
        return new Schema<>().allOf(ref).description(description);
    }

    /** A query body: the common pagination plus the entity's criteria fields. */
    @SuppressWarnings("rawtypes")
    private static Schema<?> requestOf(String base, String criteriaSchema) {
        List<Schema> parts = List.of(
                new Schema<>().$ref("#/components/schemas/" + base),
                new Schema<>().$ref("#/components/schemas/" + criteriaSchema));
        return new Schema<>().allOf(parts);
    }

    // -- Generic request shapes --

    private static Schema<?> fetchRequest() {
        return new ObjectSchema()
                .description("""
                        Common query parameters. Each entity's criteria fields and `orderBy` are in its own \
                        schema — `ProductCriteria`, `UserCriteria`, `PurchaseCriteria`, \
                        `PurchaseItemCriteria` — and the accepted body is the union of both.""")
                .addProperty("offset",
                        new IntegerSchema().description("Zero-based row offset").minimum(BigDecimal.ZERO))
                .addProperty("limit",
                        new IntegerSchema().description("Maximum rows to return — 0 means unlimited")
                                .minimum(BigDecimal.ZERO))
                .addProperty("projection", new ObjectSchema()
                        .description("Partial entity declaring which fields to include in the response"));
    }

    private static Schema<?> pageRequest() {
        return new ObjectSchema()
                .description("""
                        Common parameters of a paginated query. Each entity's criteria fields and `orderBy` are \
                        in its own schema, and the accepted body is the union of both.""")
                .addProperty("page", new IntegerSchema().description("Zero-based page index").minimum(BigDecimal.ZERO))
                .addProperty("pageSize", new IntegerSchema().description("Rows per page").minimum(BigDecimal.ONE))
                .addProperty("projection", new ObjectSchema()
                        .description("Partial entity declaring which fields to include in the response"));
    }

    private static Schema<?> fetchByIdRequest() {
        return new ObjectSchema()
                .description("Fetch a single entity by ID, optionally narrowing the returned fields with a projection.")
                .addProperty("id", new IntegerSchema().format("int64"))
                .addProperty("projection", new ObjectSchema())
                .addRequiredItem("id");
    }

    // -- Auth shapes --

    private static Schema<?> challengeRequest() {
        return new ObjectSchema()
                .description("Starts a user's SCRAM-SHA-256 sign-in")
                .addProperty("userName", new StringSchema())
                .addRequiredItem("userName");
    }

    private static Schema<?> challengeResponse() {
        return new ObjectSchema()
                .description("SCRAM-SHA-256 challenge. An unknown user gets made-up parameters, the same every time.")
                .addProperty("nonce", new StringSchema().description("Single-use nonce, good for this user only"))
                .addProperty("salt", new StringSchema().format("byte").description("Salt of the user's verifier (Base64)"))
                .addProperty("iterations", new IntegerSchema().format("int32").description("PBKDF2 iterations of the verifier"))
                .addProperty("expiresAt", new StringSchema().format("date-time").description("Nonce expiry"))
                .addRequiredItem("nonce").addRequiredItem("salt").addRequiredItem("iterations").addRequiredItem("expiresAt");
    }

    private static Schema<?> loginRequest() {
        return new ObjectSchema()
                .description("SCRAM-SHA-256 sign-in. SaltedPassword = PBKDF2-HMAC-SHA256(digest, salt, iterations), where digest"
                        + " is MD5(password) as a signed integer in base 36; ClientKey = HMAC(SaltedPassword, \"Client Key\");"
                        + " proof = ClientKey XOR HMAC(SHA-256(ClientKey), \"n=<userName>,r=<nonce>\").")
                .addProperty("userName", new StringSchema())
                .addProperty("proof", new StringSchema().format("byte").description("The client's SCRAM proof (Base64)"))
                .addProperty("nonce", new StringSchema().description("Nonce from POST /api/auth/challenge for this userName"))
                .addRequiredItem("userName").addRequiredItem("proof").addRequiredItem("nonce");
    }

    private static Schema<?> tokenResponse() {
        return new ObjectSchema()
                .description("Issued or refreshed JWT token pair")
                .addProperty("userId", new IntegerSchema().format("int64"))
                .addProperty("accessToken", new StringSchema())
                .addProperty("refreshToken", new StringSchema())
                .addProperty("expiresAt", new StringSchema().format("date-time"))
                .addProperty("publicKey", new StringSchema().description("The session's RSA public key (Base64 SPKI): encrypts a password sent to the user API"))
                .addProperty("serverSignature", new StringSchema().format("byte").description(
                        "Sign-in only: HMAC(HMAC(SaltedPassword, \"Server Key\"), \"n=<userName>,r=<nonce>\") (Base64); the client"
                                + " checks it to know the server holds the verifier"))
                .addRequiredItem("userId").addRequiredItem("accessToken").addRequiredItem("refreshToken").addRequiredItem("expiresAt");
    }

    private static Schema<?> refreshRequest() {
        return new ObjectSchema()
                .description("Token refresh or logout request")
                .addProperty("refreshToken", new StringSchema())
                .addRequiredItem("refreshToken");
    }

    // -- Entity schemas --

    private static Schema<?> product() {
        return new ObjectSchema()
                .description("Product. All fields are optional when used as a projection.")
                .addProperty("id", new IntegerSchema().format("int64"))
                .addProperty("name", new StringSchema())
                .addProperty("price", new NumberSchema().format("double"))
                .addProperty("description", new StringSchema());
    }

    private static Schema<?> user() {
        return new ObjectSchema()
                .description("User. The password field is never included in responses.")
                .addProperty("id", new IntegerSchema().format("int64"))
                .addProperty("userName", new StringSchema())
                .addProperty("name", new StringSchema())
                .addProperty("roles", new StringSchema().description("Comma-separated role names"));
    }

    private static Schema<?> purchase() {
        return new ObjectSchema()
                .description("Purchase. All fields are optional when used as a projection.")
                .addProperty("id", new IntegerSchema().format("int64"))
                .addProperty("buyDate", new StringSchema().format("date-time"))
                .addProperty("user", new Schema<>().$ref("#/components/schemas/User"))
                // In a response it is the array of rows; in a projection it is the envelope declaring shape, criteria and slice.
                .addProperty("items", purchaseItems());
    }

    /**
     * The {@code items} field of {@code Purchase}, in the two forms it takes.
     *
     * <p>
     * It gets its own method because {@code oneOf} takes {@code List<Schema>} — a raw-typed list that cannot be built
     * inline without losing the type. Here the list is built explicitly and the suppression stays confined to that exact
     * spot.
     * </p>
     */
    @SuppressWarnings("rawtypes")
    private static Schema<?> purchaseItems() {
        List<Schema> forms = List.of(
                new ArraySchema().items(new Schema<>().$ref("#/components/schemas/PurchaseItem")),
                new Schema<>().$ref("#/components/schemas/ProjectedCollection"));

        return new Schema<>()
                .oneOf(forms)
                .description("""
                        Line items. In a **response**, the array of rows. In a **projection**, a \
                        `ProjectedCollection` object declaring the item shape plus the criteria, ordering \
                        and slice to apply.""");
    }

    private static Schema<?> purchaseItem() {
        return new ObjectSchema()
                .description("Purchase line item. All fields are optional when used as a projection.")
                .addProperty("id", new IntegerSchema().format("int64"))
                .addProperty("amount", new IntegerSchema())
                .addProperty("price", new NumberSchema().format("double"))
                .addProperty("purchaseId", new IntegerSchema().format("int64"))
                .addProperty("productId", new IntegerSchema().format("int64"))
                .addProperty("product", new Schema<>().$ref("#/components/schemas/Product"));
    }

    // -- Fetch response envelopes --

    private static Schema<?> fetchResponse(String entitySchema) {
        return new ObjectSchema()
                .addProperty("items",
                        new ArraySchema().items(new Schema<>().$ref("#/components/schemas/" + entitySchema)))
                .addRequiredItem("items");
    }

    private static Schema<?> pageResponse(String entitySchema) {
        return new ObjectSchema()
                .addProperty("items",
                        new ArraySchema().items(new Schema<>().$ref("#/components/schemas/" + entitySchema)))
                .addProperty("totalItems", new IntegerSchema())
                .addRequiredItem("items").addRequiredItem("totalItems");
    }
}
