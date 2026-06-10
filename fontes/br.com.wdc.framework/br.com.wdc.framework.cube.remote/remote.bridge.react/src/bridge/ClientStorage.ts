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

  get secure(): ClientStorage { return this }
  get(key: string): string | null { return this._data.get(key) ?? null }
  set(key: string, value: string): void { this._data.set(key, value) }
  remove(key: string): void { this._data.delete(key) }  
  all(): Record<string, string> { return Object.fromEntries(this._data) }
}

// ---------------------------------------------------------------------------
// sessionStorage-backed (session scope — survives F5, not tab close)
// ---------------------------------------------------------------------------

/**
 * `sessionStorage`-backed implementation.
 *
 * @param skipPrefixes  raw key prefixes (or exact keys) to exclude from `all()`
 */
export class SessionStorageClientStorage implements ClientStorage {
  constructor(
    private readonly skipPrefixes: string[] = ['app_id', 'req_seq'],
  ) {}

  get secure(): ClientStorage { return this }

  get(key: string): string | null {
    return sessionStorage.getItem(key)
  }

  set(key: string, value: string): void {
    sessionStorage.setItem(key, value)
  }

  remove(key: string): void {
    sessionStorage.removeItem(key)
  }

  all(): Record<string, string> {
    const result: Record<string, string> = {}
    for (let i = 0; i < sessionStorage.length; i++) {
      const key = sessionStorage.key(i)
      if (key === null) continue
      if (this.skipPrefixes.some(p => key.startsWith(p))) continue
      const v = sessionStorage.getItem(key)
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
 * @param keyPrefix     raw key prefix in localStorage (`''` for plain, `'sec.'` for secure)
 * @param skipPrefixes  raw key prefixes (or exact keys) to exclude from `all()`
 * @param secureFactory factory that produces the `.secure` view of this storage
 */
export class LocalStorageClientStorage implements ClientStorage {
  private _secure: ClientStorage | null = null

  /**
   * @param shellId      short identifier for the shell (e.g. `'rr'`) — used as
   *                     localStorage namespace prefix
   * @param skipPrefixes raw key prefixes (or exact keys) to exclude from `all()`
   * @param secureFactory factory that produces the `.secure` view of this storage
   */
  constructor(
    private readonly shellId: string = '',
    private readonly skipPrefixes: string[] = ['app_', 'req_seq'],
    private readonly secureFactory: () => ClientStorage = () => new InMemoryClientStorage(),
  ) {}

  private get keyPrefix(): string {
    return this.shellId ? `${this.shellId}:` : ''
  }

  get secure(): ClientStorage {
    return (this._secure ??= this.secureFactory())
  }

  get(key: string): string | null {
    return localStorage.getItem(this.keyPrefix + key)
  }

  set(key: string, value: string): void {
    localStorage.setItem(this.keyPrefix + key, value)
  }

  remove(key: string): void {
    localStorage.removeItem(this.keyPrefix + key)
  }

  all(): Record<string, string> {
    const result: Record<string, string> = {}
    for (let i = 0; i < localStorage.length; i++) {
      const rawKey = localStorage.key(i)
      if (rawKey === null) continue
      if (!rawKey.startsWith(this.keyPrefix)) continue
      if (this.skipPrefixes.some(p => rawKey.startsWith(p))) continue
      const shortKey = rawKey.substring(this.keyPrefix.length)
      // Only sync keys prefixed with '~'
      if (!shortKey.startsWith('~')) continue
      const v = localStorage.getItem(rawKey)
      if (v !== null) result[shortKey] = v
    }
    return result
  }
}
