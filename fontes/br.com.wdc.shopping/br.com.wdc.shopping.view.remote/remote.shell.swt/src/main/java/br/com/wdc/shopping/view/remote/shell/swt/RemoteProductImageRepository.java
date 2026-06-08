package br.com.wdc.shopping.view.remote.shell.swt;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;

/**
 * ProductRepository stub that fetches product images from the remote Host over HTTP.
 * All non-image repository operations throw {@link UnsupportedOperationException}.
 */
class RemoteProductImageRepository implements ProductRepository {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteProductImageRepository.class);

    private final String serverUrl;
    private final HttpClient http;

    RemoteProductImageRepository(String serverUrl) {
        this.serverUrl = serverUrl.endsWith("/")
                ? serverUrl.substring(0, serverUrl.length() - 1)
                : serverUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public byte[] fetchImage(Long productId) {
        var uri = URI.create(this.serverUrl + "/image/product/" + productId + ".png");
        try {
            var request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();
            var response = this.http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return response.body();
            }
            LOG.debug("Image not found for product {}: HTTP {}", productId, response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted fetching image for product {}", productId);
        } catch (Exception e) {
            LOG.debug("Failed to fetch image for product {}: {}", productId, e.getMessage());
        }
        return null;
    }

    @Override
    public boolean updateImage(Long productId, byte[] image) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public boolean insert(Product bean) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public boolean update(Product newBean, Product oldBean, Product projection) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public int delete(ProductCriteria criteria) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public int count(ProductCriteria criteria) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public List<Product> fetch(ProductCriteria criteria, int offset, int limit) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public Product fetchById(Long id, Product projection) {
        throw new UnsupportedOperationException("read-only remote repository");
    }

    @Override
    public Product newProjection() {
        throw new UnsupportedOperationException("read-only remote repository");
    }
}
