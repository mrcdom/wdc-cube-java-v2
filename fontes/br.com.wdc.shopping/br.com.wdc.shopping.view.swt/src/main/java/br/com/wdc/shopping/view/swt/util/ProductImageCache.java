package br.com.wdc.shopping.view.swt.util;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import br.com.wdc.shopping.domain.repositories.ProductRepository;

/**
 * Caches product images as SWT Image objects, loading from the repository on demand.
 */
public class ProductImageCache {

    private static final ProductImageCache INSTANCE = new ProductImageCache();

    private final Map<Long, Image> cache = new ConcurrentHashMap<>();

    public static ProductImageCache getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the product image, loading from DB if not cached.
     * Returns null if no image exists for the given product.
     */
    public Image getImage(Display display, long productId) {
        var cached = this.cache.get(productId);
        if (cached != null && !cached.isDisposed()) {
            return cached;
        }

        var repo = ProductRepository.BEAN.get();
        if (repo == null) return null;

        byte[] bytes = repo.fetchImage(productId);
        if (bytes == null || bytes.length == 0) return null;

        var imageData = new ImageData(new ByteArrayInputStream(bytes));
        var image = new Image(display, imageData);
        this.cache.put(productId, image);
        return image;
    }

    /**
     * Disposes all cached images. Call on application shutdown.
     */
    public void dispose() {
        for (var img : this.cache.values()) {
            if (!img.isDisposed()) img.dispose();
        }
        this.cache.clear();
    }

    /**
     * Evicts a single product image from the cache.
     */
    public void evict(long productId) {
        var img = this.cache.remove(productId);
        if (img != null && !img.isDisposed()) img.dispose();
    }
}
