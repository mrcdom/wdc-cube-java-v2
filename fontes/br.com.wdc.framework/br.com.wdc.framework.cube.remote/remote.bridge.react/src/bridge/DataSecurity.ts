import { type ChannelCrypto, type ChannelKey, type KeyAgreement, PROTOCOL, defaultChannelCrypto } from "./ChannelCrypto"

/**
 * The security of the current connection: its handshake and the key it agreed on (protocol version 1,
 * docs/bridge-skeleton-protocol.md §6).
 *
 * Each connection starts with {@link beginHandshake}, which forgets the previous connection's key; ciphering and
 * deciphering wait until {@link completeHandshake} derives the new one.
 */
export class DataSecurity {
  private agreement: KeyAgreement | null = null
  private key: ChannelKey | null = null
  private keyReady!: Promise<ChannelKey>
  private resolveKey!: (key: ChannelKey) => void

  constructor(private readonly crypto: ChannelCrypto = defaultChannelCrypto()) {
    this.resetKey()
  }

  private resetKey() {
    this.key = null
    this.keyReady = new Promise((resolve) => {
      this.resolveKey = resolve
    })
  }

  /** Whether the current connection has its key. */
  hasKey() {
    return this.key !== null
  }

  /** Starts a connection's handshake: a new ephemeral key pair, and the message that carries its public key. */
  async beginHandshake(): Promise<{ protocol: number; clientKey: string }> {
    if (this.key) {
      this.resetKey()
    }
    const agreement = await this.crypto.newKeyAgreement()
    this.agreement = agreement
    return { protocol: PROTOCOL, clientKey: agreement.publicKey }
  }

  /** Derives the connection key from the host's answer. */
  async completeHandshake(serverKey: string, appId: string) {
    const agreement = this.agreement
    if (!agreement) {
      throw new Error("No handshake in progress")
    }
    this.agreement = null
    const key = await agreement.deriveKey(serverKey, appId)
    this.key = key
    this.resolveKey(key)
  }

  /** Ciphers a value with the connection key, waiting for the handshake if it is not done yet. */
  async b64Cipher(text: string) {
    return (await this.keyReady).cipher(text)
  }

  /** Deciphers a value with the connection key; rejects one that does not authenticate. */
  async b64Decipher(value: string) {
    return (await this.keyReady).decipher(value)
  }
}
