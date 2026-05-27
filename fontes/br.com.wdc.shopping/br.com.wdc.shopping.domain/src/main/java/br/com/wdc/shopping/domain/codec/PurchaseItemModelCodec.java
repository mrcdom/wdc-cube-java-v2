package br.com.wdc.shopping.domain.codec;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;

public class PurchaseItemModelCodec implements ModelCodec<PurchaseItem, PurchaseItemCriteria> {

	private static final ProductModelCodec PRODUCT_CODEC = new ProductModelCodec();

	@Override
	public void writeEntity(ExtensibleObjectOutput out, PurchaseItem entity) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (entity.amount != null) out.name("amount").value(entity.amount.longValue());
		if (entity.price != null) out.name("price").value(entity.price);
		if (entity.product != null) {
			out.name("product");
			PRODUCT_CODEC.writeEntity(out, entity.product);
		}
		if (entity.purchase != null && entity.purchase.id != null) {
			out.name("purchaseId").value(entity.purchase.id);
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
	public void writeCriteriaFields(ExtensibleObjectOutput out, PurchaseItemCriteria criteria) {
		if (criteria.purchaseItemId() != null) out.name("purchaseItemId").value(criteria.purchaseItemId());
		if (criteria.purchaseId() != null) out.name("purchaseId").value(criteria.purchaseId());
		if (criteria.productId() != null) out.name("productId").value(criteria.productId());
		if (criteria.userId() != null) out.name("userId").value(criteria.userId());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
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
