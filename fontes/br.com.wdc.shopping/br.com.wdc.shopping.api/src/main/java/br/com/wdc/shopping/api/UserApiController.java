package br.com.wdc.shopping.api;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class UserApiController {

	static void configure(JavalinConfig config) {
		var ctrl = new UserApiController();
		config.routes.post("/api/repo/user/insert", ctrl::insert);
		config.routes.post("/api/repo/user/update", ctrl::update);
		config.routes.post("/api/repo/user/upsert", ctrl::upsert);
		config.routes.post("/api/repo/user/delete", ctrl::delete);
		config.routes.post("/api/repo/user/count", ctrl::count);
		config.routes.post("/api/repo/user/fetch", ctrl::fetch);
		config.routes.post("/api/repo/user/fetchById", ctrl::fetchByIdPost);
		config.routes.get("/api/repo/user/{id}", ctrl::fetchById);
	}

	private static UserRepository repo() {
		return UserRepository.BEAN.get();
	}

	private static User fullProjection() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new User();
		prj.id = pv.i64;
		prj.userName = pv.str;
		prj.password = pv.str;
		prj.name = pv.str;
		return prj;
	}

	private void insert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var user = mapper.readValue(ctx.body(), User.class);
		boolean success = repo().insert(user);
		json(ctx, Map.of("success", success, "id", user.id != null ? user.id : -1));
	}

	private void update(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var newEntity = mapper.treeToValue(body.get("newEntity"), User.class);
		var oldEntity = mapper.treeToValue(body.get("oldEntity"), User.class);
		boolean success = repo().update(newEntity, oldEntity);
		json(ctx, Map.of("success", success));
	}

	private void upsert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var user = mapper.readValue(ctx.body(), User.class);
		boolean success = repo().insertOrUpdate(user);
		json(ctx, Map.of("success", success, "id", user.id != null ? user.id : -1));
	}

	private void delete(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		int count = repo().delete(parseCriteria(body));
		json(ctx, Map.of("count", count));
	}

	private void count(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		int count = repo().count(parseCriteria(body));
		json(ctx, Map.of("count", count));
	}

	private void fetch(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		var criteria = parseCriteria(body);
		var projection = ApiObjectMapper.parseProjection(body, User.class);
		criteria.withProjection(projection != null ? projection : fullProjection());
		var items = repo().fetch(criteria);
		json(ctx, Map.of("items", items));
	}

	private void fetchById(Context ctx) throws Exception {
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, fullProjection());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		json(ctx, result);
	}

	private void fetchByIdPost(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		Long id = body.get("id").asLong();
		var projection = ApiObjectMapper.parseProjection(body, User.class);
		var result = repo().fetchById(id, projection != null ? projection : fullProjection());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		json(ctx, result);
	}

	private static UserCriteria parseCriteria(JsonNode body) {
		var criteria = new UserCriteria();
		if (hasValue(body, "userId"))
			criteria.withUserId(body.get("userId").asLong());
		if (hasValue(body, "userName"))
			criteria.withUserName(body.get("userName").asText());
		if (hasValue(body, "password"))
			criteria.withPassword(body.get("password").asText());
		if (hasValue(body, "offset"))
			criteria.withOffset(body.get("offset").asInt());
		if (hasValue(body, "limit"))
			criteria.withLimit(body.get("limit").asInt());
		if (hasValue(body, "orderBy"))
			criteria.withOrderBy(UserCriteria.OrderBy.valueOf(body.get("orderBy").asText()));
		return criteria;
	}

	private static boolean hasValue(JsonNode node, String field) {
		return node.has(field) && !node.get(field).isNull();
	}

	private static void json(Context ctx, Object obj) throws Exception {
		ctx.contentType("application/json");
		ctx.result(ApiObjectMapper.get().writeValueAsString(obj));
	}
}
