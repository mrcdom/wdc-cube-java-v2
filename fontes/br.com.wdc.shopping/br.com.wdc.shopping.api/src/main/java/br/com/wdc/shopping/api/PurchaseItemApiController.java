package br.com.wdc.shopping.api;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class PurchaseItemApiController {

	static void configure(JavalinConfig config) {
		var ctrl = new PurchaseItemApiController();
		config.routes.post("/api/repo/purchase-item/insert", ctrl::insert);
		config.routes.post("/api/repo/purchase-item/update", ctrl::update);
		config.routes.post("/api/repo/purchase-item/upsert", ctrl::upsert);
		config.routes.post("/api/repo/purchase-item/delete", ctrl::delete);
		config.routes.post("/api/repo/purchase-item/count", ctrl::count);
		config.routes.post("/api/repo/purchase-item/fetch", ctrl::fetch);
		config.routes.post("/api/repo/purchase-item/fetchById", ctrl::fetchByIdPost);
		config.routes.get("/api/repo/purchase-item/{id}", ctrl::fetchById);
	}

	private static PurchaseItemRepository repo() {
		return PurchaseItemRepository.BEAN.get();
	}

	private static PurchaseItem fullProjection() {
		var pv = ProjectionValues.INSTANCE;

		var product = new Product();
		product.id = pv.i64;
		product.name = pv.str;
		product.price = pv.f64;

		var prj = new PurchaseItem();
		prj.id = pv.i64;
		prj.amount = pv.i32;
		prj.price = pv.f64;
		prj.product = product;
		return prj;
	}

	private void insert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var item = mapper.treeToValue(body, PurchaseItem.class);
		setPurchaseFromJson(body, item);
		boolean success = repo().insert(item);
		json(ctx, Map.of("success", success, "id", item.id != null ? item.id : -1));
	}

	private void update(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var newEntity = mapper.treeToValue(body.get("newEntity"), PurchaseItem.class);
		var oldEntity = mapper.treeToValue(body.get("oldEntity"), PurchaseItem.class);
		boolean success = repo().update(newEntity, oldEntity);
		json(ctx, Map.of("success", success));
	}

	private void upsert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var item = mapper.treeToValue(body, PurchaseItem.class);
		setPurchaseFromJson(body, item);
		boolean success = repo().insertOrUpdate(item);
		json(ctx, Map.of("success", success, "id", item.id != null ? item.id : -1));
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
		var projection = ApiObjectMapper.parseProjection(body, PurchaseItem.class);
		criteria.withProjection(projection != null ? projection : fullProjection());
		var items = repo().fetch(criteria);
		for (var item : items) {
			item.purchase = null;
		}
		json(ctx, Map.of("items", items));
	}

	private void fetchById(Context ctx) throws Exception {
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, fullProjection());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		result.purchase = null;
		json(ctx, result);
	}

	private void fetchByIdPost(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		Long id = body.get("id").asLong();
		var projection = ApiObjectMapper.parseProjection(body, PurchaseItem.class);
		var result = repo().fetchById(id, projection != null ? projection : fullProjection());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		result.purchase = null;
		json(ctx, result);
	}

	private static PurchaseItemCriteria parseCriteria(JsonNode body) {
		var criteria = new PurchaseItemCriteria();
		if (hasValue(body, "purchaseItemId"))
			criteria.withPurchaseItemId(body.get("purchaseItemId").asLong());
		if (hasValue(body, "purchaseId"))
			criteria.withPurchaseId(body.get("purchaseId").asLong());
		if (hasValue(body, "productId"))
			criteria.withProductId(body.get("productId").asLong());
		if (hasValue(body, "userId"))
			criteria.withUserId(body.get("userId").asLong());
		if (hasValue(body, "offset"))
			criteria.withOffset(body.get("offset").asInt());
		if (hasValue(body, "limit"))
			criteria.withLimit(body.get("limit").asInt());
		if (hasValue(body, "orderBy"))
			criteria.withOrderBy(PurchaseItemCriteria.OrderBy.valueOf(body.get("orderBy").asText()));
		return criteria;
	}

	private static boolean hasValue(JsonNode node, String field) {
		return node.has(field) && !node.get(field).isNull();
	}

	private static void json(Context ctx, Object obj) throws Exception {
		ctx.contentType("application/json");
		ctx.result(ApiObjectMapper.get().writeValueAsString(obj));
	}

	private static void setPurchaseFromJson(JsonNode body, PurchaseItem item) {
		if (body.has("purchaseId") && !body.get("purchaseId").isNull()) {
			item.purchase = new Purchase();
			item.purchase.id = body.get("purchaseId").asLong();
		} else if (body.has("purchase") && !body.get("purchase").isNull()) {
			var purchaseNode = body.get("purchase");
			if (purchaseNode.has("id") && !purchaseNode.get("id").isNull()) {
				item.purchase = new Purchase();
				item.purchase.id = purchaseNode.get("id").asLong();
			}
		}
	}
}
