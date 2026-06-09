package br.com.wdc.shopping.persistence.rest.doc;

import java.math.BigDecimal;
import java.util.List;

import br.com.wdc.shopping.persistence.rest.AuthApiController;
import br.com.wdc.shopping.persistence.rest.ProductApiController;
import br.com.wdc.shopping.persistence.rest.PurchaseApiController;
import br.com.wdc.shopping.persistence.rest.PurchaseItemApiController;
import br.com.wdc.shopping.persistence.rest.UserApiController;
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
 * <h3>Serving the document</h3> {@link br.com.wdc.shopping.persistence.rest.RepositoryApiRoutes} registers
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
                .title("WDC Shopping — Repository API")
                .description("""
                        CRUD and query endpoints for the WDC Shopping data model.

                        **Authentication**: write operations require a Bearer JWT.
                        Obtain tokens via `POST /api/auth/login` after requesting a
                        challenge nonce from `GET /api/auth/challenge`.

                        **Projection**: fetch operations accept an optional `projection`
                        field containing a partial entity — only the specified fields are
                        returned in the response.
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
                // Generic request shapes (criteria fields are entity-specific — pass alongside these)
                .addSchemas("FetchRequest", fetchRequest())
                .addSchemas("PageRequest", pageRequest())
                .addSchemas("FetchByIdRequest", fetchByIdRequest())
                // Auth shapes
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

    // -- Generic request shapes --

    private static Schema<?> fetchRequest() {
        return new ObjectSchema()
                .description("Fetch request. Add entity-specific criteria fields alongside the pagination parameters.")
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
                .description(
                        "Paginated fetch request. Add entity-specific criteria fields alongside the pagination parameters.")
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

    private static Schema<?> challengeResponse() {
        return new ObjectSchema()
                .description("HMAC challenge — use the nonce when building the login digest")
                .addProperty("nonce", new StringSchema().description("One-time nonce"))
                .addProperty("expiresAt", new StringSchema().format("date-time").description("Nonce expiry"))
                .addRequiredItem("nonce").addRequiredItem("expiresAt");
    }

    private static Schema<?> loginRequest() {
        return new ObjectSchema()
                .description("Challenge-response login. Digest = HMAC-SHA256(nonce, password).")
                .addProperty("userName", new StringSchema())
                .addProperty("digest", new StringSchema().description("HMAC-SHA256 hex digest of nonce + password"))
                .addProperty("nonce", new StringSchema().description("Nonce from GET /api/auth/challenge"))
                .addRequiredItem("userName").addRequiredItem("digest").addRequiredItem("nonce");
    }

    private static Schema<?> tokenResponse() {
        return new ObjectSchema()
                .description("Issued or refreshed JWT token pair")
                .addProperty("accessToken", new StringSchema())
                .addProperty("refreshToken", new StringSchema())
                .addProperty("expiresAt", new StringSchema().format("date-time"))
                .addProperty("user", new ObjectSchema()
                        .addProperty("id", new IntegerSchema().format("int64"))
                        .addProperty("name", new StringSchema())
                        .addProperty("roles", new StringSchema()))
                .addRequiredItem("accessToken").addRequiredItem("refreshToken").addRequiredItem("expiresAt");
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
                .addProperty("items",
                        new ArraySchema().items(new Schema<>().$ref("#/components/schemas/PurchaseItem")));
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
