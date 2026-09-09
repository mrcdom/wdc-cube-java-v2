package br.com.wdc.shopping.domain.purchaseitem;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.framework.domain.criteria.CriterionCodec;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.product.ProductModelCodec;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseModelCodec;

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
			if (entity.id() != null) out.name("id").value(entity.id());
			out.endObject();
			return;
		}
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (entity.amount() != null) out.name("amount").value(entity.amount().longValue());
		if (entity.price() != null) out.name("price").value(entity.price());
		if (entity.product() != null) {
			out.name("product");
			PRODUCT_CODEC.writeEntity(out, entity.product(), graph);
		}
		if (entity.purchase() != null && entity.purchase().id() != null) {
			out.name("purchaseId").value(entity.purchase().id());
		}
		out.endObject();
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, PurchaseItem entity, PurchaseItem projection) {
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (projection.amount() != null) {
			out.name("amount");
			if (entity.amount() != null) out.value(entity.amount().longValue()); else out.nullValue();
		}
		if (projection.price() != null) {
			out.name("price");
			if (entity.price() != null) out.value(entity.price()); else out.nullValue();
		}
		if (projection.product() != null) {
			if (entity.product() != null) {
				out.name("product");
				PRODUCT_CODEC.writeEntity(out, entity.product());
			} else {
				out.name("product").nullValue();
			}
		}
		if (projection.purchase() != null) {
			out.name("purchaseId");
			if (entity.purchase() != null && entity.purchase().id() != null) out.value(entity.purchase().id()); else out.nullValue();
		}
		out.endObject();
	}

	@Override
	public PurchaseItem computeProjection(PurchaseItem newEntity, PurchaseItem oldEntity) {
		var pv = ProjectionValues.INSTANCE;
		var projection = new PurchaseItem();
		if (!java.util.Objects.equals(newEntity.amount(), oldEntity.amount())) projection.withAmount(pv.i32);
		if (!java.util.Objects.equals(newEntity.price(), oldEntity.price())) projection.withPrice(pv.f64);
		if (!java.util.Objects.equals(newEntity.productId(), oldEntity.productId())) {
			projection.withProduct(new Product().withId(pv.i64));
		}
		if (!java.util.Objects.equals(newEntity.purchaseId(), oldEntity.purchaseId())) {
			projection.withPurchase(new Purchase().withId(pv.i64));
		}
		return projection;
	}

	@Override
	public PurchaseItem readEntity(ExtensibleObjectInput in) {
		var item = new PurchaseItem();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> item.withId(InputCoerceUtils.asLong(in));
				case "amount" -> item.withAmount(InputCoerceUtils.asInteger(in));
				case "price" -> item.withPrice(InputCoerceUtils.asDouble(in));
				case "product" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else item.withProduct(PRODUCT_CODEC.readEntity(in));
				}
				case "purchase" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else item.withPurchase(new PurchaseModelCodec().readEntity(in));
				}
				case "purchaseId" -> {
					var purchaseId = InputCoerceUtils.asLong(in);
					if (purchaseId != null) {
						if (item.purchase() == null) item.withPurchase(new Purchase().withId(purchaseId));
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
				case "id" -> { entity.withId(InputCoerceUtils.asLong(in)); projection.withId(pv.i64); }
				case "amount" -> { entity.withAmount(InputCoerceUtils.asInteger(in)); projection.withAmount(pv.i32); }
				case "price" -> { entity.withPrice(InputCoerceUtils.asDouble(in)); projection.withPrice(pv.f64); }
				case "product" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else entity.withProduct(PRODUCT_CODEC.readEntity(in));
					projection.withProduct(new Product().withId(pv.i64));
				}
				case "purchase" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else entity.withPurchase(new PurchaseModelCodec().readEntity(in));
					projection.withPurchase(new Purchase().withId(pv.i64));
				}
				case "purchaseId" -> {
					var purchaseId = InputCoerceUtils.asLong(in);
					if (purchaseId != null) {
						if (entity.purchase() == null) entity.withPurchase(new Purchase().withId(purchaseId));
					}
					projection.withPurchase(new Purchase().withId(pv.i64));
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, PurchaseItemCriteria criteria) {
		CriterionCodec.write(out, "purchaseItemId", criteria.purchaseItemId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "purchaseId", criteria.purchaseId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "productId", criteria.productId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "userId", criteria.userId(), CriterionCodec.LONG_OUT);
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, PurchaseItemCriteria criteria) {
		switch (fieldName) {
			case "purchaseItemId" -> CriterionCodec.read(in, criteria.purchaseItemId(), CriterionCodec.LONG_IN);
			case "purchaseId" -> CriterionCodec.read(in, criteria.purchaseId(), CriterionCodec.LONG_IN);
			case "productId" -> CriterionCodec.read(in, criteria.productId(), CriterionCodec.LONG_IN);
			case "userId" -> CriterionCodec.read(in, criteria.userId(), CriterionCodec.LONG_IN);
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
		entity.withId(id);
	}
}
