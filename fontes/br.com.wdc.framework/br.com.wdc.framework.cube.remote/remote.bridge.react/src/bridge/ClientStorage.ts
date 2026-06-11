/**
 * Client-side key-value storage abstraction.
 *
 * Mirrors the Java/Dart `ClientStorage` design: two scopes (session and
 * persistent) with a `.secure` view for sensitive values.
 *
 * All values that travel over WebSocket are ciphered regardless of the scope.
 * The `.secure` view is a hint to use a more secure backing store where
 * available (e.g., Keychain on mobile).
 *
 * In the browser there is no Keychain, so `.secure` uses `localStorage` with
 * a `sec.` key prefix to namespace it away from plain persistent values.
 */

export interface ClientStorage {
  /** Returns a view that uses secure backing (or `this` if already secure). */
  readonly secure: ClientStorage

  get(key: string): string | null
  set(key: string, value: string): void
  remove(key: string): void

  /** All entries in this scope. Used when building the WebSocket bootstrap payload. */
  all(): Record<string, string>
}

// ---------------------------------------------------------------------------
// In-memory fallback (use only outside the browser)
// ---------------------------------------------------------------------------

export class InMemoryClientStorage implements ClientStorage {
  private readonly _data = new Map<string, string>()

  get secure(): ClientStorage {
    return this
  }
  get(key: string): string | null {
    return this._data.get(key) ?? null
  }
  set(key: string, value: string): void {
    this._data.set(key, value)
  }
  remove(key: string): void {
    this._data.delete(key)
  }
  all(): Record<string, string> {
    return Object.fromEntries(this._data)
  }
}

// ---------------------------------------------------------------------------
// sessionStorage-backed (session scope — survives F5, not tab close)
// ---------------------------------------------------------------------------

/**
 * `sessionStorage`-backed implementation.
 *
 * @param syncNamespace key namespace that qualifies entries for WebSocket sync;
 *                      prepended to every stored key and stripped when building `all()`.
 *                      Convention: `'~<shellId>:'` (e.g. `'~rr:'`).
 * @param secureFactory factory that produces the `.secure` view of this storage.
 *                      Defaults to `this` (no encryption — fallback).
 */
export class SessionStorageClientStorage implements ClientStorage {
  private _secure: ClientStorage | null = null

  constructor(
    private readonly syncNamespace: string = "",
    private readonly secureFactory: (() => ClientStorage) | null = null,
  ) {}

  get secure(): ClientStorage {
    if (this.secureFactory) return (this._secure ??= this.secureFactory())
    return this
  }

  get(key: string): string | null {
    return sessionStorage.getItem(this.syncNamespace + key)
  }

  set(key: string, value: string): void {
    sessionStorage.setItem(this.syncNamespace + key, value)
  }

  remove(key: string): void {
    sessionStorage.removeItem(this.syncNamespace + key)
  }

  all(): Record<string, string> {
    const result: Record<string, string> = {}
    for (let i = 0; i < sessionStorage.length; i++) {
      const rawKey = sessionStorage.key(i)
      if (rawKey === null || !rawKey.startsWith(this.syncNamespace)) continue
      const key = rawKey.substring(this.syncNamespace.length)
      const v = sessionStorage.getItem(rawKey)
      if (v !== null) result[key] = v
    }
    return result
  }
}

// ---------------------------------------------------------------------------
// localStorage-backed (persistent scope)
// ---------------------------------------------------------------------------

/**
 * `localStorage`-backed implementation.
 *
 * @param syncNamespace key namespace that qualifies entries for WebSocket sync;
 *                      prepended to every stored key and stripped when building `all()`.
 *                      Convention: `'~<shellId>:'` (e.g. `'~rr:'`) — the `~`
 *                      marks the key as syncable and `<shellId>:` isolates the
 *                      shell's data from other shells on the same origin.
 * @param secureFactory factory that produces the `.secure` view of this storage
 */
export class LocalStorageClientStorage implements ClientStorage {
  private _secure: ClientStorage | null = null

  constructor(
    private readonly syncNamespace: string = "",
    private readonly secureFactory: () => ClientStorage = () => new InMemoryClientStorage(),
  ) {}

  get secure(): ClientStorage {
    return (this._secure ??= this.secureFactory())
  }

  get(key: string): string | null {
    return localStorage.getItem(this.syncNamespace + key)
  }

  set(key: string, value: string): void {
    localStorage.setItem(this.syncNamespace + key, value)
  }

  remove(key: string): void {
    localStorage.removeItem(this.syncNamespace + key)
  }

  all(): Record<string, string> {
    const result: Record<string, string> = {}
    for (let i = 0; i < localStorage.length; i++) {
      const rawKey = localStorage.key(i)
      if (rawKey === null || !rawKey.startsWith(this.syncNamespace)) continue
      const key = rawKey.substring(this.syncNamespace.length)
      const v = localStorage.getItem(rawKey)
      if (v !== null) result[key] = v
    }
    return result
  }
}
