package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.persistence.client.ModelCodec;

public class TeaVMProductCodec implements ModelCodec<Product, ProductCriteria> {

    @Override
    public JsonObject entityToJson(Product entity) {
        return JsonModelParser.productToJson(entity);
    }

    @Override
    public Product entityFromJson(JsonObject json) {
        return JsonModelParser.parseProduct(json);
    }

    @Override
    public List<Product> entityListFromJson(JsonArray array) {
        return JsonModelParser.parseProductList(array);
    }

    @Override
    public JsonObject criteriaToJson(ProductCriteria criteria) {
        var body = new JsonObject();
        if (criteria.productId() != null) body.addProperty("productId", criteria.productId());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    @Override
    public Product getProjection(ProductCriteria criteria) {
        return criteria.projection();
    }

    @Override
    public void addProjection(JsonObject body, Product projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.productToJson(projection));
        }
    }

    @Override
    public void setGeneratedId(Product entity, long id) {
        entity.id = id;
    }
}
