package br.com.wdc.shopping.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;

public class HttpProductRepository extends HttpRepository<Product, ProductCriteria, Long> implements ProductRepository {

    public HttpProductRepository(HttpTransport transport, ModelCodec<Product, ProductCriteria> codec) {
        super(transport, codec, "/api/repo/product");
    }

    @Override
    public byte[] fetchImage(Long productId) {
        return transport().getBytes(basePath() + "/" + productId + "/image");
    }

    @Override
    public boolean updateImage(Long productId, byte[] image) {
        return transport().putBytes(basePath() + "/" + productId + "/image", image);
    }
}
