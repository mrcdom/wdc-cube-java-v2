package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.persistence.client.ModelCodec;

public class TeaVMPurchaseCodec implements ModelCodec<Purchase, PurchaseCriteria> {

    @Override
    public JsonObject entityToJson(Purchase entity) {
        return JsonModelParser.purchaseToJson(entity);
    }

    @Override
    public Purchase entityFromJson(JsonObject json) {
        return JsonModelParser.parsePurchase(json);
    }

    @Override
    public List<Purchase> entityListFromJson(JsonArray array) {
        return JsonModelParser.parsePurchaseList(array);
    }

    @Override
    public JsonObject criteriaToJson(PurchaseCriteria criteria) {
        var body = new JsonObject();
        if (criteria.purchaseId() != null) body.addProperty("purchaseId", criteria.purchaseId());
        if (criteria.userId() != null) body.addProperty("userId", criteria.userId());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    @Override
    public Purchase getProjection(PurchaseCriteria criteria) {
        return criteria.projection();
    }

    @Override
    public void addProjection(JsonObject body, Purchase projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.purchaseToJson(projection));
        }
    }

    @Override
    public void setGeneratedId(Purchase entity, long id) {
        entity.id = id;
    }
}
