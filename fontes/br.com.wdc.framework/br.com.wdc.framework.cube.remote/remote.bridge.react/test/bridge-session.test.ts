/** The bridge's connection lifecycle around the handshake (protocol version 1), driven through a fake host. */
import { afterEach, beforeEach, expect, test, vi } from "vitest"

import { ScopeCoordinator } from "@/bridge/ScopeCoordinator"
import { FakeWebSocket, HostKeys, hostCipher, hostDecipher, isPing, waitFor } from "./remote-host"

const started: ScopeCoordinator[] = []

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
  FakeWebSocket.instances = []
  vi.stubGlobal("WebSocket", FakeWebSocket)
})

afterEach(() => {
  for (const app of started.splice(0)) {
    app.reconnectController.close()
  }
  for (const socket of FakeWebSocket.instances) {
    socket.hostClose(1000)
  }
  vi.unstubAllGlobals()
})

function newApp() {
  const app = new ScopeCoordinator("~bs:")
  started.push(app)
  return app
}

/** Opens a connection to the host and answers its handshake; returns the connection key. */
async function connect(app: ScopeCoordinator, socket: FakeWebSocket) {
  socket.hostOpen()
  const hello = await waitFor(() => socket.sent[0])
  expect(socket.sent).toHaveLength(1)
  const host = new HostKeys()
  const key = host.deriveKey(hello.clientKey as string, app.id)
  socket.hostSend({ protocol: 1, serverKey: host.serverKey })
  return key
}

const sentAfterHandshake = (socket: FakeWebSocket) => socket.sent.slice(1).filter((message) => !isPing(message))

test("requests made before the handshake answer wait, and go out after it in order", async () => {
  const app = newApp()
  app.onStart() // the route (event -1) is the first request
  const socket = await waitFor(() => FakeWebSocket.latest())

  app.setFormField("aaaaaaaaaaaa:1", "p.x", 1)
  app.submit("aaaaaaaaaaaa:1", 3)
  socket.hostOpen()
  app.setFormField("aaaaaaaaaaaa:1", "p.y", 2)
  app.submit("aaaaaaaaaaaa:1", 4)
  await new Promise((resolve) => setTimeout(resolve, 50))
  expect(socket.sent.map((message) => Object.keys(message).sort())).toEqual([["clientKey", "protocol"]])

  const hello = socket.sent[0]
  const host = new HostKeys()
  host.deriveKey(hello.clientKey as string, app.id)
  socket.hostSend({ protocol: 1, serverKey: host.serverKey })

  const [bootstrap] = await waitFor(() => (sentAfterHandshake(socket).length > 0 ? sentAfterHandshake(socket) : null))
  expect(bootstrap.event).toEqual(["7b32e816a191:0:-1", "aaaaaaaaaaaa:1:3", "aaaaaaaaaaaa:1:4"])
  expect(bootstrap["aaaaaaaaaaaa:1"]).toEqual({ "p.x": 1, "p.y": 2 })
  expect(bootstrap.protocol).toBeUndefined()
})

test("a value ciphered before the handshake waits for the connection key", async () => {
  const app = newApp()
  app.onStart()
  const socket = await waitFor(() => FakeWebSocket.latest())

  let ciphered: string | null = null
  void app.dataSecurity.b64Cipher("s3cret").then((value) => (ciphered = value))
  await new Promise((resolve) => setTimeout(resolve, 20))
  expect(ciphered).toBeNull()

  const key = await connect(app, socket)
  const value = await waitFor(() => ciphered)
  expect(hostDecipher(key, value)).toBe("s3cret")
})

test("a reconnection agrees on a new key, and the bootstrap storage is ciphered with it", async () => {
  const app = newApp()
  app.onStart()
  const first = await waitFor(() => FakeWebSocket.latest())
  const firstKey = await connect(app, first)
  await waitFor(() => sentAfterHandshake(first).length > 0)

  first.hostSend({ storage: { persistent: { token: hostCipher(firstKey, "t-1") } } })
  await waitFor(() => app.persistentStorage.get("token") === "t-1")

  first.hostClose(1006)
  app.connect()
  const second = await waitFor(() => (FakeWebSocket.instances.length === 2 ? FakeWebSocket.latest() : null))
  const secondKey = await connect(app, second)
  expect(secondKey.equals(firstKey)).toBe(false)

  const [bootstrap] = await waitFor(() => (sentAfterHandshake(second).length > 0 ? sentAfterHandshake(second) : null))
  const storage = bootstrap.storage as Record<string, Record<string, string>>
  expect(hostDecipher(secondKey, storage.persistent.token)).toBe("t-1")
  expect(() => hostDecipher(firstKey, storage.persistent.token)).toThrow()
})

test("a storage value that does not authenticate is refused, the others are kept", async () => {
  const app = newApp()
  app.onStart()
  const socket = await waitFor(() => FakeWebSocket.latest())
  const key = await connect(app, socket)
  await waitFor(() => sentAfterHandshake(socket).length > 0)

  const otherKey = new HostKeys().deriveKey(new HostKeys().serverKey, app.id)
  socket.hostSend({
    storage: { persistent: { forged: hostCipher(otherKey, "x"), good: hostCipher(key, "ok") } },
  })
  await waitFor(() => app.persistentStorage.get("good") === "ok")
  expect(app.persistentStorage.get("forged")).toBeNull()
})

test("no ping goes out before the handshake answer", async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true, toFake: ["setTimeout", "clearTimeout", "setInterval", "clearInterval"] })
  try {
    const app = newApp()
    app.onStart()
    const socket = await waitFor(() => FakeWebSocket.latest())
    socket.hostOpen()
    await waitFor(() => socket.sent[0])
    await vi.advanceTimersByTimeAsync(60_000)
    expect(socket.sent).toHaveLength(1)
    expect(socket.sent[0].protocol).toBe(1)
  } finally {
    vi.useRealTimers()
  }
})
