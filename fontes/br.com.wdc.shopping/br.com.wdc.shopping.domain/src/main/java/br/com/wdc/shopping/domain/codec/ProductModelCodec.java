package br.com.wdc.shopping.domain.codec;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;

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
	public void writeCriteriaFields(ExtensibleObjectOutput out, ProductCriteria criteria) {
		if (criteria.productId() != null) out.name("productId").value(criteria.productId());
		if (criteria.orderBy() != null) out.name("orderBy").value(criteria.orderBy().name());
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
