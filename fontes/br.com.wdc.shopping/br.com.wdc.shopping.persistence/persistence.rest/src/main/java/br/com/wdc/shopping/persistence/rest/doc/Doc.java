package br.com.wdc.shopping.persistence.rest.doc;

import java.util.List;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;

/**
 * Concise builder helpers for OpenAPI operation descriptors.
 * <p>
 * Used by each controller's private {@code *Doc()} static methods to reduce boilerplate when building
 * {@link io.swagger.v3.oas.models.Operation} objects.
 */
public final class Doc {

    private Doc() {
    }

    /** Standard JWT Bearer security requirement (references the {@code bearerAuth} scheme). */
    public static final List<SecurityRequirement> BEARER = List.of(new SecurityRequirement().addList("bearerAuth"));

    // -- Request bodies --

    /**
     * Required JSON request body whose schema is a {@code $ref} to a named component schema.
     *
     * @param schemaRef full {@code $ref} string, e.g. {@code "#/components/schemas/Product"}
     */
    public static RequestBody body(String schemaRef) {
        return new RequestBody()
                .required(true)
                .content(jsonContent(new Schema<>().$ref(schemaRef)));
    }

    /** Required binary ({@code application/octet-stream}) request body — used for image uploads. */
    public static RequestBody binaryBody() {
        return new RequestBody()
                .required(true)
                .content(new Content().addMediaType("application/octet-stream",
                        new MediaType().schema(new StringSchema().format("binary"))));
    }

    // -- Responses --

    /** 200 JSON response referencing a named component schema. */
    public static ApiResponse ok(String schemaRef) {
        return new ApiResponse()
                .description("OK")
                .content(jsonContent(new Schema<>().$ref(schemaRef)));
    }

    /** 200 response returning raw image bytes ({@code image/png}). */
    public static ApiResponse image() {
        return new ApiResponse()
                .description("Product image (PNG)")
                .content(new Content().addMediaType("image/png",
                        new MediaType().schema(new StringSchema().format("binary"))));
    }

    /** 204 No Content — entity has no image. */
    public static ApiResponse noContent() {
        return new ApiResponse().description("No content — image not set");
    }

    /** 400 Bad Request. */
    public static ApiResponse badRequest() {
        return new ApiResponse()
                .description("Bad request")
                .content(jsonContent(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
    }

    /** 401 Unauthorized. */
    public static ApiResponse unauthorized() {
        return new ApiResponse()
                .description("Unauthorized — invalid or expired token")
                .content(jsonContent(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
    }

    /** 403 Forbidden. */
    public static ApiResponse forbidden() {
        return new ApiResponse()
                .description("Forbidden — insufficient permissions")
                .content(jsonContent(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
    }

    /** 404 Not Found. */
    public static ApiResponse notFound() {
        return new ApiResponse()
                .description("Not found")
                .content(jsonContent(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
    }

    // -- Parameters --

    /** Path parameter {@code {id}} of type int64. */
    public static Parameter pathId() {
        return new PathParameter()
                .name("id")
                .required(true)
                .schema(new io.swagger.v3.oas.models.media.IntegerSchema().format("int64"));
    }

    // -- Internal --

    static Content jsonContent(Schema<?> schema) {
        return new Content().addMediaType("application/json", new MediaType().schema(schema));
    }
}
