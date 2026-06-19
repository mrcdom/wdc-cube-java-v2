package br.com.wdc.shopping.domain.codec;

import br.com.wdc.framework.domain.codec.ModelCodec;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public class ProductModelCodec implements ModelCodec<Product, ProductCriteria> {

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Product entity) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (entity.name != null) out.name("name").value(entity.name);
		if (entity.price != null) out.name("price").value(entity.price);
		if (entity.description != null) out.name("description").value(entity.description);
		out.endObject();
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Product entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			out.beginObject();
			if (entity.id != null) out.name("id").value(entity.id);
			out.endObject();
			return;
		}
		writeEntity(out, entity);
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, Product entity, Product projection) {
		out.beginObject();
		if (entity.id != null) out.name("id").value(entity.id);
		if (projection.name != null) {
			out.name("name");
			if (entity.name != null) out.value(entity.name); else out.nullValue();
		}
		if (projection.price != null) {
			out.name("price");
			if (entity.price != null) out.value(entity.price); else out.nullValue();
		}
		if (projection.description != null) {
			out.name("description");
			if (entity.description != null) out.value(entity.description); else out.nullValue();
		}
		out.endObject();
	}

	@Override
	public Product computeProjection(Product newEntity, Product oldEntity) {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product();
		if (!java.util.Objects.equals(newEntity.name, oldEntity.name)) projection.name = pv.str;
		if (!java.util.Objects.equals(newEntity.price, oldEntity.price)) projection.price = pv.f64;
		if (!java.util.Objects.equals(newEntity.description, oldEntity.description)) projection.description = pv.str;
		return projection;
	}

	@Override
	public Product readEntity(ExtensibleObjectInput in) {
		var product = new Product();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> product.id = InputCoerceUtils.asLong(in);
				case "name" -> product.name = InputCoerceUtils.asString(in);
				case "price" -> product.price = InputCoerceUtils.asDouble(in);
				case "description" -> product.description = InputCoerceUtils.asString(in);
				default -> in.skipValue();
			}
		}
		in.endObject();
		return product;
	}

	@Override
	public UpdateData<Product> readEntityForUpdate(ExtensibleObjectInput in) {
		var pv = ProjectionValues.INSTANCE;
		var entity = new Product();
		var projection = new Product();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> { entity.id = InputCoerceUtils.asLong(in); projection.id = pv.i64; }
				case "name" -> { entity.name = InputCoerceUtils.asString(in); projection.name = pv.str; }
				case "price" -> { entity.price = InputCoerceUtils.asDouble(in); projection.price = pv.f64; }
				case "description" -> { entity.description = InputCoerceUtils.asString(in); projection.description = pv.str; }
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, ProductCriteria criteria) {
		if (criteria.productId() != null) out.name("productId").value(criteria.productId());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, ProductCriteria criteria) {
		switch (fieldName) {
			case "productId" -> criteria.withProductId(InputCoerceUtils.asLong(in));
			case "orderBy" -> {
				var v = InputCoerceUtils.asString(in);
				if (v != null) criteria.withOrderBy(ProductCriteria.OrderBy.valueOf(v));
			}
			default -> { return false; }
		}
		return true;
	}

	@Override
	public Product getProjection(ProductCriteria criteria) {
		return criteria.projection();
	}

	@Override
	public void setGeneratedId(Product entity, long id) {
		entity.id = id;
	}
}
