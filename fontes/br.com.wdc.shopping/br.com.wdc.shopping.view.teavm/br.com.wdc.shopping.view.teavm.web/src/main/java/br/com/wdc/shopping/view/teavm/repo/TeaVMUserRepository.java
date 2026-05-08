package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.api.client.HttpTransport;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;

/**
 * Implementação de {@link UserRepository} sem Gson reflection.
 * Usa parsing manual de JsonObject.
 */
public class TeaVMUserRepository implements UserRepository {

    private final HttpTransport transport;

    public TeaVMUserRepository(HttpTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean insert(User user) {
        var body = JsonModelParser.userToJson(user);
        var result = transport.postJson("/api/repo/user/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            user.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(User newUser, User oldUser) {
        var body = new JsonObject();
        body.add("newEntity", JsonModelParser.userToJson(newUser));
        body.add("oldEntity", JsonModelParser.userToJson(oldUser));
        return transport.postJson("/api/repo/user/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(User user) {
        var body = JsonModelParser.userToJson(user);
        var result = transport.postJson("/api/repo/user/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            user.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(UserCriteria criteria) {
        return transport.postJson("/api/repo/user/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(UserCriteria criteria) {
        return transport.postJson("/api/repo/user/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<User> fetch(UserCriteria criteria) {
        var body = buildCriteria(criteria);
        addProjection(body, criteria.projection());
        var result = transport.postJson("/api/repo/user/fetch", body);
        return JsonModelParser.parseUserList(result.getAsJsonArray("items"));
    }

    @Override
    public User fetchById(Long userId, User projection) {
        var body = new JsonObject();
        body.addProperty("id", userId);
        addProjection(body, projection);
        var result = transport.postJsonNullable("/api/repo/user/fetchById", body);
        if (result == null) return null;
        return JsonModelParser.parseUser(result);
    }

    private JsonObject buildCriteria(UserCriteria criteria) {
        var body = new JsonObject();
        if (criteria.userId() != null) body.addProperty("userId", criteria.userId());
        if (criteria.userName() != null) body.addProperty("userName", criteria.userName());
        if (criteria.password() != null) body.addProperty("password", criteria.password());
        if (criteria.offset() != null) body.addProperty("offset", criteria.offset());
        if (criteria.limit() != null) body.addProperty("limit", criteria.limit());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    private void addProjection(JsonObject body, User projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.userToJson(projection));
        }
    }

}
