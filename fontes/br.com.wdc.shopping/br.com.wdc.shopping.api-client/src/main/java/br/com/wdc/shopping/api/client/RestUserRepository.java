package br.com.wdc.shopping.api.client;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;

public class RestUserRepository implements UserRepository {

    private static final Type USER_LIST_TYPE = new TypeToken<List<User>>() {}.getType();

    private final RestConfig config;

    public RestUserRepository(RestConfig config) {
        this.config = config;
    }

    @Override
    public boolean insert(User user) {
        var body = config.gson().toJsonTree(user).getAsJsonObject();
        var result = config.postJson("/api/repo/user/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            user.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(User newUser, User oldUser) {
        var body = new JsonObject();
        body.add("newEntity", config.gson().toJsonTree(newUser));
        body.add("oldEntity", config.gson().toJsonTree(oldUser));
        return config.postJson("/api/repo/user/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(User user) {
        var body = config.gson().toJsonTree(user).getAsJsonObject();
        var result = config.postJson("/api/repo/user/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            user.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(UserCriteria criteria) {
        return config.postJson("/api/repo/user/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(UserCriteria criteria) {
        return config.postJson("/api/repo/user/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<User> fetch(UserCriteria criteria) {
        var body = buildCriteria(criteria);
        config.addProjection(body, criteria.projection());
        var result = config.postJson("/api/repo/user/fetch", body);
        return config.gson().fromJson(result.getAsJsonArray("items"), USER_LIST_TYPE);
    }

    @Override
    public User fetchById(Long userId, User projection) {
        var body = new JsonObject();
        body.addProperty("id", userId);
        config.addProjection(body, projection);
        var result = config.postJsonNullable("/api/repo/user/fetchById", body);
        if (result == null) return null;
        return config.gson().fromJson(result, User.class);
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
}
