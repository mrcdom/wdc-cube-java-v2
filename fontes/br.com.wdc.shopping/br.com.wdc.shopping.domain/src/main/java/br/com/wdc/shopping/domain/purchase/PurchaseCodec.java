package br.com.wdc.shopping.domain.purchase;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.framework.domain.criteria.CriterionCodec;
import br.com.wdc.framework.domain.projection.ProjectionCollectionCodec;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.ProductCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.domain.user.UserCodec;

public class PurchaseCodec implements ModelCodec<Purchase, PurchaseCriteria> {

	private static final UserCodec USER_CODEC = new UserCodec();

	private static final PurchaseItemCodec ITEM_CODEC = new PurchaseItemCodec();

	/**
	 * Escreve a coleção {@code items}: envelope de projeção quando ela carrega critério ou recorte
	 * ({@link ProjectionCollectionCodec}), array simples quando é resultado. O critério embutido é escrito pelo codec
	 * do item, que conhece a estrutura expressiva.
	 */
	private static void writeItems(ExtensibleObjectOutput out, Purchase entity, EntityGraph graph) {
		var items = entity.items();
		if (items instanceof br.com.wdc.framework.commons.util.HasSlice
				|| items instanceof br.com.wdc.framework.commons.util.HasCriteria) {
			ProjectionCollectionCodec.write(out, "items", items,
					(o, item) -> writePurchaseItem(o, item, graph),
					(o, criteria) -> {
						o.beginObject();
						ITEM_CODEC.writeCriteriaFields(o, (PurchaseItemCriteria) criteria);
						o.endObject();
					});
		} else {
			out.name("items").beginArray();
			for (var item : items) {
				writePurchaseItem(out, item, graph);
			}
			out.endArray();
		}
	}

	/** Reidrata o critério embutido da coleção projetada, pelo codec do item. */
	private static PurchaseItemCriteria readItemCriteria(ExtensibleObjectInput in) {
		var criteria = new PurchaseItemCriteria();
		in.beginObject();
		while (in.hasNext()) {
			var name = in.nextName();
			if (!ITEM_CODEC.readCriteriaField(in, name, criteria)) {
				in.skipValue();
			}
		}
		in.endObject();
		return criteria;
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Purchase entity) {
		writeEntity(out, entity, new EntityGraph());
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Purchase entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			// Entidade já serializada neste grafo — escreve apenas a chave
			out.beginObject();
			if (entity.id() != null) out.name("id").value(entity.id());
			out.endObject();
			return;
		}
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (entity.buyDate() != null) out.name("buyDate").value(entity.buyDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		if (entity.user() != null) {
			out.name("user");
			USER_CODEC.writeEntity(out, entity.user(), graph);
		}
		if (entity.items() != null) {
			writeItems(out, entity, graph);
		}
		out.endObject();
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, Purchase entity, Purchase projection) {
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (projection.buyDate() != null) {
			out.name("buyDate");
			if (entity.buyDate() != null) out.value(entity.buyDate().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)); else out.nullValue();
		}
		if (projection.user() != null) {
			if (entity.user() != null) {
				out.name("user");
				USER_CODEC.writeEntity(out, entity.user());
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
		if (!java.util.Objects.equals(newEntity.buyDate(), oldEntity.buyDate())) projection.withBuyDate(pv.offsetDateTime);
		if (!java.util.Objects.equals(newEntity.userId(), oldEntity.userId())) {
			projection.withUser(new User().withId(pv.i64));
		}
		return projection;
	}

	@Override
	public Purchase readEntity(ExtensibleObjectInput in) {
		var purchase = new Purchase();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> purchase.withId(InputCoerceUtils.asLong(in));
				case "buyDate" -> {
					var s = InputCoerceUtils.asString(in);
					if (s != null) purchase.withBuyDate(OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
				}
				case "user" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else purchase.withUser(USER_CODEC.readEntity(in));
				}
				case "items" -> {
					if (in.peek() == SerializationToken.NULL) {
						in.nextNull();
					} else if (ProjectionCollectionCodec.isProjectionEnvelope(in)) {
						// Envelope de projeção: forma + critério + recorte. Distingue-se do array de resultado pelo token.
						purchase.withItems(ProjectionCollectionCodec.read(in,
								i -> readPurchaseItem(i, purchase),
								PurchaseCodec::readItemCriteria));
					} else {
						purchase.withItems(readPurchaseItemList(in, purchase));
					}
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
				case "id" -> { entity.withId(InputCoerceUtils.asLong(in)); projection.withId(pv.i64); }
				case "buyDate" -> {
					var s = InputCoerceUtils.asString(in);
					if (s != null) entity.withBuyDate(OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
					projection.withBuyDate(pv.offsetDateTime);
				}
				case "user" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else entity.withUser(USER_CODEC.readEntity(in));
					projection.withUser(new User().withId(pv.i64));
				}
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, PurchaseCriteria criteria) {
		CriterionCodec.write(out, "purchaseId", criteria.purchaseId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "userId", criteria.userId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "buyDate", criteria.buyDate(), CriterionCodec.ODT_OUT);
		CriterionCodec.write(out, "productId", criteria.productId(), CriterionCodec.LONG_OUT);
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, PurchaseCriteria criteria) {
		switch (fieldName) {
			case "purchaseId" -> CriterionCodec.read(in, criteria.purchaseId(), CriterionCodec.LONG_IN);
			case "userId" -> CriterionCodec.read(in, criteria.userId(), CriterionCodec.LONG_IN);
			case "buyDate" -> CriterionCodec.read(in, criteria.buyDate(), CriterionCodec.ODT_IN);
			case "productId" -> CriterionCodec.read(in, criteria.productId(), CriterionCodec.LONG_IN);
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
		entity.withId(id);
	}

	// ── PurchaseItem helpers ──

	private static void writePurchaseItem(ExtensibleObjectOutput out, PurchaseItem item, EntityGraph graph) {
		if (!graph.track(item)) {
			// Entidade já serializada — escreve apenas a chave
			out.beginObject();
			if (item.id() != null) out.name("id").value(item.id());
			out.endObject();
			return;
		}
		out.beginObject();
		if (item.id() != null) out.name("id").value(item.id());
		if (item.amount() != null) out.name("amount").value(item.amount().longValue());
		if (item.price() != null) out.name("price").value(item.price());
		if (item.product() != null) {
			out.name("product");
			new ProductCodec().writeEntity(out, item.product(), graph);
		}
		out.endObject();
	}

	private static PurchaseItem readPurchaseItem(ExtensibleObjectInput in, Purchase parent) {
		var item = new PurchaseItem();
		// Back-reference como stub (apenas chave) — evita referência cíclica no grafo
		if (parent != null && parent.id() != null) {
			var stub = new Purchase()
					.withId(parent.id());
			item.withPurchase(stub);
		}
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> item.withId(InputCoerceUtils.asLong(in));
				case "amount" -> item.withAmount(InputCoerceUtils.asInteger(in));
				case "price" -> item.withPrice(InputCoerceUtils.asDouble(in));
				case "product" -> {
					if (in.peek() == SerializationToken.NULL) { in.nextNull(); }
					else item.withProduct(new ProductCodec().readEntity(in));
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
