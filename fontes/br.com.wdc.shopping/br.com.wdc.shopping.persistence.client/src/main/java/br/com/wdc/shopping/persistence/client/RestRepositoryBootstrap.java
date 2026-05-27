package br.com.wdc.shopping.persistence.client;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.CryptoProvider;
import br.com.wdc.shopping.domain.security.JceCryptoProvider;

/**
 * Inicializa os repositórios REST e registra nos BEANs estáticos do domínio.
 * <p>
 * Também registra o {@link AuthenticationService} para que o fluxo de login
 * via HMAC challenge-response funcione corretamente em clientes REST.
 */
public final class RestRepositoryBootstrap {

    private RestRepositoryBootstrap() {}

    public static void initialize(RestConfig config) {
        CryptoProvider.BEAN.set(new JceCryptoProvider());
        UserRepository.BEAN.set(createUserRepository(config));
        ProductRepository.BEAN.set(createProductRepository(config));
        PurchaseRepository.BEAN.set(createPurchaseRepository(config));
        PurchaseItemRepository.BEAN.set(createPurchaseItemRepository(config));
        AuthenticationService.BEAN.set(new RestAuthenticationService(config));
    }

    public static void release() {
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }

    public static UserRepository createUserRepository(RestConfig config) {
        return new HttpUserRepository(config.transport(), new GsonModelCodec<>(
                config.gson(), User.class, new TypeToken<List<User>>() {}.getType(),
                RestRepositoryBootstrap::userCriteriaToJson,
                UserCriteria::projection,
                (e, id) -> e.id = id));
    }

    public static ProductRepository createProductRepository(RestConfig config) {
        return new HttpProductRepository(config.transport(), new GsonModelCodec<>(
                config.gson(), Product.class, new TypeToken<List<Product>>() {}.getType(),
                RestRepositoryBootstrap::productCriteriaToJson,
                ProductCriteria::projection,
                (e, id) -> e.id = id));
    }

    public static PurchaseRepository createPurchaseRepository(RestConfig config) {
        return new HttpPurchaseRepository(config.transport(), new GsonModelCodec<>(
                config.gson(), Purchase.class, new TypeToken<List<Purchase>>() {}.getType(),
                RestRepositoryBootstrap::purchaseCriteriaToJson,
                PurchaseCriteria::projection,
                (e, id) -> e.id = id));
    }

    public static PurchaseItemRepository createPurchaseItemRepository(RestConfig config) {
        var gson = config.gson();
        return new HttpPurchaseItemRepository(config.transport(), new GsonModelCodec<>(
                gson, PurchaseItem.class, new TypeToken<List<PurchaseItem>>() {}.getType(),
                RestRepositoryBootstrap::purchaseItemCriteriaToJson,
                PurchaseItemCriteria::projection,
                (e, id) -> e.id = id,
                item -> {
                    var json = gson.toJsonTree(item).getAsJsonObject();
                    if (item.purchase != null && item.purchase.id != null) {
                        json.addProperty("purchaseId", item.purchase.id);
                    }
                    return json;
                }));
    }

    private static JsonObject userCriteriaToJson(UserCriteria c) {
        var body = new JsonObject();
        if (c.userId() != null) body.addProperty("userId", c.userId());
        if (c.userName() != null) body.addProperty("userName", c.userName());
        if (c.password() != null) body.addProperty("password", c.password());
        if (c.orderBy() != null) body.addProperty("orderBy", c.orderBy().name());
        return body;
    }

    private static JsonObject productCriteriaToJson(ProductCriteria c) {
        var body = new JsonObject();
        if (c.productId() != null) body.addProperty("productId", c.productId());
        if (c.orderBy() != null) body.addProperty("orderBy", c.orderBy().name());
        return body;
    }

    private static JsonObject purchaseCriteriaToJson(PurchaseCriteria c) {
        var body = new JsonObject();
        if (c.purchaseId() != null) body.addProperty("purchaseId", c.purchaseId());
        if (c.userId() != null) body.addProperty("userId", c.userId());
        if (c.orderBy() != null) body.addProperty("orderBy", c.orderBy().name());
        return body;
    }

    private static JsonObject purchaseItemCriteriaToJson(PurchaseItemCriteria c) {
        var body = new JsonObject();
        if (c.purchaseItemId() != null) body.addProperty("purchaseItemId", c.purchaseItemId());
        if (c.purchaseId() != null) body.addProperty("purchaseId", c.purchaseId());
        if (c.productId() != null) body.addProperty("productId", c.productId());
        if (c.userId() != null) body.addProperty("userId", c.userId());
        if (c.orderBy() != null) body.addProperty("orderBy", c.orderBy().name());
        return body;
    }
}
