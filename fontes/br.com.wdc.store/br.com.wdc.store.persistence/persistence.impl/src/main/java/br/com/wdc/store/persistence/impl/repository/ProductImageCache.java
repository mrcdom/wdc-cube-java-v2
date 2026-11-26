package br.com.wdc.store.persistence.impl.repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The product images this instance already read, so that each one comes from the database once.
 *
 * <p>
 * {@link ProductRepositoryImpl} fills it only on reads outside a transaction, which see committed data, and drops an
 * image after the commit of the transaction that changed it: dropping it earlier would let a read in between, which
 * still sees the old image, put it back. A delete of products drops them all.
 * </p>
 */
final class ProductImageCache {

    private static final int MAX_SIZE = 200;

    private static final ConcurrentHashMap<Long, byte[]> IMAGES = new ConcurrentHashMap<>();

    private ProductImageCache() {
        // NOOP
    }

    static byte[] get(Long productId) {
        return IMAGES.get(productId);
    }

    static void put(Long productId, byte[] image) {
        if (image != null && IMAGES.size() < MAX_SIZE) {
            IMAGES.put(productId, image);
        }
    }

    static void evict(Long productId) {
        IMAGES.remove(productId);
    }

    static void clear() {
        IMAGES.clear();
    }
}
