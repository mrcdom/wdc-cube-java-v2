package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.persistence.client.HttpTransport;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;

/**
 * Implementação de {@link PurchaseItemRepository} sem Gson reflection.
 */
public class TeaVMPurchaseItemRepository implements PurchaseItemRepository {

    private final HttpTransport transport;

    public TeaVMPurchaseItemRepository(HttpTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean insert(PurchaseItem item) {
        var body = JsonModelParser.purchaseItemToJson(item);
        var result = transport.postJson("/api/repo/purchase-item/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            item.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(PurchaseItem newItem, PurchaseItem oldItem) {
        var body = new JsonObject();
        body.add("newEntity", JsonModelParser.purchaseItemToJson(newItem));
        body.add("oldEntity", JsonModelParser.purchaseItemToJson(oldItem));
        return transport.postJson("/api/repo/purchase-item/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(PurchaseItem item) {
        var body = JsonModelParser.purchaseItemToJson(item);
        var result = transport.postJson("/api/repo/purchase-item/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            item.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(PurchaseItemCriteria criteria) {
        return transport.postJson("/api/repo/purchase-item/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(PurchaseItemCriteria criteria) {
        return transport.postJson("/api/repo/purchase-item/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<PurchaseItem> fetch(PurchaseItemCriteria criteria) {
        var body = buildCriteria(criteria);
        addProjection(body, criteria.projection());
        var result = transport.postJson("/api/repo/purchase-item/fetch", body);
        return JsonModelParser.parsePurchaseItemList(result.getAsJsonArray("items"), null);
    }

    @Override
    public PurchaseItem fetchById(Long purchaseItemId, PurchaseItem projection) {
        var body = new JsonObject();
        body.addProperty("id", purchaseItemId);
        addProjection(body, projection);
        var result = transport.postJsonNullable("/api/repo/purchase-item/fetchById", body);
        if (result == null) return null;
        return JsonModelParser.parsePurchaseItem(result, null);
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

    private void addProjection(JsonObject body, PurchaseItem projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.purchaseItemToJson(projection));
        }
    }

}
