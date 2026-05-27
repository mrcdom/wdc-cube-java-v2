package br.com.wdc.shopping.view.teavm.repo;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.persistence.client.ModelCodec;

public class TeaVMPurchaseItemCodec implements ModelCodec<PurchaseItem, PurchaseItemCriteria> {

    @Override
    public JsonObject entityToJson(PurchaseItem entity) {
        var json = JsonModelParser.purchaseItemToJson(entity);
        if (entity.purchase != null && entity.purchase.id != null) {
            json.addProperty("purchaseId", entity.purchase.id);
        }
        return json;
    }

    @Override
    public PurchaseItem entityFromJson(JsonObject json) {
        return JsonModelParser.parsePurchaseItem(json, null);
    }

    @Override
    public List<PurchaseItem> entityListFromJson(JsonArray array) {
        return JsonModelParser.parsePurchaseItemList(array, null);
    }

    @Override
    public JsonObject criteriaToJson(PurchaseItemCriteria criteria) {
        var body = new JsonObject();
        if (criteria.purchaseItemId() != null) body.addProperty("purchaseItemId", criteria.purchaseItemId());
        if (criteria.purchaseId() != null) body.addProperty("purchaseId", criteria.purchaseId());
        if (criteria.productId() != null) body.addProperty("productId", criteria.productId());
        if (criteria.userId() != null) body.addProperty("userId", criteria.userId());
        if (criteria.orderBy() != null) body.addProperty("orderBy", criteria.orderBy().name());
        return body;
    }

    @Override
    public PurchaseItem getProjection(PurchaseItemCriteria criteria) {
        return criteria.projection();
    }

    @Override
    public void addProjection(JsonObject body, PurchaseItem projection) {
        if (projection != null) {
            body.add("projection", JsonModelParser.purchaseItemToJson(projection));
        }
    }

    @Override
    public void setGeneratedId(PurchaseItem entity, long id) {
        entity.id = id;
    }
}
