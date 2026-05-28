package br.com.wdc.shopping.persistence.rest;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseItemModelCodec;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
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

	private final PurchaseItemModelCodec codec = new PurchaseItemModelCodec();

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

	private void insert(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		var item = codec.readEntity(reader);
		boolean success = repo().insert(item);
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("success").value(success);
		writer.name("id").value(item.id != null ? item.id : -1);
		writer.endObject();
		json(ctx, writer);
	}

	private void update(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		ModelCodec.UpdateData<PurchaseItem> newData = null;
		PurchaseItem oldEntity = null;
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "newEntity" -> newData = codec.readEntityForUpdate(reader);
				case "oldEntity" -> oldEntity = codec.readEntity(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();
		if (newData == null) {
			throw new IllegalArgumentException("Missing 'newEntity' in request body");
		}
		boolean success = repo().update(newData.entity(), oldEntity, newData.projection());
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("success").value(success);
		writer.endObject();
		json(ctx, writer);
	}

	private void delete(Context ctx) {
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
		var reader = new JsonStreamReader(ctx.body());
		var criteria = new PurchaseItemCriteria();
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
			item.purchase = null;
			codec.writeEntity(writer, item);
		}
		writer.endArray();
		writer.endObject();
		json(ctx, writer);
	}

	private void fetchPage(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		var criteria = new PurchaseItemCriteria();
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
			item.purchase = null;
			codec.writeEntity(writer, item);
		}
		writer.endArray();
		writer.name("totalItems").value(page.totalItems());
		writer.endObject();
		json(ctx, writer);
	}

	private void fetchById(Context ctx) {
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, fullProjection());
		if (result == null) {
			ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
			return;
		}
		result.purchase = null;
		var writer = new JsonStreamWriter();
		codec.writeEntity(writer, result);
		json(ctx, writer);
	}

	private void fetchByIdPost(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		Long id = null;
		PurchaseItem projection = null;
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
		result.purchase = null;
		var writer = new JsonStreamWriter();
		codec.writeEntity(writer, result);
		json(ctx, writer);
	}

	// :: Helpers

	private PurchaseItemCriteria readCriteria(JsonStreamReader reader) {
		var criteria = new PurchaseItemCriteria();
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
