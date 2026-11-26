package br.com.wdc.store.persistence.rest;

import java.security.PrivateKey;
import java.util.Base64;

import javax.crypto.Cipher;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.security.PasswordVerifiers;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.security.policy.DataAccess;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserCodec;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.rest.doc.Doc;
import br.com.wdc.store.persistence.rest.security.SecurityEnforcer;
import br.com.wdc.store.persistence.rest.security.UserDataPolicy;
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

    private final UserCodec codec = new UserCodec();

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
        var access = access();
        var reader = new JsonStreamReader(ctx.body());
        var user = codec.readEntity(reader);
        access.checkCreate(user);
        storablePassword(user);
        boolean success = repo().insert(user);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("id").value(user.id() != null ? user.id() : -1);
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
        var access = access();
        var reader = new JsonStreamReader(ctx.body());
        var data = codec.readEntityForUpdate(reader);
        access.checkUpdate(data.entity(), data.projection(), (row, projection) -> repo().fetchById(row.id(), projection));
        storablePassword(data.entity());
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
                .requestBody(Doc.body("#/components/schemas/UserFetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("403", Doc.forbidden()));
        api.path(deletePath(prefix), new PathItem().post(operation));
    }

    private void delete(Context ctx) {
        var access = access();
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        access.restrictDelete(criteria);
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
                .requestBody(Doc.body("#/components/schemas/UserFetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/CountResult"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(countPath(prefix), new PathItem().post(operation));
    }

    private void count(Context ctx) {
        var access = access();
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        access.restrictCount(criteria);
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
                .requestBody(Doc.body("#/components/schemas/UserFetchRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/UserFetchResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPath(prefix), new PathItem().post(operation));
    }

    private void fetch(Context ctx) {
        var access = access();
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
        var plan = access.prepareRead(criteria);
        var items = plan.apply(repo().fetch(criteria, offset, limit));
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : items) {
            writeUser(writer, item);
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
                .requestBody(Doc.body("#/components/schemas/UserPageRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/UserPageResponse"))
                        .addApiResponse("401", Doc.unauthorized()));
        api.path(fetchPagePath(prefix), new PathItem().post(operation));
    }

    private void fetchPage(Context ctx) {
        var access = access();
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
        var plan = access.prepareRead(criteria);
        var page = repo().fetchPage(criteria, pageIx, pageSz);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("items").beginArray();
        for (var item : plan.apply(page.items())) {
            writeUser(writer, item);
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
        var access = access();
        Long id = Long.parseLong(ctx.pathParam("id"));
        var result = fetchOne(access, id, null);
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var writer = new JsonStreamWriter();
        writeUser(writer, result);
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
        var access = access();
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
        var result = fetchOne(access, id, projection);
        if (result == null) {
            ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
            return;
        }
        var writer = new JsonStreamWriter();
        writeUser(writer, result);
        json(ctx, writer);
    }

    // :: Security helpers

    /**
     * What the requester may do with the users, by {@link UserDataPolicy}; without security (local or test mode),
     * everything.
     */
    private static DataAccess<User, UserCriteria> access() {
        return UserDataPolicy.POLICY.accessFor(SecurityEnforcer.requireAuthenticated());
    }

    /** The user by id, through the same rules as a fetch; {@code null} when absent or out of reach. */
    private static User fetchOne(DataAccess<User, UserCriteria> access, Long id, User projection) {
        if (id == null) {
            return null;
        }
        var criteria = new UserCriteria()
                .withUserId(id)
                .withProjection(projection);
        var plan = access.prepareRead(criteria);
        var users = repo().fetch(criteria, 0, 1);
        return users.isEmpty() ? null : plan.apply(users.get(0));
    }

    /** The password never leaves the server, with or without security. */
    private void writeUser(JsonStreamWriter writer, User user) {
        user.withPassword(null);
        codec.writeEntity(writer, user);
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
     * Turns the password that came into the one stored: its verifier ({@link PasswordVerifiers}). With a session, the
     * password must come encrypted with the session's RSA key; one that does not decrypt is refused, never stored as it
     * came.
     */
    private static void storablePassword(User user) {
        if (user == null || user.password() == null || user.password().isBlank()) {
            return;
        }
        var sc = SecurityContext.CURRENT.get();
        var plain = user.password();
        if (sc != null) {
            try {
                plain = rsaDecrypt(user.password(), sc.privateKey());
            } catch (Exception e) {
                LOG.debug("Password not encrypted with the session key: {}", e.getMessage());
                throw new InvalidRequestException("The password must come encrypted with the session key");
            }
        }
        user.withPassword(PasswordVerifiers.fromPassword(plain));
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
