package br.com.wdc.shopping.persistence.rest;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.ProductModelCodec;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.persistence.rest.doc.Doc;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class ProductApiController {

    private static final Log LOG = Log.getLogger(ProductApiController.class);

    public static void configure(JavalinConfig config, String prefix) {
        var ctrl = new ProductApiController();
        config.routes.post(insertPath(prefix), RepositoryApiRoutes.transactional(ctrl::insert));
        config.routes.post(updatePath(prefix), RepositoryApiRoutes.transactional(ctrl::update));
        config.routes.post(deletePath(prefix), RepositoryApiRoutes.transactional(ctrl::delete));
        config.routes.post(countPath(prefix), ctrl::count);
        config.routes.post(fetchPath(prefix), ctrl::fetch);
        config.routes.post(fetchPagePath(prefix), ctrl::fetchPage);
        config.routes.post(fetchByIdPostPath(prefix), ctrl::fetchByIdPost);
        config.routes.get(fetchByIdPath(prefix), ctrl::fetchById);
        config.routes.get(imagePath(prefix), ctrl::fetchImage);
        config.routes.put(imagePath(prefix), RepositoryApiRoutes.transactional(ctrl::updateImage));
    }

    public static void openApi(OpenAPI api, String prefix) {
        var ctrl = new ProductApiController();
        ctrl.insertDoc(api, prefix);
        ctrl.updateDoc(api, prefix);
        ctrl.deleteDoc(api, prefix);
        ctrl.countDoc(api, prefix);
        ctrl.fetchDoc(api, prefix);
        ctrl.fetchPageDoc(api, prefix);
        ctrl.fetchByIdDoc(api, prefix);
        ctrl.fetchByIdPostDoc(api, prefix);
        ctrl.imageDoc(api, prefix);
    }

    private static ProductRepository repo() {
        return ProductRepository.BEAN.get();
    }

    private final ProductModelCodec codec = new ProductModelCodec();

    private static Product fullProjection() {
        var pv = ProjectionValues.INSTANCE;
        var prj = new Product();
        prj.id = pv.i64;
        prj.name = pv.str;
        prj.price = pv.f64;
        prj.description = pv.str;
        return prj;
    }

    // :: Insert

    private static String insertPath(String prefix) {
        return prefix + "/api/repo/product/insert";
    }

    private void insertDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Insert a product")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/Product"))
                .responses(new ApiResponses()
                        .addApiResponse("201", Doc.ok("#/components/schemas/InsertResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(insertPath(prefix), new PathItem().post(operation));
    }

    private void insert(Context ctx) {
        SecurityEnforcer.require("product", "write");
        var reader = new JsonStreamReader(ctx.body());
        var product = codec.readEntity(reader);
        boolean success = repo().insert(product);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("id").value(product.id != null ? product.id : -1);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Update

    private static String updatePath(String prefix) {
        return prefix + "/api/repo/product/update";
    }

    private void updateDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Update a product")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/Product"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/MutationResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(updatePath(prefix), new PathItem().post(operation));
    }

    private void update(Context ctx) {
        SecurityEnforcer.require("product", "write");
        var reader = new JsonStreamReader(ctx.body());
        var data = codec.readEntityForUpdate(reader);
        boolean success = repo().update(data.entity(), null, data.projection());
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Delete

    private static String deletePath(String prefix) {
        return prefix + "/api/repo/product/delete";
    }

    private void deleteDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Delete products matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(deletePath(prefix), new PathItem().post(operation));
    }

    private void delete(Context ctx) {
        SecurityEnforcer.require("product", "write");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        int count = repo().delete(criteria);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("count").value(count);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Count

    private static String countPath(String prefix) {
        return prefix + "/api/repo/product/count";
    }

    private void countDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Count products matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(countPath(prefix), new PathItem().post(operation));
    }

    private void count(Context ctx) {
        SecurityEnforcer.require("product", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        int count = repo().count(criteria);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("count").value(count);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch

    private static String fetchPath(String prefix) {
        return prefix + "/api/repo/product/fetch";
    }

    private void fetchDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Fetch products matching criteria (offset/limit)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/ProductFetchResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPath(prefix), new PathItem().post(operation));
    }

    private void fetch(Context ctx) {
        SecurityEnforcer.require("product", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new ProductCriteria();
        int offset = 0;
        int limit = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            var name = reader.nextName();
            switch (name) {
            case "projection" -> criteria.withProjection(codec.readEntity(reader));
            case "offset" -> offset = InputCoerceUtils.asInteger(reader, 0);
            case "limit" -> limit = InputCoerceUtils.asInteger(reader, 0);
            default -> {
                if (!codec.readCriteriaField(reader, name, criteria))
                    reader.skipValue();
            }
            }
        }
        reader.endObject();
        if (criteria.projection() == null)
            criteria.withProjection(fullProjection());
        var items = repo().fetch(criteria, offset, limit);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : items) {
            codec.writeEntity(writer, item);
        }
        writer.endArray();
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch Page

    private static String fetchPagePath(String prefix) {
        return prefix + "/api/repo/product/fetch-page";
    }

    private void fetchPageDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Fetch products matching criteria (page/pageSize)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/PageRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/ProductPageResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPagePath(prefix), new PathItem().post(operation));
    }

    private void fetchPage(Context ctx) {
        SecurityEnforcer.require("product", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new ProductCriteria();
        int pageIx = 0;
        int pageSz = 0;
        reader.beginObject();
        while (reader.hasNext()) {
            var name = reader.nextName();
            switch (name) {
            case "projection" -> criteria.withProjection(codec.readEntity(reader));
            case "page" -> pageIx = InputCoerceUtils.asInteger(reader, 0);
            case "pageSize" -> pageSz = InputCoerceUtils.asInteger(reader, 0);
            default -> {
                if (!codec.readCriteriaField(reader, name, criteria))
                    reader.skipValue();
            }
            }
        }
        reader.endObject();
        if (criteria.projection() == null)
            criteria.withProjection(fullProjection());
        var page = repo().fetchPage(criteria, pageIx, pageSz);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : page.items()) {
            codec.writeEntity(writer, item);
        }
        writer.endArray();
        writer.name("totalItems").value(page.totalItems());
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch by ID

    private static String fetchByIdPath(String prefix) {
        return prefix + "/api/repo/product/{id}";
    }

    private void fetchByIdDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Fetch a product by ID (GET)")
                .security(Doc.BEARER)
                .addParametersItem(Doc.pathId())
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/Product"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPath(prefix), new PathItem().get(operation));
    }

    private void fetchById(Context ctx) {
        SecurityEnforcer.require("product", "read");
        Long id = Long.parseLong(ctx.pathParam("id"));
        var result = repo().fetchById(id, fullProjection());
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Fetch by ID (POST)

    private static String fetchByIdPostPath(String prefix) {
        return prefix + "/api/repo/product/fetch-by-id";
    }

    private void fetchByIdPostDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("product").summary("Fetch a product by ID (POST, supports projection)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchByIdRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/Product"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPostPath(prefix), new PathItem().post(operation));
    }

    private void fetchByIdPost(Context ctx) {
        SecurityEnforcer.require("product", "read");
        var reader = new JsonStreamReader(ctx.body());
        Long id = null;
        Product projection = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
            case "id" -> id = InputCoerceUtils.asLong(reader);
            case "projection" -> projection = codec.readEntity(reader);
            default -> reader.skipValue();
            }
        }
        reader.endObject();
        var result = repo().fetchById(id, projection != null ? projection : fullProjection());
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Image

    private static String imagePath(String prefix) {
        return prefix + "/api/repo/product/{id}/image";
    }

    private void imageDoc(OpenAPI api, String prefix) {
        api.path(imagePath(prefix), new PathItem()
                .get(new Operation()
                        .addTagsItem("product").summary("Get the product image")
                        .addParametersItem(Doc.pathId())
                        .responses(new ApiResponses()
                                .addApiResponse("200", Doc.image())
                                .addApiResponse("204", Doc.noContent())))
                .put(new Operation()
                        .addTagsItem("product").summary("Upload or replace the product image")
                        .security(Doc.BEARER)
                        .addParametersItem(Doc.pathId())
                        .requestBody(Doc.binaryBody())
                        .responses(new ApiResponses()
                                .addApiResponse("200", Doc.ok("#/components/schemas/MutationResult"))
                                .addApiResponse("401", Doc.unauthorized())
                                .addApiResponse("403", Doc.forbidden()))));
    }

    private void fetchImage(Context ctx) {
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            LOG.debug(e.getMessage());
            ctx.status(400).contentType("application/json").result("{\"error\":\"Invalid product ID\"}");
            return;
        }

        byte[] imageBytes;
        try {
            imageBytes = repo().fetchImage(id);
        } catch (Exception e) {
            LOG.error("Fetching product image", e);
            ctx.status(500).contentType("application/json").result("{\"error\":\"Failed to fetch image\"}");
            return;
        }

        if (imageBytes == null) {
            ctx.status(204);
            return;
        }
        ctx.contentType("image/png");
        ctx.result(imageBytes);
    }

    private void updateImage(Context ctx) {
        SecurityEnforcer.require("product", "write");
        Long id;
        try {
            id = Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            LOG.debug(e.getMessage());
            ctx.status(400).contentType("application/json").result("{\"error\":\"Invalid product ID\"}");
            return;
        }

        try {
            byte[] imageBytes = ctx.bodyAsBytes();
            boolean success = repo().updateImage(id, imageBytes);
            ctx.contentType("application/json").result("{\"success\":" + success + "}");
        } catch (Exception e) {
            LOG.error("Updating product image", e);
            ctx.status(500).contentType("application/json").result("{\"error\":\"Failed to update image\"}");
        }
    }

    // :: Helpers

    private ProductCriteria readCriteria(JsonStreamReader reader) {
        var criteria = new ProductCriteria();
        reader.beginObject();
        while (reader.hasNext()) {
            var name = reader.nextName();
            if (!codec.readCriteriaField(reader, name, criteria)) {
                reader.skipValue();
            }
        }
        reader.endObject();
        return criteria;
    }

    private static void json(Context ctx, JsonStreamWriter writer) {
        ctx.contentType("application/json");
        ctx.result(writer.result());
    }

}
