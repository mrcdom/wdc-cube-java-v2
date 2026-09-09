package br.com.wdc.shopping.domain.product;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.framework.domain.criteria.CriterionCodec;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public class ProductCodec implements ModelCodec<Product, ProductCriteria> {

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Product entity) {
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (entity.name() != null) out.name("name").value(entity.name());
		if (entity.price() != null) out.name("price").value(entity.price());
		if (entity.description() != null) out.name("description").value(entity.description());
		out.endObject();
	}

	@Override
	public void writeEntity(ExtensibleObjectOutput out, Product entity, EntityGraph graph) {
		if (!graph.track(entity)) {
			out.beginObject();
			if (entity.id() != null) out.name("id").value(entity.id());
			out.endObject();
			return;
		}
		writeEntity(out, entity);
	}

	@Override
	public void writeEntityProjected(ExtensibleObjectOutput out, Product entity, Product projection) {
		out.beginObject();
		if (entity.id() != null) out.name("id").value(entity.id());
		if (projection.name() != null) {
			out.name("name");
			if (entity.name() != null) out.value(entity.name()); else out.nullValue();
		}
		if (projection.price() != null) {
			out.name("price");
			if (entity.price() != null) out.value(entity.price()); else out.nullValue();
		}
		if (projection.description() != null) {
			out.name("description");
			if (entity.description() != null) out.value(entity.description()); else out.nullValue();
		}
		out.endObject();
	}

	@Override
	public Product computeProjection(Product newEntity, Product oldEntity) {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product();
		if (!java.util.Objects.equals(newEntity.name(), oldEntity.name())) projection.withName(pv.str);
		if (!java.util.Objects.equals(newEntity.price(), oldEntity.price())) projection.withPrice(pv.f64);
		if (!java.util.Objects.equals(newEntity.description(), oldEntity.description())) projection.withDescription(pv.str);
		return projection;
	}

	@Override
	public Product readEntity(ExtensibleObjectInput in) {
		var product = new Product();
		in.beginObject();
		while (in.hasNext()) {
			switch (in.nextName()) {
				case "id" -> product.withId(InputCoerceUtils.asLong(in));
				case "name" -> product.withName(InputCoerceUtils.asString(in));
				case "price" -> product.withPrice(InputCoerceUtils.asDouble(in));
				case "description" -> product.withDescription(InputCoerceUtils.asString(in));
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
				case "id" -> { entity.withId(InputCoerceUtils.asLong(in)); projection.withId(pv.i64); }
				case "name" -> { entity.withName(InputCoerceUtils.asString(in)); projection.withName(pv.str); }
				case "price" -> { entity.withPrice(InputCoerceUtils.asDouble(in)); projection.withPrice(pv.f64); }
				case "description" -> { entity.withDescription(InputCoerceUtils.asString(in)); projection.withDescription(pv.str); }
				default -> in.skipValue();
			}
		}
		in.endObject();
		return new UpdateData<>(entity, projection);
	}

	@Override
	public void writeCriteriaFields(ExtensibleObjectOutput out, ProductCriteria criteria) {
		CriterionCodec.write(out, "productId", criteria.productId(), CriterionCodec.LONG_OUT);
		CriterionCodec.write(out, "name", criteria.name(), CriterionCodec.STRING_OUT);
		CriterionCodec.write(out, "price", criteria.price(), CriterionCodec.DOUBLE_OUT);
		CriterionCodec.write(out, "description", criteria.description(), CriterionCodec.STRING_OUT);
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
	}

	@Override
	public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, ProductCriteria criteria) {
		switch (fieldName) {
			case "productId" -> CriterionCodec.read(in, criteria.productId(), CriterionCodec.LONG_IN);
			case "name" -> CriterionCodec.read(in, criteria.name(), CriterionCodec.STRING_IN);
			case "price" -> CriterionCodec.read(in, criteria.price(), CriterionCodec.DOUBLE_IN);
			case "description" -> CriterionCodec.read(in, criteria.description(), CriterionCodec.STRING_IN);
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
		entity.withId(id);
	}
}
