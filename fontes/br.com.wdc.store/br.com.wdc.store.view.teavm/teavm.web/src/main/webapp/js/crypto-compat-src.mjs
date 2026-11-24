/**
 * Crypto fallback for non-secure contexts (HTTP without localhost).
 *
 * Exposes `globalThis.wdcCryptoFallback` with PBKDF2-SHA256 + AES-256-GCM
 * implemented via @noble — same parameters as WebCrypto.java's crypto.subtle paths,
 * so the server deciphers both paths identically.
 *
 * Build command (run once from this directory or from teavm.web root):
 *   npx esbuild src/main/webapp/js/crypto-compat-src.mjs \
 *     --bundle --format=iife --minify \
 *     --outfile=src/main/webapp/js/crypto-compat.js
 *
 * Required packages (resolved by npx --yes, no install needed):
 *   @noble/hashes ^1.7.0
 *   @noble/ciphers ^1.2.0
 */
import { pbkdf2 } from '@noble/hashes/pbkdf2'
import { sha256 } from '@noble/hashes/sha2'
import { gcm } from '@noble/ciphers/aes'

const DEC = new TextDecoder()

globalThis.wdcCryptoFallback = {
  /**
   * PBKDF2-SHA256: derives a 32-byte AES key from the given UTF-8 password bytes.
   * @param {Uint8Array} pwdBytes - password as UTF-8 bytes
   * @param {Uint8Array} salt
   * @param {number} iterations
   * @param {number} keyLen - bytes (32 for AES-256)
   * @returns {Uint8Array}
   */
  pbkdf2(pwdBytes, salt, iterations, keyLen) {
    return pbkdf2(sha256, pwdBytes, salt, { c: iterations, dkLen: keyLen })
  },

  /**
   * AES-256-GCM encrypt. Returns ciphertext + 16-byte auth tag (appended).
   * @param {Uint8Array} key - 32-byte raw key
   * @param {Uint8Array} iv  - 12-byte nonce
   * @param {Uint8Array} plainBytes
   * @returns {Uint8Array}
   */
  aesGcmEncrypt(key, iv, plainBytes) {
    return gcm(key, iv).encrypt(plainBytes)
  },

  /**
   * AES-256-GCM decrypt.
   * @param {Uint8Array} key
   * @param {Uint8Array} iv
   * @param {Uint8Array} cipherBytes - ciphertext + 16-byte auth tag
   * @returns {string} decrypted plaintext
   */
  aesGcmDecrypt(key, iv, cipherBytes) {
    return DEC.decode(gcm(key, iv).decrypt(cipherBytes))
  },
}
