/**
 * The host's end of a fake connection, for driving the bridge in tests: a WebSocket the test controls, and the
 * host's half of the protocol's cryptography done with Node's own crypto (independent of the bridge's).
 */
import * as nodeCrypto from "node:crypto"

export class FakeWebSocket {
  static readonly CONNECTING = 0
  static readonly OPEN = 1
  static readonly CLOSING = 2
  static readonly CLOSED = 3

  static instances: FakeWebSocket[] = []

  readyState = FakeWebSocket.CONNECTING
  readonly sent: Record<string, unknown>[] = []

  onopen: ((event: unknown) => void) | null = null
  onmessage: ((event: { data: string }) => void) | null = null
  onclose: ((event: { code: number }) => void) | null = null
  onerror: ((event: unknown) => void) | null = null

  constructor(
    readonly url: string,
    readonly protocols?: string[],
  ) {
    FakeWebSocket.instances.push(this)
  }

  static latest() {
    return FakeWebSocket.instances.at(-1)
  }

  send(data: string) {
    if (this.readyState !== FakeWebSocket.OPEN) {
      throw new Error("send on a socket that is not open")
    }
    this.sent.push(JSON.parse(data))
  }

  close() {
    this.hostClose(1000)
  }

  // :: The host's side

  hostOpen() {
    this.readyState = FakeWebSocket.OPEN
    this.onopen?.({})
  }

  hostSend(message: unknown) {
    this.onmessage?.({ data: JSON.stringify(message) })
  }

  hostClose(code: number) {
    if (this.readyState === FakeWebSocket.CLOSED) {
      return
    }
    this.readyState = FakeWebSocket.CLOSED
    this.onclose?.({ code })
  }
}

/** The host's half of a connection's handshake. */
export class HostKeys {
  private readonly pair = nodeCrypto.generateKeyPairSync("ec", { namedCurve: "P-256" })
  readonly serverKey = (this.pair.publicKey.export({ format: "der", type: "spki" }) as Buffer).toString("base64")

  /** The connection key, as the host derives it (§6.2). */
  deriveKey(clientKey: string, appId: string): Buffer {
    const peer = nodeCrypto.createPublicKey({ key: Buffer.from(clientKey, "base64"), format: "der", type: "spki" })
    const shared = nodeCrypto.diffieHellman({ privateKey: this.pair.privateKey, publicKey: peer })
    const info = "wdc-cube-remote/aes-256-gcm/v1"
    return Buffer.from(nodeCrypto.hkdfSync("sha256", shared, Buffer.from(appId, "utf8"), info, 32))
  }
}

/** `Base64(IV ‖ ciphertext ‖ tag)` with a random IV (§6.3). */
export function hostCipher(key: Buffer, text: string) {
  const iv = nodeCrypto.randomBytes(12)
  const cipher = nodeCrypto.createCipheriv("aes-256-gcm", key, iv)
  const body = Buffer.concat([cipher.update(text, "utf8"), cipher.final()])
  return Buffer.concat([iv, body, cipher.getAuthTag()]).toString("base64")
}

export function hostDecipher(key: Buffer, value: string) {
  const bytes = Buffer.from(value, "base64")
  const decipher = nodeCrypto.createDecipheriv("aes-256-gcm", key, bytes.subarray(0, 12))
  decipher.setAuthTag(bytes.subarray(bytes.length - 16))
  return Buffer.concat([decipher.update(bytes.subarray(12, bytes.length - 16)), decipher.final()]).toString("utf8")
}

export function isPing(message: Record<string, unknown>) {
  return message.ping === true && Object.keys(message).length === 1
}

export async function waitFor<T>(probe: () => T | undefined | null | false, timeoutMs = 3000): Promise<T> {
  const deadline = Date.now() + timeoutMs
  for (;;) {
    const value = probe()
    if (value) {
      return value
    }
    if (Date.now() > deadline) {
      throw new Error("Timed out")
    }
    await new Promise((resolve) => setTimeout(resolve, 10))
  }
}
