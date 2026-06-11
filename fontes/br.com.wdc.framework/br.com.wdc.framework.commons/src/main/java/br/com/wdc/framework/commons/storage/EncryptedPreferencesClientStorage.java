package br.com.wdc.framework.commons.storage;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import br.com.wdc.framework.commons.log.Log;

/**
 * {@link ClientStorage} backed by {@link Preferences} with AES-256-GCM encryption at rest.
 * <p>
 * All keys are stored with the prefix {@value #KEY_PREFIX} to avoid collisions with
 * the plain-text entries of the sibling {@link PreferencesClientStorage} that owns the
 * same {@link Preferences} node. The AES encryption key itself is also stored under a
 * reserved entry ({@value #KEY_ENTRY}) in the same node; it is generated once per
 * installation and reused across app restarts.
 * <p>
 * Each stored value is encrypted as {@code base64(iv ‖ ciphertext)} where the IV is
 * freshly randomised for every write (GCM with a unique IV per encryption is mandatory
 * for security).
 * <p>
 * <b>Security notes:</b>
 * <ul>
 *   <li>The AES key lives in the same backing store as the encrypted values.  An
 *       attacker with raw read access to the backing store (NSUserDefaults on iOS,
 *       SharedPreferences on Android) could recover plaintext.  However, both
 *       platforms enforce app-sandbox isolation, and iOS also applies Data Protection
 *       (hardware-backed encryption tied to the device passcode) to the sandbox.  For
 *       a client credential store this level of protection is generally sufficient.</li>
 *   <li>If AES/GCM is unavailable in the runtime (rare, but possible in restrictive
 *       environments), operations degrade gracefully: {@code set} stores plaintext,
 *       {@code get} returns {@code null} for any entry that cannot be decrypted.</li>
 * </ul>
 */
public class EncryptedPreferencesClientStorage implements ClientStorage {

    private static final Log LOG = Log.getLogger(EncryptedPreferencesClientStorage.class);

    /** Prefix for all encrypted entries — avoids collisions with plain PreferencesClientStorage. */
    static final String KEY_PREFIX = "_sec.";

    /** Preferences entry that stores the base64-encoded AES key. */
    private static final String KEY_ENTRY = "_sec._k";

    private static final String CIPHER_ALGO  = "AES/GCM/NoPadding";
    private static final int    GCM_TAG_LEN  = 128; // bits
    private static final int    GCM_IV_LEN   = 12;  // bytes
    private static final int    AES_KEY_BITS = 256;

    private final Preferences prefs;

    /** Lazily-loaded and cached AES key. Volatile for safe publication. */
    private volatile SecretKey secretKey;

    /**
     * Package-private — obtain via {@link PreferencesClientStorage#secure()}.
     */
    EncryptedPreferencesClientStorage(Preferences prefs) {
        this.prefs = prefs;
    }

    // ------------------------------------------------------------------
    // ClientStorage contract
    // ------------------------------------------------------------------

    @Override
    public ClientStorage secure() {
        return this; // already the secure view
    }

    @Override
    public String get(String key) {
        var stored = prefs.get(KEY_PREFIX + key, null);
        if (stored == null) {
            return null;
        }
        try {
            return decrypt(stored);
        } catch (Exception e) {
            // Corrupt entry or leftover from old plaintext storage — evict it.
            LOG.warn("EncryptedPreferencesClientStorage: failed to decrypt '{}', removing entry: {}", key, e.getMessage());
            prefs.remove(KEY_PREFIX + key);
            return null;
        }
    }

    @Override
    public void set(String key, String value) {
        if (value == null) {
            prefs.remove(KEY_PREFIX + key);
            return;
        }
        try {
            prefs.put(KEY_PREFIX + key, encrypt(value));
        } catch (Exception e) {
            // Degraded mode: store plaintext so the app does not crash.
            LOG.warn("EncryptedPreferencesClientStorage: failed to encrypt '{}', storing plaintext: {}", key, e.getMessage());
            prefs.put(KEY_PREFIX + key, value);
        }
    }

    @Override
    public void remove(String key) {
        prefs.remove(KEY_PREFIX + key);
    }

    // ------------------------------------------------------------------
    // Key management
    // ------------------------------------------------------------------

    private SecretKey getOrCreateKey() throws Exception {
        if (secretKey != null) {
            return secretKey;
        }
        synchronized (this) {
            if (secretKey != null) {
                return secretKey;
            }
            var stored = prefs.get(KEY_ENTRY, null);
            if (stored == null) {
                var gen = KeyGenerator.getInstance("AES");
                gen.init(AES_KEY_BITS);
                var newKey = gen.generateKey();
                prefs.put(KEY_ENTRY, Base64.getEncoder().encodeToString(newKey.getEncoded()));
                secretKey = newKey;
            } else {
                var keyBytes = Base64.getDecoder().decode(stored);
                secretKey = new SecretKeySpec(keyBytes, "AES");
            }
        }
        return secretKey;
    }

    // ------------------------------------------------------------------
    // Encryption / decryption
    // ------------------------------------------------------------------

    private String encrypt(String plaintext) throws Exception {
        var key = getOrCreateKey();
        var iv = new byte[GCM_IV_LEN];
        new SecureRandom().nextBytes(iv);

        var cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LEN, iv));
        var cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Encode as base64(iv ‖ ciphertext+tag)
        var combined = new byte[GCM_IV_LEN + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LEN);
        System.arraycopy(cipherBytes, 0, combined, GCM_IV_LEN, cipherBytes.length);
        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String b64) throws Exception {
        var key = getOrCreateKey();
        var combined = Base64.getDecoder().decode(b64);
        if (combined.length < GCM_IV_LEN + 1) {
            throw new IllegalArgumentException("Ciphertext too short");
        }
        var iv         = Arrays.copyOfRange(combined, 0, GCM_IV_LEN);
        var cipherBytes = Arrays.copyOfRange(combined, GCM_IV_LEN, combined.length);

        var cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LEN, iv));
        return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
    }
}
