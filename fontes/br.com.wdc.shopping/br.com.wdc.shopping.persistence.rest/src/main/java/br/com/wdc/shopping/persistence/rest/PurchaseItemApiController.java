package br.com.wdc.shopping.persistence.rest;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.PurchaseItemModelCodec;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.persistence.rest.doc.Doc;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class PurchaseItemApiController {

    static void configure(JavalinConfig config) {
        configure(config, "");
    }

    static void configure(JavalinConfig config, String prefix) {
        var ctrl = new PurchaseItemApiController();
        config.routes.post(insertPath(prefix), ctrl::insert);
        config.routes.post(updatePath(prefix), ctrl::update);
        config.routes.post(deletePath(prefix), ctrl::delete);
        config.routes.post(countPath(prefix), ctrl::count);
        config.routes.post(fetchPath(prefix), ctrl::fetch);
        config.routes.post(fetchPagePath(prefix), ctrl::fetchPage);
        config.routes.post(fetchByIdPostPath(prefix), ctrl::fetchByIdPost);
        config.routes.get(fetchByIdPath(prefix), ctrl::fetchById);
    }

    public static void openApi(OpenAPI api) {
        var ctrl = new PurchaseItemApiController();
        ctrl.insertDoc(api);
        ctrl.updateDoc(api);
        ctrl.deleteDoc(api);
        ctrl.countDoc(api);
        ctrl.fetchDoc(api);
        ctrl.fetchPageDoc(api);
        ctrl.fetchByIdDoc(api);
        ctrl.fetchByIdPostDoc(api);
    }

    private static PurchaseItemRepository repo() {
        return PurchaseItemRepository.BEAN.get();
    }

    private final PurchaseItemModelCodec codec = new PurchaseItemModelCodec();

    private static PurchaseItem fullProjection() {
        var pv = ProjectionValues.INSTANCE;

        var product = new Product();
        product.id = pv.i64;
        product.name = pv.str;
        product.price = pv.f64;

        var prj = new PurchaseItem();
        prj.id = pv.i64;
        prj.amount = pv.i32;
        prj.price = pv.f64;
        prj.product = product;
        return prj;
    }

    // :: Insert

    private static String insertPath(String prefix) {
        return prefix + "/api/repo/purchase-item/insert";
    }

    private void insertDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Insert a purchase item")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/PurchaseItem"))
                .responses(new ApiResponses()
                        .addApiResponse("201", Doc.ok("#/components/schemas/InsertResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(insertPath(""), new PathItem().post(operation));
    }

    private void insert(Context ctx) {
        SecurityEnforcer.require("purchase-item", "write");
        var reader = new JsonStreamReader(ctx.body());
        var item = codec.readEntity(reader);
        boolean success = repo().insert(item);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("id").value(item.id != null ? item.id : -1);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Update

    private static String updatePath(String prefix) {
        return prefix + "/api/repo/purchase-item/update";
    }

    private void updateDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Update a purchase item")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/PurchaseItem"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/MutationResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(updatePath(""), new PathItem().post(operation));
    }

    private void update(Context ctx) {
        SecurityEnforcer.require("purchase-item", "write");
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
        return prefix + "/api/repo/purchase-item/delete";
    }

    private void deleteDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Delete purchase items matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(deletePath(""), new PathItem().post(operation));
    }

    private void delete(Context ctx) {
        var sc = SecurityEnforcer.require("purchase-item", "delete");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        enforceUserScope(sc, criteria);
        int count = repo().delete(criteria);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("count").value(count);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Count

    private static String countPath(String prefix) {
        return prefix + "/api/repo/purchase-item/count";
    }

    private void countDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Count purchase items matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(countPath(""), new PathItem().post(operation));
    }

    private void count(Context ctx) {
        var sc = SecurityEnforcer.require("purchase-item", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        enforceUserScope(sc, criteria);
        int count = repo().count(criteria);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("count").value(count);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch

    private static String fetchPath(String prefix) {
        return prefix + "/api/repo/purchase-item/fetch";
    }

    private void fetchDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Fetch purchase items matching criteria (offset/limit)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/PurchaseItemFetchResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPath(""), new PathItem().post(operation));
    }

    private void fetch(Context ctx) {
        var sc = SecurityEnforcer.require("purchase-item", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new PurchaseItemCriteria();
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
        enforceUserScope(sc, criteria);
        if (criteria.projection() == null)
            criteria.withProjection(fullProjection());
        var items = repo().fetch(criteria, offset, limit);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : items) {
            item.purchase = null;
            codec.writeEntity(writer, item);
        }
        writer.endArray();
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch Page

    private static String fetchPagePath(String prefix) {
        return prefix + "/api/repo/purchase-item/fetch-page";
    }

    private void fetchPageDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Fetch purchase items matching criteria (page/pageSize)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/PageRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/PurchaseItemPageResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPagePath(""), new PathItem().post(operation));
    }

    private void fetchPage(Context ctx) {
        SecurityEnforcer.require("purchase-item", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new PurchaseItemCriteria();
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
            item.purchase = null;
            codec.writeEntity(writer, item);
        }
        writer.endArray();
        writer.name("totalItems").value(page.totalItems());
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch by ID

    private static String fetchByIdPath(String prefix) {
        return prefix + "/api/repo/purchase-item/{id}";
    }

    private void fetchByIdDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Fetch a purchase item by ID (GET)")
                .security(Doc.BEARER)
                .addParametersItem(Doc.pathId())
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/PurchaseItem"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPath(""), new PathItem().get(operation));
    }

    private void fetchById(Context ctx) {
        SecurityEnforcer.require("purchase-item", "read");
        Long id = Long.parseLong(ctx.pathParam("id"));
        var result = repo().fetchById(id, fullProjection());
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        result.purchase = null;
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Fetch by ID (POST)

    private static String fetchByIdPostPath(String prefix) {
        return prefix + "/api/repo/purchase-item/fetch-by-id";
    }

    private void fetchByIdPostDoc(OpenAPI api) {
        var operation = new Operation()
                .addTagsItem("purchase-item").summary("Fetch a purchase item by ID (POST, supports projection)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchByIdRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/PurchaseItem"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPostPath(""), new PathItem().post(operation));
    }

    private void fetchByIdPost(Context ctx) {
        SecurityEnforcer.require("purchase-item", "read");
        var reader = new JsonStreamReader(ctx.body());
        Long id = null;
        PurchaseItem projection = null;
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
        result.purchase = null;
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Security helpers

    private static void enforceUserScope(SecurityContext sc, PurchaseItemCriteria criteria) {
        if (sc != null && !sc.hasDataAll()) {
            criteria.withUserId(sc.userId());
        }
    }

    // :: Helpers

    private PurchaseItemCriteria readCriteria(JsonStreamReader reader) {
        var criteria = new PurchaseItemCriteria();
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
