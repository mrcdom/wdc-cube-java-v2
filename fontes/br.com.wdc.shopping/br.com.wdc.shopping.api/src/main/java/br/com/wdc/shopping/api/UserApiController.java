package br.com.wdc.shopping.api;

import java.security.PrivateKey;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class UserApiController {

	private static final Logger LOG = LoggerFactory.getLogger(UserApiController.class);

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

	private void insert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var user = mapper.readValue(ctx.body(), User.class);
		decryptPasswordIfPresent(user);
		boolean success = repo().insert(user);
		json(ctx, Map.of("success", success, "id", user.id != null ? user.id : -1));
	}

	private void update(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var newEntity = mapper.treeToValue(body.get("newEntity"), User.class);
		var oldEntity = mapper.treeToValue(body.get("oldEntity"), User.class);
		decryptPasswordIfPresent(newEntity);
		boolean success = repo().update(newEntity, oldEntity);
		json(ctx, Map.of("success", success));
	}

	private void upsert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var user = mapper.readValue(ctx.body(), User.class);
		decryptPasswordIfPresent(user);
		boolean success = repo().insertOrUpdate(user);
		json(ctx, Map.of("success", success, "id", user.id != null ? user.id : -1));
	}

	private void delete(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		var criteria = parseCriteria(body);
		int count = repo().delete(criteria);
		json(ctx, Map.of("count", count));
	}

	private void count(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		var criteria = parseCriteria(body);
		int count = repo().count(criteria);
		json(ctx, Map.of("count", count));
	}

	private void fetch(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		var criteria = parseCriteria(body);
		var projection = ApiObjectMapper.parseProjection(body, User.class);
		criteria.withProjection(projection);
		var items = repo().fetch(criteria);
		json(ctx, Map.of("items", items));
	}

	private void fetchById(Context ctx) throws Exception {
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, null);
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
		var result = repo().fetchById(id, projection);
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		json(ctx, result);
	}

	// :: Transport-level helpers

	/**
	 * Decripta a senha se presente e criptografada com RSA (chave da sessão).
	 */
	private static void decryptPasswordIfPresent(User user) {
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

	// :: Parsing

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
