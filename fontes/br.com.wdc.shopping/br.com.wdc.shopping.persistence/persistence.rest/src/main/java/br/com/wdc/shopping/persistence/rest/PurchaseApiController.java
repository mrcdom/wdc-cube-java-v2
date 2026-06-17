package br.com.wdc.shopping.persistence.rest;

import java.util.Collections;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.PurchaseModelCodec;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.persistence.rest.doc.Doc;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class PurchaseApiController {

    public static void configure(JavalinConfig config, String prefix) {
        var ctrl = new PurchaseApiController();
        config.routes.post(insertPath(prefix), ctrl::insert);
        config.routes.post(updatePath(prefix), ctrl::update);
        config.routes.post(deletePath(prefix), ctrl::delete);
        config.routes.post(countPath(prefix), ctrl::count);
        config.routes.post(fetchPath(prefix), ctrl::fetch);
        config.routes.post(fetchPagePath(prefix), ctrl::fetchPage);
        config.routes.post(fetchByIdPostPath(prefix), ctrl::fetchByIdPost);
        config.routes.get(fetchByIdPath(prefix), ctrl::fetchById);
    }

    public static void openApi(OpenAPI api, String prefix) {
        var ctrl = new PurchaseApiController();
        ctrl.insertDoc(api, prefix);
        ctrl.updateDoc(api, prefix);
        ctrl.deleteDoc(api, prefix);
        ctrl.countDoc(api, prefix);
        ctrl.fetchDoc(api, prefix);
        ctrl.fetchPageDoc(api, prefix);
        ctrl.fetchByIdDoc(api, prefix);
        ctrl.fetchByIdPostDoc(api, prefix);
    }

    private static PurchaseRepository repo() {
        return PurchaseRepository.BEAN.get();
    }

    private final PurchaseModelCodec codec = new PurchaseModelCodec();

    private static Purchase fullProjectionWithItems() {
        var pv = ProjectionValues.INSTANCE;

        var product = new Product();
        product.id = pv.i64;
        product.name = pv.str;
        product.price = pv.f64;

        var item = new PurchaseItem();
        item.id = pv.i64;
        item.amount = pv.i32;
        item.price = pv.f64;
        item.product = product;

        var prj = new Purchase();
        prj.id = pv.i64;
        prj.buyDate = pv.offsetDateTime;
        prj.user = new User();
        prj.user.id = pv.i64;
        prj.user.name = pv.str;
        prj.items = Collections.singletonList(item);

        return prj;
    }

    private static Purchase simpleProjection() {
        var pv = ProjectionValues.INSTANCE;
        var prj = new Purchase();
        prj.id = pv.i64;
        prj.buyDate = pv.offsetDateTime;
        prj.user = new User();
        prj.user.id = pv.i64;
        prj.user.name = pv.str;
        return prj;
    }

    // :: Insert

    private static String insertPath(String prefix) {
        return prefix + "/api/repo/purchase/insert";
    }

    private void insertDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Insert a purchase")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/Purchase"))
                .responses(new ApiResponses()
                        .addApiResponse("201", Doc.ok("#/components/schemas/InsertResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(insertPath(prefix), new PathItem().post(operation));
    }

    private void insert(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "write");
        var reader = new JsonStreamReader(ctx.body());
        var purchase = codec.readEntity(reader);
        enforceUserScope(sc, purchase);
        boolean success = repo().insert(purchase);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("id").value(purchase.id != null ? purchase.id : -1);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Update

    private static String updatePath(String prefix) {
        return prefix + "/api/repo/purchase/update";
    }

    private void updateDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Update a purchase")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/Purchase"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/MutationResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(updatePath(prefix), new PathItem().post(operation));
    }

    private void update(Context ctx) {
        SecurityEnforcer.require("purchase", "write");
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
        return prefix + "/api/repo/purchase/delete";
    }

    private void deleteDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Delete purchases matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(deletePath(prefix), new PathItem().post(operation));
    }

    private void delete(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "delete");
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
        return prefix + "/api/repo/purchase/count";
    }

    private void countDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Count purchases matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(countPath(prefix), new PathItem().post(operation));
    }

    private void count(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "read");
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
        return prefix + "/api/repo/purchase/fetch";
    }

    private void fetchDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Fetch purchases matching criteria (offset/limit)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/PurchaseFetchResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPath(prefix), new PathItem().post(operation));
    }

    private void fetch(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new PurchaseCriteria();
        int offset = 0;
        int limit = 0;
        boolean includeItems = true;
        reader.beginObject();
        while (reader.hasNext()) {
            var name = reader.nextName();
            switch (name) {
            case "projection" -> criteria.withProjection(codec.readEntity(reader));
            case "offset" -> offset = InputCoerceUtils.asInteger(reader, 0);
            case "limit" -> limit = InputCoerceUtils.asInteger(reader, 0);
            case "includeItems" -> includeItems = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
            default -> {
                if (!codec.readCriteriaField(reader, name, criteria))
                    reader.skipValue();
            }
            }
        }
        reader.endObject();
        enforceUserScope(sc, criteria);
        if (criteria.projection() == null) {
            criteria.withProjection(includeItems ? fullProjectionWithItems() : simpleProjection());
        }
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
        return prefix + "/api/repo/purchase/fetch-page";
    }

    private void fetchPageDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Fetch purchases matching criteria (page/pageSize)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/PageRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/PurchasePageResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPagePath(prefix), new PathItem().post(operation));
    }

    private void fetchPage(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new PurchaseCriteria();
        int pageIx = 0;
        int pageSz = 0;
        boolean includeItems = true;
        reader.beginObject();
        while (reader.hasNext()) {
            var name = reader.nextName();
            switch (name) {
            case "projection" -> criteria.withProjection(codec.readEntity(reader));
            case "page" -> pageIx = InputCoerceUtils.asInteger(reader, 0);
            case "pageSize" -> pageSz = InputCoerceUtils.asInteger(reader, 0);
            case "includeItems" -> includeItems = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
            default -> {
                if (!codec.readCriteriaField(reader, name, criteria))
                    reader.skipValue();
            }
            }
        }
        reader.endObject();
        enforceUserScope(sc, criteria);
        if (criteria.projection() == null) {
            criteria.withProjection(includeItems ? fullProjectionWithItems() : simpleProjection());
        }
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
        return prefix + "/api/repo/purchase/{id}";
    }

    private void fetchByIdDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Fetch a purchase by ID (GET)")
                .security(Doc.BEARER)
                .addParametersItem(Doc.pathId())
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/Purchase"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPath(prefix), new PathItem().get(operation));
    }

    private void fetchById(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "read");
        Long id = Long.parseLong(ctx.pathParam("id"));
        var result = repo().fetchById(id, fullProjectionWithItems());
        if (result == null
                || (sc != null && !sc.hasDataAll() && result.user != null && !sc.userId().equals(result.user.id))) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Fetch by ID (POST)

    private static String fetchByIdPostPath(String prefix) {
        return prefix + "/api/repo/purchase/fetch-by-id";
    }

    private void fetchByIdPostDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("purchase").summary("Fetch a purchase by ID (POST, supports projection)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchByIdRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/Purchase"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPostPath(prefix), new PathItem().post(operation));
    }

    private void fetchByIdPost(Context ctx) {
        var sc = SecurityEnforcer.require("purchase", "read");
        var reader = new JsonStreamReader(ctx.body());
        Long id = null;
        Purchase projection = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
            case "id" -> id = InputCoerceUtils.asLong(reader);
            case "projection" -> projection = codec.readEntity(reader);
            default -> reader.skipValue();
            }
        }
        reader.endObject();
        var result = repo().fetchById(id, projection != null ? projection : fullProjectionWithItems());
        if (result == null
                || (sc != null && !sc.hasDataAll() && result.user != null && !sc.userId().equals(result.user.id))) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Security helpers

    private static void enforceUserScope(SecurityContext sc, PurchaseCriteria criteria) {
        if (sc != null && !sc.hasDataAll()) {
            criteria.withUserId(sc.userId());
        }
    }

    private static void enforceUserScope(SecurityContext sc, Purchase purchase) {
        if (sc == null || purchase == null) {
            return;
        }
        if (!sc.hasDataAll()) {
            if (purchase.user == null) {
                purchase.user = new User();
            }
            purchase.user.id = sc.userId();
        }
    }

    // :: Helpers

    private PurchaseCriteria readCriteria(JsonStreamReader reader) {
        var criteria = new PurchaseCriteria();
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
