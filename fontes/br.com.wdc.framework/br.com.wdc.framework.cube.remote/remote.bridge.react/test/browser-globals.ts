/**
 * The few browser globals the bridge reads, for running it in Node: Web Storage, the document, the location and the
 * history. The WebSocket is installed by each test (FakeWebSocket); Web Crypto is Node's own; there is no IndexedDB,
 * so the encrypted storage works in memory.
 */

class MemoryStorage implements Storage {
  private readonly items = new Map<string, string>()

  get length() {
    return this.items.size
  }

  clear() {
    this.items.clear()
  }

  getItem(key: string) {
    return this.items.get(key) ?? null
  }

  key(index: number) {
    return [...this.items.keys()][index] ?? null
  }

  removeItem(key: string) {
    this.items.delete(key)
  }

  setItem(key: string, value: string) {
    this.items.set(key, String(value))
  }
}

const g = globalThis as Record<string, unknown>

Object.defineProperty(g, "sessionStorage", { value: new MemoryStorage(), configurable: true, writable: true })
Object.defineProperty(g, "localStorage", { value: new MemoryStorage(), configurable: true, writable: true })

const fakeLocation = {
  protocol: "http:",
  host: "localhost:8080",
  pathname: "/store/index.html",
  hash: "",
  reloads: 0,
  reload() {
    fakeLocation.reloads++
  },
}

Object.defineProperty(g, "location", { value: fakeLocation, configurable: true, writable: true })
g.document = {
  cookie: "",
  hidden: false,
  location: fakeLocation,
  addEventListener() {},
  removeEventListener() {},
}
g.history = {
  pushState() {},
  replaceState() {},
}
g.addEventListener ??= () => {}
g.removeEventListener ??= () => {}
