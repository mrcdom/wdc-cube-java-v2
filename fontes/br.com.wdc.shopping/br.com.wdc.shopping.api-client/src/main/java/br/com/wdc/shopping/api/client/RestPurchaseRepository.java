package br.com.wdc.shopping.api.client;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;

public class RestPurchaseRepository implements PurchaseRepository {

    private static final Type PURCHASE_LIST_TYPE = new TypeToken<List<Purchase>>() {}.getType();

    private final RestConfig config;

    public RestPurchaseRepository(RestConfig config) {
        this.config = config;
    }

    @Override
    public boolean insert(Purchase purchase) {
        var body = config.gson().toJsonTree(purchase).getAsJsonObject();
        var result = config.postJson("/api/repo/purchase/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            purchase.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(Purchase newPurchase, Purchase oldPurchase) {
        var body = new JsonObject();
        body.add("newEntity", config.gson().toJsonTree(newPurchase));
        body.add("oldEntity", config.gson().toJsonTree(oldPurchase));
        return config.postJson("/api/repo/purchase/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(Purchase purchase) {
        var body = config.gson().toJsonTree(purchase).getAsJsonObject();
        var result = config.postJson("/api/repo/purchase/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            purchase.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(PurchaseCriteria criteria) {
        return config.postJson("/api/repo/purchase/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(PurchaseCriteria criteria) {
        return config.postJson("/api/repo/purchase/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<Purchase> fetch(PurchaseCriteria criteria) {
        var body = buildCriteria(criteria);
        config.addProjection(body, criteria.projection());
        var result = config.postJson("/api/repo/purchase/fetch", body);
        return config.gson().fromJson(result.getAsJsonArray("items"), PURCHASE_LIST_TYPE);
    }

    @Override
    public Purchase fetchById(Long purchaseId, Purchase projection) {
        var body = new JsonObject();
        body.addProperty("id", purchaseId);
        config.addProjection(body, projection);
        var result = config.postJsonNullable("/api/repo/purchase/fetchById", body);
        if (result == null) return null;
        return config.gson().fromJson(result, Purchase.class);
    }

    private JsonObject buildCriteria(PurchaseCriteria criteria) {
        var body = new JsonObject();
        if (criteria.purchaseId() != null) body.addProperty("purchaseId", criteria.purchaseId());
        if (criteria.userId() != null) body.addProperty("userId", criteria.userId());
        if (criteria.offset() != null) body.addProperty("offset", criteria.offset());
        if (criteria.limit() != null) body.addProperty("limit", criteria.limit());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }
}
