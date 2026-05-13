package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.persistence.client.HttpTransport;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;

/**
 * Implementação de {@link PurchaseRepository} sem Gson reflection.
 */
public class TeaVMPurchaseRepository implements PurchaseRepository {

    private final HttpTransport transport;

    public TeaVMPurchaseRepository(HttpTransport transport) {
        this.transport = transport;
    }

    @Override
    public boolean insert(Purchase purchase) {
        var body = JsonModelParser.purchaseToJson(purchase);
        var result = transport.postJson("/api/repo/purchase/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            purchase.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public boolean update(Purchase newPurchase, Purchase oldPurchase) {
        var body = new JsonObject();
        body.add("newEntity", JsonModelParser.purchaseToJson(newPurchase));
        body.add("oldEntity", JsonModelParser.purchaseToJson(oldPurchase));
        return transport.postJson("/api/repo/purchase/update", body).get("success").getAsBoolean();
    }

    @Override
    public boolean insertOrUpdate(Purchase purchase) {
        var body = JsonModelParser.purchaseToJson(purchase);
        var result = transport.postJson("/api/repo/purchase/upsert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            purchase.id = result.get("id").getAsLong();
        }
        return success;
    }

    @Override
    public int delete(PurchaseCriteria criteria) {
        return transport.postJson("/api/repo/purchase/delete", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(PurchaseCriteria criteria) {
        return transport.postJson("/api/repo/purchase/count", buildCriteria(criteria)).get("count").getAsInt();
    }

    @Override
    public List<Purchase> fetch(PurchaseCriteria criteria) {
        var body = buildCriteria(criteria);
        addProjection(body, criteria.projection());
        var result = transport.postJson("/api/repo/purchase/fetch", body);
        return JsonModelParser.parsePurchaseList(result.getAsJsonArray("items"));
    }

    @Override
    public Purchase fetchById(Long purchaseId, Purchase projection) {
        var body = new JsonObject();
        body.addProperty("id", purchaseId);
        addProjection(body, projection);
        var result = transport.postJsonNullable("/api/repo/purchase/fetchById", body);
        if (result == null) return null;
        return JsonModelParser.parsePurchase(result);
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

    private void addProjection(JsonObject body, Purchase projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.purchaseToJson(projection));
        }
    }

}
