package br.com.wdc.shopping.persistence.client;

import br.com.wdc.shopping.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;

public class HttpPurchaseItemRepository extends HttpRepository<PurchaseItem, PurchaseItemCriteria, Long>
        implements PurchaseItemRepository {

    public HttpPurchaseItemRepository(HttpTransport transport, ModelCodec<PurchaseItem, PurchaseItemCriteria> codec) {
        super(transport, codec, "/api/repo/purchase-item");
    }
}
