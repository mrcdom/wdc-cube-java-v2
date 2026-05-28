package br.com.wdc.shopping.domain.codec;

import static br.com.wdc.shopping.domain.repositories.Repository.changed;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

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
	public void writeEntityProjected(ExtensibleObjectOutput out, Product newEntity, Product oldEntity, Product projection) {
		out.beginObject();
		if (newEntity.id != null) out.name("id").value(newEntity.id);
		if (changed(newEntity, oldEntity, projection, p -> p.name)) {
			out.name("name");
			if (newEntity.name != null) out.value(newEntity.name); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, p -> p.price)) {
			out.name("price");
			if (newEntity.price != null) out.value(newEntity.price); else out.nullValue();
		}
		if (changed(newEntity, oldEntity, projection, p -> p.description)) {
			out.name("description");
			if (newEntity.description != null) out.value(newEntity.description); else out.nullValue();
		}
		out.endObject();
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
