/**
 * Crypto fallback for non-secure contexts (plain HTTP outside localhost), where `crypto.subtle` does not exist.
 *
 * Exposes `globalThis.wdcCryptoFallback` with the primitives of remote protocol version 1
 * (docs/bridge-skeleton-protocol.md §6): P-256 ECDH, HKDF-SHA256 and AES-256-GCM, implemented with @noble.
 * The bridge's WebCrypto.java uses it with the same parameters as its crypto.subtle path, so the host
 * cannot tell the two apart. `crypto.getRandomValues` exists in every context and is what @noble draws from.
 *
 * Build (from the module root): npm install && npm run build:crypto-compat
 * Output: src/main/webapp/js/crypto-compat.js (committed; the Maven build only copies it).
 */
import { p256 } from '@noble/curves/p256'
import { hkdf } from '@noble/hashes/hkdf'
import { sha256 } from '@noble/hashes/sha2'
import { gcm } from '@noble/ciphers/aes'

const DEC = new TextDecoder()

/** DER prefix of an X.509 SubjectPublicKeyInfo holding an uncompressed P-256 point (id-ecPublicKey, prime256v1). */
const SPKI_P256_PREFIX = new Uint8Array([
  0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01, 0x06, 0x08, 0x2a, 0x86, 0x48,
  0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
])
const SPKI_P256_LENGTH = SPKI_P256_PREFIX.length + 65

function pointToSpki(point) {
  const spki = new Uint8Array(SPKI_P256_LENGTH)
  spki.set(SPKI_P256_PREFIX, 0)
  spki.set(point, SPKI_P256_PREFIX.length)
  return spki
}

function spkiToPoint(spki) {
  if (!(spki instanceof Uint8Array) || spki.length !== SPKI_P256_LENGTH) {
    throw new Error('Not a P-256 SubjectPublicKeyInfo')
  }
  for (let i = 0; i < SPKI_P256_PREFIX.length; i++) {
    if (spki[i] !== SPKI_P256_PREFIX[i]) throw new Error('Not a P-256 SubjectPublicKeyInfo')
  }
  return spki.slice(SPKI_P256_PREFIX.length)
}

globalThis.wdcCryptoFallback = {
  /**
   * A new ephemeral P-256 key pair.
   * @returns {{privateKey: Uint8Array, publicKey: Uint8Array}} the 32-byte private scalar and the public key as
   *   X.509 SubjectPublicKeyInfo (DER, uncompressed point)
   */
  ecdhKeyPair() {
    const privateKey = p256.utils.randomPrivateKey()
    return { privateKey, publicKey: pointToSpki(p256.getPublicKey(privateKey, false)) }
  },

  /**
   * The ECDH shared secret: the 32-byte x-coordinate of own private key × peer public key.
   * @param {Uint8Array} privateKey - 32-byte private scalar
   * @param {Uint8Array} peerSpki - the peer's public key as X.509 SubjectPublicKeyInfo
   * @returns {Uint8Array}
   * @throws if the peer key is not a valid P-256 point
   */
  ecdhSharedSecret(privateKey, peerSpki) {
    return p256.getSharedSecret(privateKey, spkiToPoint(peerSpki), true).slice(1)
  },

  /**
   * HKDF-SHA256 (RFC 5869, extract then expand).
   * @param {Uint8Array} ikm
   * @param {Uint8Array} salt
   * @param {Uint8Array} info
   * @param {number} length - output bytes
   * @returns {Uint8Array}
   */
  hkdfSha256(ikm, salt, info, length) {
    return hkdf(sha256, ikm, salt, info, length)
  },

  /**
   * AES-256-GCM encrypt, 128-bit tag, no additional data.
   * @param {Uint8Array} key - 32-byte raw key
   * @param {Uint8Array} iv - 12-byte nonce
   * @param {Uint8Array} plainBytes
   * @returns {Uint8Array} ciphertext ‖ tag(16)
   */
  aesGcmEncrypt(key, iv, plainBytes) {
    return gcm(key, iv).encrypt(plainBytes)
  },

  /**
   * AES-256-GCM decrypt.
   * @param {Uint8Array} key
   * @param {Uint8Array} iv
   * @param {Uint8Array} cipherBytes - ciphertext ‖ tag(16)
   * @returns {string} the UTF-8 plaintext
   * @throws if the value does not authenticate
   */
  aesGcmDecrypt(key, iv, cipherBytes) {
    return DEC.decode(gcm(key, iv).decrypt(cipherBytes))
  },
}
