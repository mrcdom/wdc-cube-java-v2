package br.com.wdc.store.persistence.impl.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.log.Log;

/**
 * The product images this instance already read, so that each one comes from the database once.
 *
 * <p>
 * {@link ProductRepositoryImpl} fills it only on reads outside a transaction, which see committed data, and drops an
 * image after the commit of the transaction that changed it: dropping it earlier would let a read in between, which
 * still sees the old image, put it back. A delete of products drops them all.
 * </p>
 * <p>
 * The images changed here are told to whoever listens ({@link #onChanged}: the mesh between backend instances), and
 * the ones changed on another instance are dropped here with {@link #evict} and {@link #clear}, which tell no one.
 * </p>
 */
public final class ProductImageCache {

    private static final Log LOG = Log.getLogger(ProductImageCache.class);

    private static final int MAX_SIZE = 200;

    private static final ConcurrentHashMap<Long, byte[]> IMAGES = new ConcurrentHashMap<>();

    private static final CopyOnWriteArrayList<Consumer<Long>> LISTENERS = new CopyOnWriteArrayList<>();

    private ProductImageCache() {
        // NOOP
    }

    /**
     * Calls {@code listener} after each image changed here, with the product's id, or {@code null} when every image
     * may have changed.
     */
    public static Registration onChanged(Consumer<Long> listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    /** Drops the image of {@code productId}, changed on another instance. */
    public static void evict(Long productId) {
        IMAGES.remove(productId);
    }

    /** Drops every image. */
    public static void clear() {
        IMAGES.clear();
    }

    static byte[] get(Long productId) {
        return IMAGES.get(productId);
    }

    static void put(Long productId, byte[] image) {
        if (image != null && IMAGES.size() < MAX_SIZE) {
            IMAGES.put(productId, image);
        }
    }

    /** The image of {@code productId} changed here. */
    static void changed(Long productId) {
        IMAGES.remove(productId);
        tell(productId);
    }

    /** Every image may have changed here. */
    static void changedAll() {
        IMAGES.clear();
        tell(null);
    }

    private static void tell(Long productId) {
        for (var listener : LISTENERS) {
            try {
                listener.accept(productId);
            } catch (RuntimeException e) {
                LOG.warn("An image change listener failed", e);
            }
        }
    }
}
