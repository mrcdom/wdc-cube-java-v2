package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.persistence.client.HttpTransport;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;

/**
 * Implementação de {@link ProductRepository} sem Gson reflection.
 */
public class TeaVMProductRepository implements ProductRepository {

    private final HttpTransport transport;

    public TeaVMProductRepository(HttpTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean insert(Product product) {
        var body = JsonModelParser.productToJson(product);
        var result = transport.postJson("/api/repo/product/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            product.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(Product newProduct, Product oldProduct) {
        var body = new JsonObject();
        body.add("newEntity", JsonModelParser.productToJson(newProduct));
        body.add("oldEntity", JsonModelParser.productToJson(oldProduct));
        return transport.postJson("/api/repo/product/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(Product product) {
        var body = JsonModelParser.productToJson(product);
        var result = transport.postJson("/api/repo/product/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            product.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(ProductCriteria criteria) {
        return transport.postJson("/api/repo/product/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(ProductCriteria criteria) {
        return transport.postJson("/api/repo/product/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<Product> fetch(ProductCriteria criteria) {
        var body = buildCriteria(criteria);
        addProjection(body, criteria.projection());
        var result = transport.postJson("/api/repo/product/fetch", body);
        return JsonModelParser.parseProductList(result.getAsJsonArray("items"));
    }

    @Override
    public Product fetchById(Long productId, Product projection) {
        var body = new JsonObject();
        body.addProperty("id", productId);
        addProjection(body, projection);
        var result = transport.postJsonNullable("/api/repo/product/fetchById", body);
        if (result == null) return null;
        return JsonModelParser.parseProduct(result);
    }

    @Override
    public byte[] fetchImage(Long productId) {
        return transport.getBytes("/api/repo/product/" + productId + "/image");
    }

    @Override
    public boolean updateImage(Long productId, byte[] image) {
        return transport.putBytes("/api/repo/product/" + productId + "/image", image);
    }

    private JsonObject buildCriteria(ProductCriteria criteria) {
        var body = new JsonObject();
        if (criteria.productId() != null) body.addProperty("productId", criteria.productId());
        if (criteria.offset() != null) body.addProperty("offset", criteria.offset());
        if (criteria.limit() != null) body.addProperty("limit", criteria.limit());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    private void addProjection(JsonObject body, Product projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.productToJson(projection));
        }
    }

}
