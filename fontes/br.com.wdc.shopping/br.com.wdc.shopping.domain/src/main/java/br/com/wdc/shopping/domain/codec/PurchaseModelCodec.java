package br.com.wdc.shopping.domain.codec;

import br.com.wdc.framework.domain.codec.ModelCodec;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public class PurchaseModelCodec implements ModelCodec<Purchase, PurchaseCriteria> {

	private static final UserModelCodec USER_CODEC = new UserModelCodec();

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Purchase entity) {
		writeEntity(out, entity, new EntityGraph());
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Purchase entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			// Entidade já serializada neste grafo — escreve apenas a chave
			out.beginObject();
			if (entity.id != null) out.name("id").value(entity.id);
			out.endObject();
			return;
		}
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (entity.buyDate != null) out.name("buyDate").value(entity.buyDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		if (entity.user != null) {
			out.name("user");
			USER_CODEC.writeEntity(out, entity.user, graph);
		}
		if (entity.items != null) {
			out.name("items").beginArray();
			for (var item : entity.items) {
				writePurchaseItem(out, item, graph);
			}
			out.endArray();
		}
		out.endObject();
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, Purchase entity, Purchase projection) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (projection.buyDate != null) {
			out.name("buyDate");
			if (entity.buyDate != null) out.value(entity.buyDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)); else out.nullValue();
		}
		if (projection.user != null) {
			if (entity.user != null) {
				out.name("user");
				USER_CODEC.writeEntity(out, entity.user);
			} else {
				out.name("user").nullValue();
			}
		}
		out.endObject();
	}

	@Override
	public Purchase computeProjection(Purchase newEntity, Purchase oldEntity) {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Purchase();
		if (!java.util.Objects.equals(newEntity.buyDate, oldEntity.buyDate)) projection.buyDate = pv.offsetDateTime;
		if (!java.util.Objects.equals(newEntity.userId(), oldEntity.userId())) {
			projection.user = new User();
			projection.user.id = pv.i64;
		}
		return projection;
	}

	@Override
	public Purchase readEntity(ExtensibleObjectInput in) {
		var purchase = new Purchase();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> purchase.id = InputCoerceUtils.asLong(in);
				case "buyDate" -> {
					var s = InputCoerceUtils.asString(in);
					if (s != null) purchase.buyDate = OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
				}
				case "user" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else purchase.user = USER_CODEC.readEntity(in);
				}
				case "items" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else purchase.items = readPurchaseItemList(in, purchase);
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return purchase;
	}

	@Override
	public UpdateData<Purchase> readEntityForUpdate(ExtensibleObjectInput in) {
		var pv = ProjectionValues.INSTANCE;
		var entity = new Purchase();
		var projection = new Purchase();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> { entity.id = InputCoerceUtils.asLong(in); projection.id = pv.i64; }
				case "buyDate" -> {
					var s = InputCoerceUtils.asString(in);
					if (s != null) entity.buyDate = OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
					projection.buyDate = pv.offsetDateTime;
				}
				case "user" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else entity.user = USER_CODEC.readEntity(in);
					projection.user = new User();
					projection.user.id = pv.i64;
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, PurchaseCriteria criteria) {
		if (criteria.purchaseId() != null) out.name("purchaseId").value(criteria.purchaseId());
		if (criteria.userId() != null) out.name("userId").value(criteria.userId());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, PurchaseCriteria criteria) {
		switch (fieldName) {
			case "purchaseId" -> criteria.withPurchaseId(InputCoerceUtils.asLong(in));
			case "userId" -> criteria.withUserId(InputCoerceUtils.asLong(in));
			case "orderBy" -> {
				var v = InputCoerceUtils.asString(in);
				if (v != null) criteria.withOrderBy(PurchaseCriteria.OrderBy.valueOf(v));
			}
			default -> { return false; }
		}
		return true;
	}

	@Override
	public Purchase getProjection(PurchaseCriteria criteria) {
		return criteria.projection();
	}

	@Override
	public void setGeneratedId(Purchase entity, long id) {
		entity.id = id;
	}

	// ── PurchaseItem helpers ──

	private static void writePurchaseItem(ExtensibleObjectOutput out, PurchaseItem item, EntityGraph graph) {
		if (!graph.track(item)) {
			// Entidade já serializada — escreve apenas a chave
			out.beginObject();
			if (item.id != null) out.name("id").value(item.id);
			out.endObject();
			return;
		}
		out.beginObject();
		if (item.id != null) out.name("id").value(item.id);
		if (item.amount != null) out.name("amount").value(item.amount.longValue());
		if (item.price != null) out.name("price").value(item.price);
		if (item.product != null) {
			out.name("product");
			new ProductModelCodec().writeEntity(out, item.product, graph);
		}
		out.endObject();
	}

	private static PurchaseItem readPurchaseItem(ExtensibleObjectInput in, Purchase parent) {
		var item = new PurchaseItem();
		// Back-reference como stub (apenas chave) — evita referência cíclica no grafo
		if (parent != null && parent.id != null) {
			var stub = new Purchase();
			stub.id = parent.id;
			item.purchase = stub;
		}
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> item.id = InputCoerceUtils.asLong(in);
				case "amount" -> item.amount = InputCoerceUtils.asInteger(in);
				case "price" -> item.price = InputCoerceUtils.asDouble(in);
				case "product" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else item.product = new ProductModelCodec().readEntity(in);
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return item;
	}

	private static List<PurchaseItem> readPurchaseItemList(ExtensibleObjectInput in, Purchase parent) {
		var list = new ArrayList<PurchaseItem>();
		in.beginArray();
		while (in.hasNext()) {
			list.add(readPurchaseItem(in, parent));
		}
		in.endArray();
		return list;
	}
}
