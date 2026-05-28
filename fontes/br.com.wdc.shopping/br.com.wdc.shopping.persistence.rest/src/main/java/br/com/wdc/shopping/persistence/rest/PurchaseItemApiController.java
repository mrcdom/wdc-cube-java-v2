package br.com.wdc.shopping.persistence.rest;

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
		configure(config, "");
	}

	static void configure(JavalinConfig config, String prefix) {
		var ctrl = new PurchaseItemApiController();
		config.routes.post(prefix + "/api/repo/purchase-item/insert", ctrl::insert);
		config.routes.post(prefix + "/api/repo/purchase-item/update", ctrl::update);
		config.routes.post(prefix + "/api/repo/purchase-item/delete", ctrl::delete);
		config.routes.post(prefix + "/api/repo/purchase-item/count", ctrl::count);
		config.routes.post(prefix + "/api/repo/purchase-item/fetch", ctrl::fetch);
		config.routes.post(prefix + "/api/repo/purchase-item/fetch-page", ctrl::fetchPage);
		config.routes.post(prefix + "/api/repo/purchase-item/fetch-by-id", ctrl::fetchByIdPost);
		config.routes.get(prefix + "/api/repo/purchase-item/{id}", ctrl::fetchById);
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
		var newEntityNode = body.get("newEntity");
		var newEntity = mapper.treeToValue(newEntityNode, PurchaseItem.class);
		var oldNode = body.get("oldEntity");
		var oldEntity = oldNode != null ? mapper.treeToValue(oldNode, PurchaseItem.class) : null;
		setPurchaseFromJson(newEntityNode, newEntity);
		if (oldEntity != null) setPurchaseFromJson(oldNode, oldEntity);
		var projection = buildUpdateProjection(newEntityNode);
		boolean success = repo().update(newEntity, oldEntity, projection);
		json(ctx, Map.of("success", success));
	}

	private static PurchaseItem buildUpdateProjection(JsonNode node) {
		var pv = ProjectionValues.INSTANCE;
		var prj = new PurchaseItem();
		if (node.has("id")) prj.id = pv.i64;
		if (node.has("amount")) prj.amount = pv.i32;
		if (node.has("price")) prj.price = pv.f64;
		if (node.has("product")) {
			prj.product = new Product();
			prj.product.id = pv.i64;
		}
		if (node.has("purchaseId") || node.has("purchase")) {
			prj.purchase = new Purchase();
			prj.purchase.id = pv.i64;
		}
		return prj;
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
		var projection = ApiObjectMapper.parseProjection(body, PurchaseItem.class);
		criteria.withProjection(projection != null ? projection : fullProjection());
		int offset = hasValue(body, "offset") ? body.get("offset").asInt() : 0;
		int limit = hasValue(body, "limit") ? body.get("limit").asInt() : 0;
		var items = repo().fetch(criteria, offset, limit);
		for (var item : items) {
			item.purchase = null;
		}
		json(ctx, Map.of("items", items));
	}

	private void fetchPage(Context ctx) throws Exception {
		var body = ApiObjectMapper.get().readTree(ctx.body());
		var criteria = parseCriteria(body);
		var projection = ApiObjectMapper.parseProjection(body, PurchaseItem.class);
		criteria.withProjection(projection != null ? projection : fullProjection());
		int pageIx = hasValue(body, "page") ? body.get("page").asInt() : 0;
		int pageSz = hasValue(body, "pageSize") ? body.get("pageSize").asInt() : 0;
		var page = repo().fetchPage(criteria, pageIx, pageSz);
		for (var item : page.items()) {
			item.purchase = null;
		}
		json(ctx, Map.of("items", page.items(), "totalItems", page.totalItems()));
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
