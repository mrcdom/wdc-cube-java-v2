package br.com.wdc.shopping.domain.codec;

import static br.com.wdc.shopping.domain.repositories.Repository.changed;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public class PurchaseItemModelCodec implements ModelCodec<PurchaseItem, PurchaseItemCriteria> {

	private static final ProductModelCodec PRODUCT_CODEC = new ProductModelCodec();

	@Override
	public void writeEntity(ExtensibleObjectOutput out, PurchaseItem entity) {
		writeEntity(out, entity, new EntityGraph());
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, PurchaseItem entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			out.beginObject();
			if (entity.id != null) out.name("id").value(entity.id);
			out.endObject();
			return;
		}
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (entity.amount != null) out.name("amount").value(entity.amount.longValue());
		if (entity.price != null) out.name("price").value(entity.price);
		if (entity.product != null) {
			out.name("product");
			PRODUCT_CODEC.writeEntity(out, entity.product, graph);
		}
		if (entity.purchase != null && entity.purchase.id != null) {
			out.name("purchaseId").value(entity.purchase.id);
		}
		out.endObject();
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, PurchaseItem newEntity, PurchaseItem oldEntity, PurchaseItem projection) {
		out.beginObject();
		if (newEntity.id != null) out.name("id").value(newEntity.id);
		if (changed(newEntity, oldEntity, projection, pi -> pi.amount)) {
			out.name("amount");
			if (newEntity.amount != null) out.value(newEntity.amount.longValue()); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, pi -> pi.price)) {
			out.name("price");
			if (newEntity.price != null) out.value(newEntity.price); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, PurchaseItem::productId)) {
			if (newEntity.product != null) {
				out.name("product");
				PRODUCT_CODEC.writeEntity(out, newEntity.product);
			} else {
				out.name("product").nullValue();
			}
		}
		if (changed(newEntity, oldEntity, projection, PurchaseItem::purchaseId)) {
			out.name("purchaseId");
			if (newEntity.purchase != null && newEntity.purchase.id != null) out.value(newEntity.purchase.id); else out.nullValue();
		}
		out.endObject();
	}

	@Override
	public PurchaseItem readEntity(ExtensibleObjectInput in) {
		var item = new PurchaseItem();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> item.id = InputCoerceUtils.asLong(in);
				case "amount" -> item.amount = InputCoerceUtils.asInteger(in);
				case "price" -> item.price = InputCoerceUtils.asDouble(in);
				case "product" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else item.product = PRODUCT_CODEC.readEntity(in);
				}
				case "purchase" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else item.purchase = new PurchaseModelCodec().readEntity(in);
				}
				case "purchaseId" -> {
					var purchaseId = InputCoerceUtils.asLong(in);
					if (purchaseId != null) {
						if (item.purchase == null) item.purchase = new Purchase();
						item.purchase.id = purchaseId;
					}
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return item;
	}

	@Override
	public UpdateData<PurchaseItem> readEntityForUpdate(ExtensibleObjectInput in) {
		var pv = ProjectionValues.INSTANCE;
		var entity = new PurchaseItem();
		var projection = new PurchaseItem();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> { entity.id = InputCoerceUtils.asLong(in); projection.id = pv.i64; }
				case "amount" -> { entity.amount = InputCoerceUtils.asInteger(in); projection.amount = pv.i32; }
				case "price" -> { entity.price = InputCoerceUtils.asDouble(in); projection.price = pv.f64; }
				case "product" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else entity.product = PRODUCT_CODEC.readEntity(in);
					projection.product = new Product();
					projection.product.id = pv.i64;
				}
				case "purchase" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else entity.purchase = new PurchaseModelCodec().readEntity(in);
					projection.purchase = new Purchase();
					projection.purchase.id = pv.i64;
				}
				case "purchaseId" -> {
					var purchaseId = InputCoerceUtils.asLong(in);
					if (purchaseId != null) {
						if (entity.purchase == null) entity.purchase = new Purchase();
						entity.purchase.id = purchaseId;
					}
					projection.purchase = new Purchase();
					projection.purchase.id = pv.i64;
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, PurchaseItemCriteria criteria) {
		if (criteria.purchaseItemId() != null) out.name("purchaseItemId").value(criteria.purchaseItemId());
		if (criteria.purchaseId() != null) out.name("purchaseId").value(criteria.purchaseId());
		if (criteria.productId() != null) out.name("productId").value(criteria.productId());
		if (criteria.userId() != null) out.name("userId").value(criteria.userId());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, PurchaseItemCriteria criteria) {
		switch (fieldName) {
			case "purchaseItemId" -> criteria.withPurchaseItemId(InputCoerceUtils.asLong(in));
			case "purchaseId" -> criteria.withPurchaseId(InputCoerceUtils.asLong(in));
			case "productId" -> criteria.withProductId(InputCoerceUtils.asLong(in));
			case "userId" -> criteria.withUserId(InputCoerceUtils.asLong(in));
			case "orderBy" -> {
				var v = InputCoerceUtils.asString(in);
				if (v != null) criteria.withOrderBy(PurchaseItemCriteria.OrderBy.valueOf(v));
			}
			default -> { return false; }
		}
		return true;
	}

	@Override
	public PurchaseItem getProjection(PurchaseItemCriteria criteria) {
		return criteria.projection();
	}

	@Override
	public void setGeneratedId(PurchaseItem entity, long id) {
		entity.id = id;
	}
}
