package br.com.wdc.shopping.api.client;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;

public class RestProductRepository implements ProductRepository {

    private static final Type PRODUCT_LIST_TYPE = new TypeToken<List<Product>>() {}.getType();

    private final RestConfig config;

    public RestProductRepository(RestConfig config) {
        this.config = config;
    }

    @Override
    public boolean insert(Product product) {
        var body = config.gson().toJsonTree(product).getAsJsonObject();
        var result = config.postJson("/api/repo/product/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            product.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(Product newProduct, Product oldProduct) {
        var body = new JsonObject();
        body.add("newEntity", config.gson().toJsonTree(newProduct));
        body.add("oldEntity", config.gson().toJsonTree(oldProduct));
        return config.postJson("/api/repo/product/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(Product product) {
        var body = config.gson().toJsonTree(product).getAsJsonObject();
        var result = config.postJson("/api/repo/product/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            product.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(ProductCriteria criteria) {
        return config.postJson("/api/repo/product/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(ProductCriteria criteria) {
        return config.postJson("/api/repo/product/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<Product> fetch(ProductCriteria criteria) {
        var body = buildCriteria(criteria);
        config.addProjection(body, criteria.projection());
        var result = config.postJson("/api/repo/product/fetch", body);
        return config.gson().fromJson(result.getAsJsonArray("items"), PRODUCT_LIST_TYPE);
    }

    @Override
    public Product fetchById(Long productId, Product projection) {
        var body = new JsonObject();
        body.addProperty("id", productId);
        config.addProjection(body, projection);
        var result = config.postJsonNullable("/api/repo/product/fetchById", body);
        if (result == null) return null;
        return config.gson().fromJson(result, Product.class);
    }

    @Override
    public byte[] fetchImage(Long productId) {
        return config.getBytes("/api/repo/product/" + productId + "/image");
    }

    @Override
    public boolean updateImage(Long productId, byte[] image) {
        return config.putBytes("/api/repo/product/" + productId + "/image", image);
    }

    private JsonObject buildCriteria(ProductCriteria criteria) {
        var body = new JsonObject();
        if (criteria.productId() != null) body.addProperty("productId", criteria.productId());
        if (criteria.offset() != null) body.addProperty("offset", criteria.offset());
        if (criteria.limit() != null) body.addProperty("limit", criteria.limit());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }
}
