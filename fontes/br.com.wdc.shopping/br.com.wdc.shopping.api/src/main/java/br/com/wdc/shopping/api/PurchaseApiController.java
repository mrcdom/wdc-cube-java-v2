package br.com.wdc.shopping.api;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class PurchaseApiController {

	static void configure(JavalinConfig config) {
		var ctrl = new PurchaseApiController();
		config.routes.post("/api/repo/purchase/insert", ctrl::insert);
		config.routes.post("/api/repo/purchase/update", ctrl::update);
		config.routes.post("/api/repo/purchase/upsert", ctrl::upsert);
		config.routes.post("/api/repo/purchase/delete", ctrl::delete);
		config.routes.post("/api/repo/purchase/count", ctrl::count);
		config.routes.post("/api/repo/purchase/fetch", ctrl::fetch);
		config.routes.post("/api/repo/purchase/fetchById", ctrl::fetchByIdPost);
		config.routes.get("/api/repo/purchase/{id}", ctrl::fetchById);
	}

	private static PurchaseRepository repo() {
		return PurchaseRepository.BEAN.get();
	}

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

	private void insert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var purchase = mapper.readValue(ctx.body(), Purchase.class);
		boolean success = repo().insert(purchase);
		json(ctx, Map.of("success", success, "id", purchase.id != null ? purchase.id : -1));
	}

	private void update(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var newEntity = mapper.treeToValue(body.get("newEntity"), Purchase.class);
		var oldEntity = mapper.treeToValue(body.get("oldEntity"), Purchase.class);
		boolean success = repo().update(newEntity, oldEntity);
		json(ctx, Map.of("success", success));
	}

	private void upsert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var purchase = mapper.readValue(ctx.body(), Purchase.class);
		boolean success = repo().insertOrUpdate(purchase);
		json(ctx, Map.of("success", success, "id", purchase.id != null ? purchase.id : -1));
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

		var projection = ApiObjectMapper.parseProjection(body, Purchase.class);
		if (projection == null) {
			boolean includeItems = body.has("includeItems") && body.get("includeItems").asBoolean(true);
			projection = includeItems ? fullProjectionWithItems() : simpleProjection();
		}
		criteria.withProjection(projection);

		var items = repo().fetch(criteria);
		clearCircularRefs(items);
		json(ctx, Map.of("items", items));
	}

	private void fetchById(Context ctx) throws Exception {
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, fullProjectionWithItems());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		clearCircularRefs(result);
		json(ctx, result);
	}

	private void fetchByIdPost(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		Long id = body.get("id").asLong();
		var projection = ApiObjectMapper.parseProjection(body, Purchase.class);
		var result = repo().fetchById(id, projection != null ? projection : fullProjectionWithItems());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		clearCircularRefs(result);
		json(ctx, result);
	}

	private static void clearCircularRefs(java.util.List<Purchase> purchases) {
		for (var purchase : purchases) {
			clearCircularRefs(purchase);
		}
	}

	private static void clearCircularRefs(Purchase purchase) {
		if (purchase.items != null) {
			for (var item : purchase.items) {
				item.purchase = null;
			}
		}
	}

	private static PurchaseCriteria parseCriteria(JsonNode body) {
		var criteria = new PurchaseCriteria();
		if (hasValue(body, "purchaseId"))
			criteria.withPurchaseId(body.get("purchaseId").asLong());
		if (hasValue(body, "userId"))
			criteria.withUserId(body.get("userId").asLong());
		if (hasValue(body, "offset"))
			criteria.withOffset(body.get("offset").asInt());
		if (hasValue(body, "limit"))
			criteria.withLimit(body.get("limit").asInt());
		if (hasValue(body, "orderBy"))
			criteria.withOrderBy(PurchaseCriteria.OrderBy.valueOf(body.get("orderBy").asText()));
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
