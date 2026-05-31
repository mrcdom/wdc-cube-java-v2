package br.com.wdc.shopping.persistence.rest;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.ProductModelCodec;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class ProductApiController {

	private static final Log LOG = Log.getLogger(ProductApiController.class);

	static void configure(JavalinConfig config) {
		configure(config, "");
	}

	static void configure(JavalinConfig config, String prefix) {
		var ctrl = new ProductApiController();
		config.routes.post(prefix + "/api/repo/product/insert", ctrl::insert);
		config.routes.post(prefix + "/api/repo/product/update", ctrl::update);
		config.routes.post(prefix + "/api/repo/product/delete", ctrl::delete);
		config.routes.post(prefix + "/api/repo/product/count", ctrl::count);
		config.routes.post(prefix + "/api/repo/product/fetch", ctrl::fetch);
		config.routes.post(prefix + "/api/repo/product/fetch-page", ctrl::fetchPage);
		config.routes.post(prefix + "/api/repo/product/fetch-by-id", ctrl::fetchByIdPost);
		config.routes.get(prefix + "/api/repo/product/{id}", ctrl::fetchById);
		config.routes.get(prefix + "/api/repo/product/{id}/image", ctrl::fetchImage);
		config.routes.put(prefix + "/api/repo/product/{id}/image", ctrl::updateImage);
	}

	private static ProductRepository repo() {
		return ProductRepository.BEAN.get();
	}

	private final ProductModelCodec codec = new ProductModelCodec();

	private static Product fullProjection() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new Product();
		prj.id = pv.i64;
		prj.name = pv.str;
		prj.price = pv.f64;
		prj.description = pv.str;
		return prj;
	}

	private void insert(Context ctx) {
		SecurityEnforcer.require("product", "write");
		var reader = new JsonStreamReader(ctx.body());
		var product = codec.readEntity(reader);
		boolean success = repo().insert(product);
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("success").value(success);
		writer.name("id").value(product.id != null ? product.id : -1);
		writer.endObject();
		json(ctx, writer);
	}

	private void update(Context ctx) {
		SecurityEnforcer.require("product", "write");
		var reader = new JsonStreamReader(ctx.body());
		var data = codec.readEntityForUpdate(reader);
		boolean success = repo().update(data.entity(), null, data.projection());
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("success").value(success);
		writer.endObject();
		json(ctx, writer);
	}

	private void delete(Context ctx) {
		SecurityEnforcer.require("product", "write");
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
		SecurityEnforcer.require("product", "read");
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
		SecurityEnforcer.require("product", "read");
		var reader = new JsonStreamReader(ctx.body());
		var criteria = new ProductCriteria();
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
		if (criteria.projection() == null) criteria.withProjection(fullProjection());
		var items = repo().fetch(criteria, offset, limit);
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("items").beginArray();
		for (var item : items) {
			codec.writeEntity(writer, item);
		}
		writer.endArray();
		writer.endObject();
		json(ctx, writer);
	}

	private void fetchPage(Context ctx) {
		SecurityEnforcer.require("product", "read");
		var reader = new JsonStreamReader(ctx.body());
		var criteria = new ProductCriteria();
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
		if (criteria.projection() == null) criteria.withProjection(fullProjection());
		var page = repo().fetchPage(criteria, pageIx, pageSz);
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("items").beginArray();
		for (var item : page.items()) {
			codec.writeEntity(writer, item);
		}
		writer.endArray();
		writer.name("totalItems").value(page.totalItems());
		writer.endObject();
		json(ctx, writer);
	}

	private void fetchById(Context ctx) {
		SecurityEnforcer.require("product", "read");
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, fullProjection());
		if (result == null) {
			ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
			return;
		}
		var writer = new JsonStreamWriter();
		codec.writeEntity(writer, result);
		json(ctx, writer);
	}

	private void fetchByIdPost(Context ctx) {
		SecurityEnforcer.require("product", "read");
		var reader = new JsonStreamReader(ctx.body());
		Long id = null;
		Product projection = null;
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "id" -> id = InputCoerceUtils.asLong(reader);
				case "projection" -> projection = codec.readEntity(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();
		var result = repo().fetchById(id, projection != null ? projection : fullProjection());
		if (result == null) {
			ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
			return;
		}
		var writer = new JsonStreamWriter();
		codec.writeEntity(writer, result);
		json(ctx, writer);
	}

	private void fetchImage(Context ctx) {
		Long id;
		try {
			id = Long.parseLong(ctx.pathParam("id"));
		} catch (NumberFormatException e) {
		    LOG.debug(e.getMessage());
			ctx.status(400).contentType("application/json").result("{\"error\":\"Invalid product ID\"}");
			return;
		}

		byte[] imageBytes;
		try {
			imageBytes = repo().fetchImage(id);
		} catch (Exception e) {
			LOG.error("Fetching product image", e);
			ctx.status(500).contentType("application/json").result("{\"error\":\"Failed to fetch image\"}");
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
		SecurityEnforcer.require("product", "write");
		Long id;
		try {
			id = Long.parseLong(ctx.pathParam("id"));
		} catch (NumberFormatException e) {
			LOG.debug(e.getMessage());
			ctx.status(400).contentType("application/json").result("{\"error\":\"Invalid product ID\"}");
			return;
		}

		try {
			byte[] imageBytes = ctx.bodyAsBytes();
			boolean success = repo().updateImage(id, imageBytes);
			ctx.contentType("application/json").result("{\"success\":" + success + "}");
		} catch (Exception e) {
			LOG.error("Updating product image", e);
			ctx.status(500).contentType("application/json").result("{\"error\":\"Failed to update image\"}");
		}
	}

	// :: Helpers

	private ProductCriteria readCriteria(JsonStreamReader reader) {
		var criteria = new ProductCriteria();
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

	private static void json(Context ctx, JsonStreamWriter writer) {
		ctx.contentType("application/json");
		ctx.result(writer.result());
	}
}
