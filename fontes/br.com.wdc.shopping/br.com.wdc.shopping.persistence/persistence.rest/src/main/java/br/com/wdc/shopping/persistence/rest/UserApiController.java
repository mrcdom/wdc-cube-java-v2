package br.com.wdc.shopping.persistence.rest;

import java.security.PrivateKey;
import java.util.Base64;

import javax.crypto.Cipher;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.UserModelCodec;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.shopping.persistence.rest.doc.Doc;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class UserApiController {

    private static final Log LOG = Log.getLogger(UserApiController.class);

    public static void configure(JavalinConfig config, String prefix) {
        var ctrl = new UserApiController();
        config.routes.post(insertPath(prefix), RepositoryApiRoutes.transactional(ctrl::insert));
        config.routes.post(updatePath(prefix), RepositoryApiRoutes.transactional(ctrl::update));
        config.routes.post(deletePath(prefix), RepositoryApiRoutes.transactional(ctrl::delete));
        config.routes.post(countPath(prefix), ctrl::count);
        config.routes.post(fetchPath(prefix), ctrl::fetch);
        config.routes.post(fetchPagePath(prefix), ctrl::fetchPage);
        config.routes.post(fetchByIdPostPath(prefix), ctrl::fetchByIdPost);
        config.routes.get(fetchByIdPath(prefix), ctrl::fetchById);
    }

    public static void openApi(OpenAPI api, String prefix) {
        var ctrl = new UserApiController();
        ctrl.insertDoc(api, prefix);
        ctrl.updateDoc(api, prefix);
        ctrl.deleteDoc(api, prefix);
        ctrl.countDoc(api, prefix);
        ctrl.fetchDoc(api, prefix);
        ctrl.fetchPageDoc(api, prefix);
        ctrl.fetchByIdDoc(api, prefix);
        ctrl.fetchByIdPostDoc(api, prefix);
    }

    private static UserRepository repo() {
        return UserRepository.BEAN.get();
    }

    private final UserModelCodec codec = new UserModelCodec();

    // :: Insert

    private static String insertPath(String prefix) {
        return prefix + "/api/repo/user/insert";
    }

    private void insertDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Insert a user")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/User"))
                .responses(new ApiResponses()
                        .addApiResponse("201", Doc.ok("#/components/schemas/InsertResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(insertPath(prefix), new PathItem().post(operation));
    }

    private void insert(Context ctx) {
        var sc = SecurityEnforcer.require("user", "write");
        var reader = new JsonStreamReader(ctx.body());
        var user = codec.readEntity(reader);
        decryptPasswordIfPresent(user);
        enforceUserScope(sc, user);
        boolean success = repo().insert(user);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("id").value(user.id != null ? user.id : -1);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Update

    private static String updatePath(String prefix) {
        return prefix + "/api/repo/user/update";
    }

    private void updateDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Update a user")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/User"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/MutationResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(updatePath(prefix), new PathItem().post(operation));
    }

    private void update(Context ctx) {
        var sc = SecurityEnforcer.require("user", "write");
        var reader = new JsonStreamReader(ctx.body());
        var data = codec.readEntityForUpdate(reader);
        decryptPasswordIfPresent(data.entity());
        enforceUserScope(sc, data.entity());
        boolean success = repo().update(data.entity(), null, data.projection());
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.endObject();
        json(ctx, writer);
    }

    // :: Delete

    private static String deletePath(String prefix) {
        return prefix + "/api/repo/user/delete";
    }

    private void deleteDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Delete users matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(deletePath(prefix), new PathItem().post(operation));
    }

    private void delete(Context ctx) {
        var sc = SecurityEnforcer.require("user", "delete");
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
        return prefix + "/api/repo/user/count";
    }

    private void countDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Count users matching criteria")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(countPath(prefix), new PathItem().post(operation));
    }

    private void count(Context ctx) {
        var sc = SecurityEnforcer.require("user", "read");
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
        return prefix + "/api/repo/user/fetch";
    }

    private void fetchDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Fetch users matching criteria (offset/limit)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/UserFetchResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPath(prefix), new PathItem().post(operation));
    }

    private void fetch(Context ctx) {
        var sc = SecurityEnforcer.require("user", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new UserCriteria();
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
        sanitizeProjection(criteria);
        var items = repo().fetch(criteria, offset, limit);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : items) {
            item.password = null;
            codec.writeEntity(writer, item);
        }
        writer.endArray();
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch Page

    private static String fetchPagePath(String prefix) {
        return prefix + "/api/repo/user/fetch-page";
    }

    private void fetchPageDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Fetch users matching criteria (page/pageSize)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/PageRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/UserPageResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPagePath(prefix), new PathItem().post(operation));
    }

    private void fetchPage(Context ctx) {
        var sc = SecurityEnforcer.require("user", "read");
        var reader = new JsonStreamReader(ctx.body());
        var criteria = new UserCriteria();
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
        enforceUserScope(sc, criteria);
        sanitizeProjection(criteria);
        var page = repo().fetchPage(criteria, pageIx, pageSz);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : page.items()) {
            item.password = null;
            codec.writeEntity(writer, item);
        }
        writer.endArray();
        writer.name("totalItems").value(page.totalItems());
        writer.endObject();
        json(ctx, writer);
    }

    // :: Fetch by ID

    private static String fetchByIdPath(String prefix) {
        return prefix + "/api/repo/user/{id}";
    }

    private void fetchByIdDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Fetch a user by ID (GET)")
                .security(Doc.BEARER)
                .addParametersItem(Doc.pathId())
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/User"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPath(prefix), new PathItem().get(operation));
    }

    private void fetchById(Context ctx) {
        var sc = SecurityEnforcer.require("user", "read");
        Long id = Long.parseLong(ctx.pathParam("id"));
        if (sc != null && !sc.hasDataAll() && !id.equals(sc.userId())) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var result = repo().fetchById(id, null);
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        result.password = null;
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Fetch by ID (POST)

    private static String fetchByIdPostPath(String prefix) {
        return prefix + "/api/repo/user/fetch-by-id";
    }

    private void fetchByIdPostDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("user").summary("Fetch a user by ID (POST, supports projection)")
                .security(Doc.BEARER)
                .requestBody(Doc.body("#/components/schemas/FetchByIdRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/User"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("404", Doc.notFound()));
        api.path(fetchByIdPostPath(prefix), new PathItem().post(operation));
    }

    private void fetchByIdPost(Context ctx) {
        var sc = SecurityEnforcer.require("user", "read");
        var reader = new JsonStreamReader(ctx.body());
        Long id = null;
        User projection = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
            case "id" -> id = InputCoerceUtils.asLong(reader);
            case "projection" -> projection = codec.readEntity(reader);
            default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (sc != null && !sc.hasDataAll() && id != null && !id.equals(sc.userId())) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        if (projection != null)
            projection.password = null;
        var result = repo().fetchById(id, projection);
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        result.password = null;
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, result);
        json(ctx, writer);
    }

    // :: Security helpers

    private static void enforceUserScope(SecurityContext sc, UserCriteria criteria) {
        if (sc != null && !sc.hasDataAll()) {
            criteria.withUserId(sc.userId());
        }
    }

    private static void enforceUserScope(SecurityContext sc, User user) {
        if (sc == null || user == null) {
            return;
        }
        if (!sc.hasDataAll() && user.id != null && !user.id.equals(sc.userId())) {
            throw new AccessDeniedException("Cannot modify other user's data");
        }
    }

    private static void sanitizeProjection(UserCriteria criteria) {
        if (criteria.projection() != null) {
            criteria.projection().password = null;
        }
    }

    // :: Helpers

    private UserCriteria readCriteria(JsonStreamReader reader) {
        var criteria = new UserCriteria();
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

    /**
     * Decripta a senha se presente e criptografada com RSA (chave da sessão).
     */
    private static void decryptPasswordIfPresent(User user) {
        if (user == null) {
            return;
        }
        var sc = SecurityContext.CURRENT.get();
        if (sc != null && user.password != null && !user.password.isBlank()) {
            try {
                user.password = rsaDecrypt(user.password, sc.privateKey());
            } catch (Exception e) {
                LOG.debug("Password not RSA-encrypted or decryption failed, using as-is");
            }
        }
    }

    private static String rsaDecrypt(String encryptedBase64, PrivateKey privateKey) throws Exception {
        var cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        var decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void json(Context ctx, JsonStreamWriter writer) {
        ctx.contentType("application/json");
        ctx.result(writer.result());
    }

}
