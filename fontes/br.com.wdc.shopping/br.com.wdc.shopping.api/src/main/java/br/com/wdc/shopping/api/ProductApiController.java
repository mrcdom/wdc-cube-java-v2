package br.com.wdc.shopping.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class ProductApiController {

	private static final Logger LOG = LoggerFactory.getLogger(ProductApiController.class);

	static void configure(JavalinConfig config) {
		var ctrl = new ProductApiController();
		config.routes.post("/api/repo/product/insert", ctrl::insert);
		config.routes.post("/api/repo/product/update", ctrl::update);
		config.routes.post("/api/repo/product/upsert", ctrl::upsert);
		config.routes.post("/api/repo/product/delete", ctrl::delete);
		config.routes.post("/api/repo/product/count", ctrl::count);
		config.routes.post("/api/repo/product/fetch", ctrl::fetch);
		config.routes.post("/api/repo/product/fetchById", ctrl::fetchByIdPost);
		config.routes.get("/api/repo/product/{id}", ctrl::fetchById);
		config.routes.get("/api/repo/product/{id}/image", ctrl::fetchImage);
		config.routes.put("/api/repo/product/{id}/image", ctrl::updateImage);
	}

	private static ProductRepository repo() {
		return ProductRepository.BEAN.get();
	}

	private static Product fullProjection() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new Product();
		prj.id = pv.i64;
		prj.name = pv.str;
		prj.price = pv.f64;
		prj.description = pv.str;
		return prj;
	}

	private void insert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var product = mapper.readValue(ctx.body(), Product.class);
		boolean success = repo().insert(product);
		json(ctx, Map.of("success", success, "id", product.id != null ? product.id : -1));
	}

	private void update(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());
		var newEntity = mapper.treeToValue(body.get("newEntity"), Product.class);
		var oldEntity = mapper.treeToValue(body.get("oldEntity"), Product.class);
		boolean success = repo().update(newEntity, oldEntity);
		json(ctx, Map.of("success", success));
	}

	private void upsert(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var product = mapper.readValue(ctx.body(), Product.class);
		boolean success = repo().insertOrUpdate(product);
		json(ctx, Map.of("success", success, "id", product.id != null ? product.id : -1));
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
		var projection = ApiObjectMapper.parseProjection(body, Product.class);
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
		var projection = ApiObjectMapper.parseProjection(body, Product.class);
		var result = repo().fetchById(id, projection != null ? projection : fullProjection());
		if (result == null) {
			ctx.status(404).json(Map.of("error", "Not found"));
			return;
		}
		json(ctx, result);
	}

	private void fetchImage(Context ctx) {
		Long id;
		try {
			id = Long.parseLong(ctx.pathParam("id"));
		} catch (NumberFormatException e) {
		    LOG.debug(e.getMessage());
			ctx.status(400).json(Map.of("error", "Invalid product ID"));
			return;
		}

		byte[] imageBytes;
		try {
			imageBytes = repo().fetchImage(id);
		} catch (Exception e) {
			LOG.error("Fetching product image", e);
			ctx.status(500).json(Map.of("error", "Failed to fetch image"));
			return;
		}

		if (imageBytes == null) {
			ctx.status(204);
			return;
		}
		ctx.contentType("image/png");
		ctx.result(imageBytes);
	}

	private void updateImage(Context ctx) {
		Long id;
		try {
			id = Long.parseLong(ctx.pathParam("id"));
		} catch (NumberFormatException e) {
			LOG.debug(e.getMessage());
			ctx.status(400).json(Map.of("error", "Invalid product ID"));
			return;
		}

		try {
			byte[] imageBytes = ctx.bodyAsBytes();
			boolean success = repo().updateImage(id, imageBytes);
			ctx.json(Map.of("success", success));
		} catch (Exception e) {
			LOG.error("Updating product image", e);
			ctx.status(500).json(Map.of("error", "Failed to update image"));
		}
	}

	private static ProductCriteria parseCriteria(JsonNode body) {
		var criteria = new ProductCriteria();
		if (hasValue(body, "productId"))
			criteria.withProductId(body.get("productId").asLong());
		if (hasValue(body, "offset"))
			criteria.withOffset(body.get("offset").asInt());
		if (hasValue(body, "limit"))
			criteria.withLimit(body.get("limit").asInt());
		if (hasValue(body, "orderBy"))
			criteria.withOrderBy(ProductCriteria.OrderBy.valueOf(body.get("orderBy").asText()));
		return criteria;
	}

	private static boolean hasValue(JsonNode node, String field) {
		return node.has(field) && !node.get(field).isNull();
	}

	private static void json(Context ctx, Object obj) throws JsonProcessingException {
		ctx.contentType("application/json");
		ctx.result(ApiObjectMapper.get().writeValueAsString(obj));
	}
}
