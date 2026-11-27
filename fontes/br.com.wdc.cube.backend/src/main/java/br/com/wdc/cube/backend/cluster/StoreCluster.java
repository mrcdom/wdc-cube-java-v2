package br.com.wdc.cube.backend.cluster;

import java.util.List;
import java.util.function.Consumer;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.store.persistence.impl.repository.ProductImageCache;
import br.com.wdc.store.persistence.impl.security.SessionEvents;
import io.micrometer.core.instrument.Metrics;

/**
 * What the WDC Store's instances tell each other on the {@link ClusterMesh mesh}:
 * <ul>
 * <li><b>a session changed</b> (renewed, signed out, expired): the others drop their copy ({@link #sessionEvents});</li>
 * <li><b>a product image changed</b> (an id, or {@value #ALL_IMAGES} for every image): the others drop it from their
 * image cache;</li>
 * <li><b>a remote view session was made here</b>: its tab moved to this instance, and the one it was on lets go of its
 * copy, if no socket holds it there.</li>
 * </ul>
 * When another instance connects (it may have published something while it could not), this one drops every session
 * and every image it holds — they are read again from the database. Remote view sessions are left to expire.
 */
public final class StoreCluster {

    public static final String SESSION_CHANGED = "session-changed";
    public static final String IMAGE_CHANGED = "image-changed";
    public static final String REMOTE_SESSION_MADE = "remote-session-made";

    /** The id of an image change that may concern every image. */
    static final String ALL_IMAGES = "*";

    private static final Log LOG = Log.getLogger(StoreCluster.class);

    private final ClusterMesh mesh;

    public StoreCluster(ClusterMesh mesh) {
        this.mesh = mesh;
        // the series exist from the start, at zero, not from the first message
        for (var type : List.of(SESSION_CHANGED, IMAGE_CHANGED, REMOTE_SESSION_MADE)) {
            Metrics.counter("store.cluster.messages.sent", "type", type);
            Metrics.counter("store.cluster.messages.received", "type", type);
        }
        Metrics.counter("store.cluster.connections");
    }

    public ClusterMesh mesh() {
        return mesh;
    }

    /** The sessions that change, told to and by the other instances. */
    public SessionEvents sessionEvents() {
        return new SessionEvents() {

            @Override
            public void sessionChanged(String sessionId) {
                mesh.publish(SESSION_CHANGED, sessionId);
            }

            @Override
            public void listen(Consumer<String> onChanged, Runnable onMissed) {
                mesh.on(SESSION_CHANGED, onChanged);
                mesh.onMissed(onMissed);
            }
        };
    }

    /** Passes on the product images that change here, and drops the ones that change elsewhere. */
    public Registration shareImageChanges() {
        mesh.on(IMAGE_CHANGED, id -> {
            if (ALL_IMAGES.equals(id)) {
                ProductImageCache.clear();
                return;
            }
            try {
                ProductImageCache.evict(Long.valueOf(id));
            } catch (NumberFormatException e) {
                LOG.warn("Image change of an unknown id: {}", id);
            }
        });
        mesh.onMissed(ProductImageCache::clear);
        return ProductImageCache.onChanged(productId -> mesh.publish(IMAGE_CHANGED,
                productId == null ? ALL_IMAGES : productId.toString()));
    }

    /** Tells the remote view sessions made here, and lets go of the ones made elsewhere. */
    public Registration shareRemoteSessions(RemoteApplicationRegistry<?> registry) {
        mesh.on(REMOTE_SESSION_MADE, id -> {
            if (registry.releaseIdle(id)) {
                LOG.debug("Remote session {} moved to another instance: released here", id);
            }
        });
        return registry.onCreated(id -> mesh.publish(REMOTE_SESSION_MADE, id));
    }
}
