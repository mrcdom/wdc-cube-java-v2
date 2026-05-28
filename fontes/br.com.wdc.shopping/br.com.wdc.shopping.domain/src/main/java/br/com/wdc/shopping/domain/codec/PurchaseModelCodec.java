package br.com.wdc.shopping.domain.codec;

import static br.com.wdc.shopping.domain.repositories.Repository.changed;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;

public class PurchaseModelCodec implements ModelCodec<Purchase, PurchaseCriteria> {

	private static final UserModelCodec USER_CODEC = new UserModelCodec();

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Purchase entity) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (entity.buyDate != null) out.name("buyDate").value(entity.buyDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		if (entity.user != null) {
			out.name("user");
			USER_CODEC.writeEntity(out, entity.user);
		}
		if (entity.items != null) {
			out.name("items").beginArray();
			for (var item : entity.items) {
				writePurchaseItem(out, item);
			}
			out.endArray();
		}
		out.endObject();
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, Purchase newEntity, Purchase oldEntity, Purchase projection) {
		out.beginObject();
		if (newEntity.id != null) out.name("id").value(newEntity.id);
		if (changed(newEntity, oldEntity, projection, p -> p.buyDate)) {
			out.name("buyDate");
			if (newEntity.buyDate != null) out.value(newEntity.buyDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, Purchase::userId)) {
			if (newEntity.user != null) {
				out.name("user");
				USER_CODEC.writeEntity(out, newEntity.user);
			} else {
				out.name("user").nullValue();
			}
		}
		out.endObject();
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
	public void writeCriteriaFields(ExtensibleObjectOutput out, PurchaseCriteria criteria) {
		if (criteria.purchaseId() != null) out.name("purchaseId").value(criteria.purchaseId());
		if (criteria.userId() != null) out.name("userId").value(criteria.userId());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
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

	private static void writePurchaseItem(ExtensibleObjectOutput out, PurchaseItem item) {
		out.beginObject();
		if (item.id != null) out.name("id").value(item.id);
		if (item.amount != null) out.name("amount").value(item.amount.longValue());
		if (item.price != null) out.name("price").value(item.price);
		if (item.product != null) {
			out.name("product");
			new ProductModelCodec().writeEntity(out, item.product);
		}
		out.endObject();
	}

	private static PurchaseItem readPurchaseItem(ExtensibleObjectInput in, Purchase parent) {
		var item = new PurchaseItem();
		item.purchase = parent;
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
