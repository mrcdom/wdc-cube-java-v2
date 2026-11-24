package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchase.PurchaseRepository;

public class HttpPurchaseRepository extends HttpRepository<Purchase, PurchaseCriteria, Long> implements PurchaseRepository {

    public HttpPurchaseRepository(HttpTransport transport, ModelCodec<Purchase, PurchaseCriteria> codec) {
        super(transport, codec, "/api/repo/purchase");
    }
}
