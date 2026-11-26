/**
 * Runs the client fixtures of the remote protocol (conformance/client/*.json, see conformance/README.md) against
 * the bridge, with a fake WebSocket the test drives as the host. Each fixture runs with both channel cryptography
 * implementations: Web Crypto and the pure JS one for plain HTTP.
 */
import { readdirSync, readFileSync } from "node:fs"
import { join } from "node:path"
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest"

import { ScopeCoordinator } from "@/bridge/ScopeCoordinator"
import { type ChannelCrypto, NobleChannelCrypto, WebChannelCrypto } from "@/bridge/ChannelCrypto"
import { BROWSER_VSID, ON_ALERT_OK, OUTDATED_MESSAGE } from "@/bridge/constants"
import type { BrowserScope } from "@/bridge/types"
import type { ClientStorage } from "@/bridge/ClientStorage"
import { FakeWebSocket, HostKeys, hostCipher, isPing, waitFor } from "./remote-host"

const FIXTURES = join(__dirname, "..", "..", "conformance", "client")
const QUIET_MS = 300

type Step = Record<string, unknown>
type Fixture = { description: string; steps: Step[] }

const fixtures = readdirSync(FIXTURES)
  .filter((name) => name.endsWith(".json"))
  .sort()
  .map((name) => ({ name, fixture: JSON.parse(readFileSync(join(FIXTURES, name), "utf8")) as Fixture }))

const cryptos: [string, () => ChannelCrypto][] = [
  ["Web Crypto", () => new WebChannelCrypto(crypto.subtle)],
  ["pure JS", () => new NobleChannelCrypto()],
]

// :: Matching (conformance/README.md → Matching)

function matches(expected: unknown, actual: unknown, path = "$"): string | null {
  if (expected === "$any") {
    return actual === undefined ? `${path}: missing` : null
  }
  if (expected === "$string") {
    return typeof actual === "string" ? null : `${path}: not a string`
  }
  if (expected === "$number") {
    return typeof actual === "number" ? null : `${path}: not a number`
  }
  if (Array.isArray(expected)) {
    if (!Array.isArray(actual) || actual.length !== expected.length) {
      return `${path}: not an array of ${expected.length}`
    }
    for (let i = 0; i < expected.length; i++) {
      const failure = matches(expected[i], actual[i], `${path}[${i}]`)
      if (failure) {
        return failure
      }
    }
    return null
  }
  if (expected !== null && typeof expected === "object") {
    const contains = (expected as Record<string, unknown>)["$contains"]
    if (contains !== undefined) {
      return Array.isArray(actual) && actual.some((item) => matches(contains, item) === null)
        ? null
        : `${path}: no element matches`
    }
    if (actual === null || typeof actual !== "object") {
      return `${path}: not an object`
    }
    for (const [key, value] of Object.entries(expected)) {
      const field = (actual as Record<string, unknown>)[key]
      if (value === "$absent") {
        if (field !== undefined) {
          return `${path}.${key}: present`
        }
        continue
      }
      const failure = matches(value, field, `${path}.${key}`)
      if (failure) {
        return failure
      }
    }
    return null
  }
  return Object.is(expected, actual) ? null : `${path}: ${JSON.stringify(actual)} != ${JSON.stringify(expected)}`
}

// :: Runner

class ClientRunner {
  readonly app: ScopeCoordinator
  private consumed = 0
  private clientKey: string | null = null
  private key: Buffer | null = null

  constructor(channelCrypto: ChannelCrypto) {
    this.app = new ScopeCoordinator("~cf:", { channelCrypto })
    started.push(this.app)
  }

  private socket() {
    const socket = FakeWebSocket.latest()
    if (!socket) {
      throw new Error("The client opened no connection")
    }
    return socket
  }

  /** The messages sent since the last one consumed, keep-alives left out. */
  private unread() {
    return this.socket()
      .sent.slice(this.consumed)
      .filter((message) => !isPing(message))
  }

  private nextSent() {
    const sent = this.socket().sent
    while (this.consumed < sent.length) {
      const message = sent[this.consumed++]
      if (!isPing(message)) {
        return message
      }
    }
    return undefined
  }

  private substitute(value: unknown): unknown {
    if (value === "$serverKey") {
      const host = new HostKeys()
      this.key = host.deriveKey(this.clientKey!, this.app.id)
      return host.serverKey
    }
    if (typeof value === "string" && value.startsWith("$cipher:")) {
      expect(this.key, "$cipher before $serverKey").not.toBeNull()
      return hostCipher(this.key!, value.substring("$cipher:".length))
    }
    if (Array.isArray(value)) {
      return value.map((item) => this.substitute(item))
    }
    if (value !== null && typeof value === "object") {
      return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, this.substitute(v)]))
    }
    return value
  }

  private storage(scope: string): ClientStorage {
    switch (scope) {
      case "session":
        return this.app.sessionStorage
      case "persistent":
        return this.app.persistentStorage
      case "persistent-secure":
        return this.app.persistentStorage.secure
      default:
        throw new Error("Unknown storage scope: " + scope)
    }
  }

  async run(step: Step) {
    if ("start" in step) {
      this.app.onStart()
      const socket = await waitFor(() => FakeWebSocket.latest())
      socket.hostOpen()
    } else if ("expectSent" in step) {
      const message = await waitFor(() => this.nextSent())
      expect(matches(step.expectSent, message), JSON.stringify(message)).toBeNull()
      if (typeof message.clientKey === "string") {
        this.clientKey = message.clientKey
      }
    } else if ("expectNothingSent" in step) {
      await new Promise((resolve) => setTimeout(resolve, QUIET_MS))
      expect(this.unread()).toEqual([])
    } else if ("receive" in step) {
      this.socket().hostSend(this.substitute(step.receive))
    } else if ("expectOutdated" in step) {
      await this.expectOutdated()
    } else if ("expectStorage" in step) {
      const { scope, key, value } = step.expectStorage as { scope: string; key: string; value: string }
      await waitFor(() => this.storage(scope).get(key) === value).catch(() => undefined)
      expect(this.storage(scope).get(key)).toBe(value)
    } else {
      throw new Error("Unknown step: " + JSON.stringify(step))
    }
  }

  private async expectOutdated() {
    const app = this.app
    await waitFor(() => app.contextExchanger.outdated).catch(() => undefined)
    expect(app.contextExchanger.outdated).toBe(true)

    const scope = app.viewMap.get(BROWSER_VSID)!.getValues() as BrowserScope
    expect(scope.outdated).toBe(true)
    expect(scope.alertArgs).toEqual([OUTDATED_MESSAGE])

    // The host closes with 4002; the client does not reconnect, even when asked to.
    const connections = FakeWebSocket.instances.length
    this.socket().hostClose(4002)
    app.connect()
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(FakeWebSocket.instances.length).toBe(connections)
    expect(app.reconnectController.reconnectHandler).toBe(0)
    expect(scope.error).toBeUndefined()

    // The alert's OK reloads the page.
    const reloads = (location as unknown as { reloads: number }).reloads
    app.submit(BROWSER_VSID, ON_ALERT_OK)
    expect((location as unknown as { reloads: number }).reloads).toBe(reloads + 1)
  }
}

const started: ScopeCoordinator[] = []

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
  FakeWebSocket.instances = []
  vi.stubGlobal("WebSocket", FakeWebSocket)
})

afterEach(() => {
  for (const socket of FakeWebSocket.instances) {
    socket.hostClose(1000)
  }
  for (const app of started.splice(0)) {
    app.reconnectController.close()
  }
  vi.unstubAllGlobals()
})

for (const [cryptoName, makeCrypto] of cryptos) {
  describe(`conformance (${cryptoName})`, () => {
    test("has fixtures", () => {
      expect(fixtures.length).toBeGreaterThan(0)
    })

    for (const { name, fixture } of fixtures) {
      test(`${name}: ${fixture.description}`, async () => {
        const runner = new ClientRunner(makeCrypto())
        for (const step of fixture.steps) {
          await runner.run(step)
        }
      })
    }
  })
}
