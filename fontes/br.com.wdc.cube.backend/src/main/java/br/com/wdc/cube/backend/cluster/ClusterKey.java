package br.com.wdc.cube.backend.cluster;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The key the instances of the mesh prove to each other ({@link ClusterMesh}), derived from the JWT secret: the
 * instances of one configuration share it, and nothing else does. HMAC-SHA256 of a label of its own under the secret,
 * so this key reveals nothing about the secret, and no other key derived from it is this one.
 */
public final class ClusterKey {

    private static final String LABEL = "wdc-store/cluster-mesh/v1";

    private ClusterKey() {
    }

    /** 32 bytes, the same for the same {@code jwtSecret}. */
    public static byte[] from(String jwtSecret) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(LABEL.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new AssertionError("HMAC-SHA256 not available", e);
        }
    }
}
