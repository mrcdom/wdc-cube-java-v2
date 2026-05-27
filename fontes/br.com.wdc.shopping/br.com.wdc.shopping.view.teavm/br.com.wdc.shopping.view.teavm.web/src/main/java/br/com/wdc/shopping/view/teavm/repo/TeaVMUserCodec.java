package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.persistence.client.ModelCodec;

public class TeaVMUserCodec implements ModelCodec<User, UserCriteria> {

    @Override
    public JsonObject entityToJson(User entity) {
        return JsonModelParser.userToJson(entity);
    }

    @Override
    public User entityFromJson(JsonObject json) {
        return JsonModelParser.parseUser(json);
    }

    @Override
    public List<User> entityListFromJson(JsonArray array) {
        return JsonModelParser.parseUserList(array);
    }

    @Override
    public JsonObject criteriaToJson(UserCriteria criteria) {
        var body = new JsonObject();
        if (criteria.userId() != null) body.addProperty("userId", criteria.userId());
        if (criteria.userName() != null) body.addProperty("userName", criteria.userName());
        if (criteria.password() != null) body.addProperty("password", criteria.password());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    @Override
    public User getProjection(UserCriteria criteria) {
        return criteria.projection();
    }

    @Override
    public void addProjection(JsonObject body, User projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.userToJson(projection));
        }
    }

    @Override
    public void setGeneratedId(User entity, long id) {
        entity.id = id;
    }
}
