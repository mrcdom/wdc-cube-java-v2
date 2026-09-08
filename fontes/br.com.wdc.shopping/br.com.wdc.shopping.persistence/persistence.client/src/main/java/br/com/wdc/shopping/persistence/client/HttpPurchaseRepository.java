package br.com.wdc.shopping.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;

public class HttpPurchaseRepository extends HttpRepository<Purchase, PurchaseCriteria, Long> implements PurchaseRepository {

    public HttpPurchaseRepository(HttpTransport transport, ModelCodec<Purchase, PurchaseCriteria> codec) {
        super(transport, codec, "/api/repo/purchase");
    }
}
