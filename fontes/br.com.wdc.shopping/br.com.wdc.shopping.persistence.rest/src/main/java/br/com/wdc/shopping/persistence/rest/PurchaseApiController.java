package br.com.wdc.shopping.persistence.rest;

import java.util.Collections;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseModelCodec;
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
		configure(config, "");
	}

	static void configure(JavalinConfig config, String prefix) {
		var ctrl = new PurchaseApiController();
		config.routes.post(prefix + "/api/repo/purchase/insert", ctrl::insert);
		config.routes.post(prefix + "/api/repo/purchase/update", ctrl::update);
		config.routes.post(prefix + "/api/repo/purchase/delete", ctrl::delete);
		config.routes.post(prefix + "/api/repo/purchase/count", ctrl::count);
		config.routes.post(prefix + "/api/repo/purchase/fetch", ctrl::fetch);
		config.routes.post(prefix + "/api/repo/purchase/fetch-page", ctrl::fetchPage);
		config.routes.post(prefix + "/api/repo/purchase/fetch-by-id", ctrl::fetchByIdPost);
		config.routes.get(prefix + "/api/repo/purchase/{id}", ctrl::fetchById);
	}

	private static PurchaseRepository repo() {
		return PurchaseRepository.BEAN.get();
	}

	private final PurchaseModelCodec codec = new PurchaseModelCodec();

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

	private void insert(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		var purchase = codec.readEntity(reader);
		boolean success = repo().insert(purchase);
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("success").value(success);
		writer.name("id").value(purchase.id != null ? purchase.id : -1);
		writer.endObject();
		json(ctx, writer);
	}

	private void update(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		ModelCodec.UpdateData<Purchase> newData = null;
		Purchase oldEntity = null;
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
		var criteria = new PurchaseCriteria();
		int offset = 0;
		int limit = 0;
		boolean includeItems = true;
		reader.beginObject();
		while (reader.hasNext()) {
			var name = reader.nextName();
			switch (name) {
				case "projection" -> criteria.withProjection(codec.readEntity(reader));
				case "offset" -> offset = InputCoerceUtils.asInteger(reader, 0);
				case "limit" -> limit = InputCoerceUtils.asInteger(reader, 0);
				case "includeItems" -> includeItems = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
				default -> { if (!codec.readCriteriaField(reader, name, criteria)) reader.skipValue(); }
			}
		}
		reader.endObject();
		if (criteria.projection() == null) {
			criteria.withProjection(includeItems ? fullProjectionWithItems() : simpleProjection());
		}
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
		var reader = new JsonStreamReader(ctx.body());
		var criteria = new PurchaseCriteria();
		int pageIx = 0;
		int pageSz = 0;
		boolean includeItems = true;
		reader.beginObject();
		while (reader.hasNext()) {
			var name = reader.nextName();
			switch (name) {
				case "projection" -> criteria.withProjection(codec.readEntity(reader));
				case "page" -> pageIx = InputCoerceUtils.asInteger(reader, 0);
				case "pageSize" -> pageSz = InputCoerceUtils.asInteger(reader, 0);
				case "includeItems" -> includeItems = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
				default -> { if (!codec.readCriteriaField(reader, name, criteria)) reader.skipValue(); }
			}
		}
		reader.endObject();
		if (criteria.projection() == null) {
			criteria.withProjection(includeItems ? fullProjectionWithItems() : simpleProjection());
		}
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
		Long id = Long.parseLong(ctx.pathParam("id"));
		var result = repo().fetchById(id, fullProjectionWithItems());
		if (result == null) {
			ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
			return;
		}
		var writer = new JsonStreamWriter();
		codec.writeEntity(writer, result);
		json(ctx, writer);
	}

	private void fetchByIdPost(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		Long id = null;
		Purchase projection = null;
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "id" -> id = InputCoerceUtils.asLong(reader);
				case "projection" -> projection = codec.readEntity(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();
		var result = repo().fetchById(id, projection != null ? projection : fullProjectionWithItems());
		if (result == null) {
			ctx.status(404).contentType("application/json").result("{\"error\":\"Not found\"}");
			return;
		}
		var writer = new JsonStreamWriter();
		codec.writeEntity(writer, result);
		json(ctx, writer);
	}

	// :: Helpers

	private PurchaseCriteria readCriteria(JsonStreamReader reader) {
		var criteria = new PurchaseCriteria();
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
