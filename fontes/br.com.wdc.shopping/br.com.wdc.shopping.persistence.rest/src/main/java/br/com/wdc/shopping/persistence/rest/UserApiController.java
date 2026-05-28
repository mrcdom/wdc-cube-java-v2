package br.com.wdc.shopping.persistence.rest;

import java.security.PrivateKey;
import java.util.Base64;

import javax.crypto.Cipher;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.codec.UserModelCodec;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class UserApiController {

    private static final Log LOG = Log.getLogger(UserApiController.class);

    static void configure(JavalinConfig config) {
        configure(config, "");
    }

    static void configure(JavalinConfig config, String prefix) {
        var ctrl = new UserApiController();
        config.routes.post(prefix + "/api/repo/user/insert", ctrl::insert);
        config.routes.post(prefix + "/api/repo/user/update", ctrl::update);
        config.routes.post(prefix + "/api/repo/user/delete", ctrl::delete);
        config.routes.post(prefix + "/api/repo/user/count", ctrl::count);
        config.routes.post(prefix + "/api/repo/user/fetch", ctrl::fetch);
        config.routes.post(prefix + "/api/repo/user/fetch-page", ctrl::fetchPage);
        config.routes.post(prefix + "/api/repo/user/fetch-by-id", ctrl::fetchByIdPost);
        config.routes.get(prefix + "/api/repo/user/{id}", ctrl::fetchById);
    }

    private static UserRepository repo() {
        return UserRepository.BEAN.get();
    }

    private final UserModelCodec codec = new UserModelCodec();

    private void insert(Context ctx) {
        var reader = new JsonStreamReader(ctx.body());
        var user = codec.readEntity(reader);
        decryptPasswordIfPresent(user);
        boolean success = repo().insert(user);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.name("id").value(user.id != null ? user.id : -1);
        writer.endObject();
        json(ctx, writer);
    }

    private void update(Context ctx) {
        var reader = new JsonStreamReader(ctx.body());
        ModelCodec.UpdateData<User> newData = null;
        User oldEntity = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "newEntity" -> newData = codec.readEntityForUpdate(reader);
                case "oldEntity" -> oldEntity = codec.readEntity(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        if (newData == null) {
            throw new IllegalArgumentException("Missing 'newEntity' in request body");
        }
        decryptPasswordIfPresent(newData.entity());
        boolean success = repo().update(newData.entity(), oldEntity, newData.projection());
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(success);
        writer.endObject();
        json(ctx, writer);
    }

    private void delete(Context ctx) {
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        int count = repo().delete(criteria);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("count").value(count);
        writer.endObject();
        json(ctx, writer);
    }

    private void count(Context ctx) {
        var reader = new JsonStreamReader(ctx.body());
        var criteria = readCriteria(reader);
        int count = repo().count(criteria);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("count").value(count);
        writer.endObject();
        json(ctx, writer);
    }

    private void fetch(Context ctx) {
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
                default -> { if (!codec.readCriteriaField(reader, name, criteria)) reader.skipValue(); }
            }
        }
        reader.endObject();
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

    private void fetchPage(Context ctx) {
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
                default -> { if (!codec.readCriteriaField(reader, name, criteria)) reader.skipValue(); }
            }
        }
        reader.endObject();
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

    private void fetchById(Context ctx) {
        Long id = Long.parseLong(ctx.pathParam("id"));
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

    private void fetchByIdPost(Context ctx) {
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
        var sc = SecurityContextHolder.get();
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
