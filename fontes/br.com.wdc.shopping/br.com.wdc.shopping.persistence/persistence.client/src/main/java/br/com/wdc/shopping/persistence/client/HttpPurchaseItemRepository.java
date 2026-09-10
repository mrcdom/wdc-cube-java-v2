package br.com.wdc.shopping.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;

public class HttpPurchaseItemRepository extends HttpRepository<PurchaseItem, PurchaseItemCriteria, Long>
        implements PurchaseItemRepository {

    public HttpPurchaseItemRepository(HttpTransport transport, ModelCodec<PurchaseItem, PurchaseItemCriteria> codec) {
        super(transport, codec, "/api/repo/purchase-item");
    }
}
