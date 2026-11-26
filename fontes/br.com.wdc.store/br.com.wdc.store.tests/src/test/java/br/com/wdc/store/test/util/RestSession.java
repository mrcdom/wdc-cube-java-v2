package br.com.wdc.store.test.util;

import br.com.wdc.store.domain.product.ProductCodec;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.PurchaseCodec;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.domain.user.UserCodec;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.client.HttpProductRepository;
import br.com.wdc.store.persistence.client.HttpPurchaseItemRepository;
import br.com.wdc.store.persistence.client.HttpPurchaseRepository;
import br.com.wdc.store.persistence.client.HttpUserRepository;
import br.com.wdc.store.persistence.client.OkHttpTransport;
import br.com.wdc.store.persistence.client.RestAuthClient;

/**
 * A signed-in client of the REST API, for tests with security on ({@link TestEnvironment#withSecurity()}): its own
 * transport, carrying its token, and the HTTP repositories over it.
 */
public record RestSession(OkHttpTransport transport, RestAuthClient auth, UserRepository users,
        ProductRepository products, PurchaseRepository purchases, PurchaseItemRepository items) {

    /** Logs in through the REST API — the seed's passwords are the logins. */
    public static RestSession signIn(TestEnvironment env, String userName) {
        return signIn(env, userName, userName);
    }

    /** Logs in through the REST API with SCRAM-SHA-256: the password does not leave the client. */
    public static RestSession signIn(TestEnvironment env, String userName, String password) {
        var transport = new OkHttpTransport(env.baseUrl());
        var auth = new RestAuthClient(transport);
        auth.login(userName, password);
        transport.setAccessTokenSupplier(auth::accessToken);
        return new RestSession(transport, auth,
                new HttpUserRepository(transport, new UserCodec()),
                new HttpProductRepository(transport, new ProductCodec()),
                new HttpPurchaseRepository(transport, new PurchaseCodec()),
                new HttpPurchaseItemRepository(transport, new PurchaseItemCodec()));
    }
}
