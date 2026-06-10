/**
 * AES-GCM encrypted localStorage implementation.
 *
 * The AES-256 key is created with `extractable: false` and persisted in the
 * origin's IndexedDB (`_sec` database, object store `k`). Because the key is
 * non-extractable, its raw bytes are never accessible to JavaScript — not even
 * via `crypto.subtle.exportKey()` — which limits the impact of an XSS attack.
 *
 * Values are stored in `localStorage` under a `sec.` prefix as
 * `base64(iv[12] || ciphertext)`. A fresh random IV is generated per write.
 *
 * ### Lifecycle
 * 1. Call `EncryptedLocalStorage.initialize()` once before the app starts.
 * 2. It opens (or creates) the IndexedDB key, then decrypts all existing
 *    `sec.*` entries into an in-memory cache.
 * 3. After the returned Promise resolves, `get`/`set`/`remove` are synchronous
 *    (cache-backed). Writes persist asynchronously (fire-and-forget).
 *
 * ### Fallback
 * If IndexedDB is unavailable (e.g. some private-browsing modes), the storage
 * works in-memory only — data is not persisted across reloads.
 */

import { ClientStorage } from './ClientStorage'

const LS_PREFIX = 'sec.'
const IDB_NAME = '_sec'
const IDB_STORE = 'k'
const IDB_KEY = 0

export class EncryptedLocalStorage implements ClientStorage {
  private readonly cache = new Map<string, string>()
  private cryptoKey: CryptoKey | null = null

  get secure(): ClientStorage {
    return this
  }

  get(key: string): string | null {
    return this.cache.get(key) ?? null
  }

  set(key: string, value: string): void {
    this.cache.set(key, value)
    if (this.cryptoKey) {
      this.encryptAndStore(key, value, this.cryptoKey).catch(() => {
        // fire-and-forget; failures are non-fatal
      })
    }
  }

  remove(key: string): void {
    this.cache.delete(key)
    localStorage.removeItem(LS_PREFIX + key)
  }

  all(): Record<string, string> {
    return Object.fromEntries(this.cache)
  }

  /**
   * Initialises the AES-GCM key from IndexedDB and pre-decrypts all stored
   * `sec.*` entries into the in-memory cache. Must be awaited before the app
   * starts so that `get()` returns correct values synchronously.
   */
  async initialize(): Promise<void> {
    try {
      this.cryptoKey = await openOrCreateKey()
    } catch {
      // IndexedDB unavailable — in-memory only
      return
    }

    if (!this.cryptoKey) return

    const key = this.cryptoKey
    const len = localStorage.length
    const entries: Array<{ short: string; b64: string }> = []
    for (let i = 0; i < len; i++) {
      const rawKey = localStorage.key(i)
      if (rawKey && rawKey.startsWith(LS_PREFIX)) {
        const b64 = localStorage.getItem(rawKey)
        if (b64) entries.push({ short: rawKey.slice(LS_PREFIX.length), b64 })
      }
    }

    await Promise.all(
      entries.map(async ({ short, b64 }) => {
        const plaintext = await decryptValue(key, b64)
        if (plaintext !== null) {
          this.cache.set(short, plaintext)
        }
        // null means stale key or corrupted data — silently discard
      }),
    )
  }

  private async encryptAndStore(key: string, value: string, cryptoKey: CryptoKey): Promise<void> {
    const b64 = await encryptValue(cryptoKey, value)
    localStorage.setItem(LS_PREFIX + key, b64)
  }
}

// ---------------------------------------------------------------------------
// IndexedDB helpers
// ---------------------------------------------------------------------------

/** Opens (or creates) the `_sec` IndexedDB and returns the stored AES key,
 *  generating and persisting a new one if none exists yet. */
function openOrCreateKey(): Promise<CryptoKey> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_NAME, 1)

    req.onupgradeneeded = (e) => {
      ;(e.target as IDBOpenDBRequest).result.createObjectStore(IDB_STORE)
    }

    req.onerror = () => reject(req.error)

    req.onsuccess = () => {
      const db = req.result
      const tx = db.transaction(IDB_STORE, 'readwrite')
      const store = tx.objectStore(IDB_STORE)
      const get = store.get(IDB_KEY)

      get.onsuccess = () => {
        if (get.result) {
          resolve(get.result as CryptoKey)
          return
        }
        // No key yet — generate one
        crypto.subtle
          .generateKey({ name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt'])
          .then((key) => {
            const tx2 = db.transaction(IDB_STORE, 'readwrite')
            tx2.objectStore(IDB_STORE).put(key, IDB_KEY)
            resolve(key)
          })
          .catch(reject)
      }

      get.onerror = () => reject(get.error)
    }
  })
}

// ---------------------------------------------------------------------------
// Crypto helpers
// ---------------------------------------------------------------------------

/** Encrypts `text` and returns `base64(iv[12] || ciphertext)`. */
async function encryptValue(key: CryptoKey, text: string): Promise<string> {
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const encoded = new TextEncoder().encode(text)
  const ct = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, key, encoded)
  const combined = new Uint8Array(12 + ct.byteLength)
  combined.set(iv, 0)
  combined.set(new Uint8Array(ct), 12)
  let s = ''
  for (let i = 0; i < combined.length; i++) s += String.fromCharCode(combined[i])
  return btoa(s)
}

/** Decrypts a `base64(iv[12] || ciphertext)` value. Returns `null` on failure. */
async function decryptValue(key: CryptoKey, b64: string): Promise<string | null> {
  try {
    const s = atob(b64)
    const b = new Uint8Array(s.length)
    for (let i = 0; i < s.length; i++) b[i] = s.charCodeAt(i)
    const pt = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: b.slice(0, 12) }, key, b.slice(12))
    return new TextDecoder().decode(new Uint8Array(pt))
  } catch {
    return null
  }
}
