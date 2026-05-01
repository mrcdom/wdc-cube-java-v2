package br.com.wdc.shopping.api.client;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;

public class RestPurchaseItemRepository implements PurchaseItemRepository {

    private static final Type ITEM_LIST_TYPE = new TypeToken<List<PurchaseItem>>() {}.getType();

    private final RestConfig config;

    public RestPurchaseItemRepository(RestConfig config) {
        this.config = config;
    }

    @Override
    public boolean insert(PurchaseItem item) {
        var body = config.gson().toJsonTree(item).getAsJsonObject();
        addPurchaseId(body, item);
        var result = config.postJson("/api/repo/purchase-item/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            item.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(PurchaseItem newItem, PurchaseItem oldItem) {
        var body = new JsonObject();
        var newJson = config.gson().toJsonTree(newItem).getAsJsonObject();
        addPurchaseId(newJson, newItem);
        var oldJson = config.gson().toJsonTree(oldItem).getAsJsonObject();
        addPurchaseId(oldJson, oldItem);
        body.add("newEntity", newJson);
        body.add("oldEntity", oldJson);
        return config.postJson("/api/repo/purchase-item/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(PurchaseItem item) {
        var body = config.gson().toJsonTree(item).getAsJsonObject();
        addPurchaseId(body, item);
        var result = config.postJson("/api/repo/purchase-item/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            item.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(PurchaseItemCriteria criteria) {
        return config.postJson("/api/repo/purchase-item/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(PurchaseItemCriteria criteria) {
        return config.postJson("/api/repo/purchase-item/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<PurchaseItem> fetch(PurchaseItemCriteria criteria) {
        var body = buildCriteria(criteria);
        config.addProjection(body, criteria.projection());
        var result = config.postJson("/api/repo/purchase-item/fetch", body);
        return config.gson().fromJson(result.getAsJsonArray("items"), ITEM_LIST_TYPE);
    }

    @Override
    public PurchaseItem fetchById(Long purchaseItemId, PurchaseItem projection) {
        var body = new JsonObject();
        body.addProperty("id", purchaseItemId);
        config.addProjection(body, projection);
        var result = config.postJsonNullable("/api/repo/purchase-item/fetchById", body);
        if (result == null) return null;
        return config.gson().fromJson(result, PurchaseItem.class);
    }

    private JsonObject buildCriteria(PurchaseItemCriteria criteria) {
        var body = new JsonObject();
        if (criteria.purchaseItemId() != null) body.addProperty("purchaseItemId", criteria.purchaseItemId());
        if (criteria.purchaseId() != null) body.addProperty("purchaseId", criteria.purchaseId());
        if (criteria.productId() != null) body.addProperty("productId", criteria.productId());
        if (criteria.userId() != null) body.addProperty("userId", criteria.userId());
        if (criteria.offset() != null) body.addProperty("offset", criteria.offset());
        if (criteria.limit() != null) body.addProperty("limit", criteria.limit());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    private static void addPurchaseId(JsonObject body, PurchaseItem item) {
        if (item.purchase != null && item.purchase.id != null) {
            body.addProperty("purchaseId", item.purchase.id);
        }
    }
}
