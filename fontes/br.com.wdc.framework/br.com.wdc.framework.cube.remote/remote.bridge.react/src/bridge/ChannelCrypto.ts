import { p256 } from "@noble/curves/nist"
import { hkdf } from "@noble/hashes/hkdf"
import { sha256 } from "@noble/hashes/sha2"
import { gcm } from "@noble/ciphers/aes"

import Base64 from "@/utils/Base64"
import UTF8 from "@/utils/UTF8"

/**
 * The cryptography of the remote channel, protocol version 1 (docs/bridge-skeleton-protocol.md §6):
 *
 * - **Key agreement**, per connection: an ephemeral ECDH P-256 key pair; public keys travel as Base64 of the
 *   X.509 SubjectPublicKeyInfo.
 * - **Connection key**: HKDF-SHA256 over the raw 32-byte ECDH x-coordinate, salted with the `appId` (UTF-8), with
 *   {@link KEY_INFO} as info, 32 bytes — an AES-256 key.
 * - **A ciphered value**: AES-256-GCM, 128-bit tag, a random 12-byte IV of its own, written as
 *   `Base64(IV ‖ ciphertext ‖ tag)`.
 *
 * Two implementations with identical parameters: Web Crypto ({@link WebChannelCrypto}), which only exists in secure
 * contexts (HTTPS or localhost), and a pure JS one ({@link NobleChannelCrypto}) for plain HTTP.
 */

/** The version of the remote protocol this bridge speaks. */
export const PROTOCOL = 1

/** The HKDF info that names what the derived key is for. */
export const KEY_INFO = "wdc-cube-remote/aes-256-gcm/v1"

const IV_LENGTH = 12
const TAG_LENGTH = 16
const KEY_LENGTH = 32

/** The key one connection agreed on: ciphers and deciphers values. */
export interface ChannelKey {
  /** `Base64(IV ‖ ciphertext ‖ tag)`, with a random IV. */
  cipher(text: string): Promise<string>
  /** Rejects a value that does not authenticate (tampered, or ciphered with another key). */
  decipher(value: string): Promise<string>
}

/** One end of an ephemeral ECDH P-256 agreement. */
export interface KeyAgreement {
  /** Base64 of the X.509 SubjectPublicKeyInfo of this end's public key. */
  readonly publicKey: string
  /** The connection key, from the peer's public key (Base64 SubjectPublicKeyInfo) and the `appId`. */
  deriveKey(peerPublicKey: string, appId: string): Promise<ChannelKey>
}

export interface ChannelCrypto {
  newKeyAgreement(): Promise<KeyAgreement>
  /** HKDF with HMAC-SHA256 (RFC 5869): extract, then expand to `length` bytes. */
  hkdfSha256(ikm: Uint8Array, salt: Uint8Array, info: Uint8Array, length: number): Promise<Uint8Array>
  /** A connection key from its raw 32 bytes. */
  importKey(raw: Uint8Array): Promise<ChannelKey>
}

function randomIv() {
  return crypto.getRandomValues(new Uint8Array(IV_LENGTH))
}

function join(iv: Uint8Array, sealed: Uint8Array) {
  const out = new Uint8Array(iv.length + sealed.length)
  out.set(iv, 0)
  out.set(sealed, iv.length)
  return Base64.encode(out)
}

function split(value: string) {
  const bytes = Base64.decode(value)
  if (bytes.length < IV_LENGTH + TAG_LENGTH) {
    throw new Error("A ciphered value is too short")
  }
  return { iv: bytes.subarray(0, IV_LENGTH), sealed: bytes.subarray(IV_LENGTH) }
}

function hkdfInputs(appId: string) {
  return { salt: UTF8.encode(appId), info: UTF8.encode(KEY_INFO) }
}

function buf(bytes: Uint8Array): Uint8Array<ArrayBuffer> {
  return bytes as Uint8Array<ArrayBuffer>
}

// :: Web Crypto

const ECDH_P256 = { name: "ECDH", namedCurve: "P-256" } as const

export class WebChannelCrypto implements ChannelCrypto {
  constructor(private readonly subtle: SubtleCrypto) {}

  async newKeyAgreement(): Promise<KeyAgreement> {
    const subtle = this.subtle
    const pair = (await subtle.generateKey(ECDH_P256, false, ["deriveBits"])) as CryptoKeyPair
    const publicKey = Base64.encode(new Uint8Array(await subtle.exportKey("spki", pair.publicKey)))

    return {
      publicKey,
      deriveKey: async (peerPublicKey, appId) => {
        const peer = await subtle.importKey("spki", buf(Base64.decode(peerPublicKey)), ECDH_P256, false, [])
        const shared = new Uint8Array(await subtle.deriveBits({ name: "ECDH", public: peer }, pair.privateKey, 256))
        const { salt, info } = hkdfInputs(appId)
        try {
          return await this.importKey(await this.hkdfSha256(shared, salt, info, KEY_LENGTH))
        } finally {
          shared.fill(0)
        }
      },
    }
  }

  async hkdfSha256(ikm: Uint8Array, salt: Uint8Array, info: Uint8Array, length: number) {
    const key = await this.subtle.importKey("raw", buf(ikm), "HKDF", false, ["deriveBits"])
    const bits = await this.subtle.deriveBits(
      { name: "HKDF", hash: "SHA-256", salt: buf(salt), info: buf(info) },
      key,
      length * 8,
    )
    return new Uint8Array(bits)
  }

  async importKey(raw: Uint8Array): Promise<ChannelKey> {
    const subtle = this.subtle
    const key = await subtle.importKey("raw", buf(raw), "AES-GCM", false, ["encrypt", "decrypt"])
    raw.fill(0)
    return {
      async cipher(text) {
        const iv = randomIv()
        const sealed = await subtle.encrypt({ name: "AES-GCM", iv }, key, buf(UTF8.encode(text)))
        return join(iv, new Uint8Array(sealed))
      },
      async decipher(value) {
        const { iv, sealed } = split(value)
        const plain = await subtle.decrypt({ name: "AES-GCM", iv: buf(iv) }, key, buf(sealed))
        return UTF8.decode(plain)
      },
    }
  }
}

// :: Pure JS (@noble), for plain HTTP

/** DER of `SEQUENCE { OID id-ecPublicKey, OID prime256v1 }`, the AlgorithmIdentifier of a P-256 public key. */
const P256_ALGORITHM = Uint8Array.from([
  0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01, 0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03,
  0x01, 0x07,
])

/** The SubjectPublicKeyInfo of a P-256 point (uncompressed, 65 bytes). */
export function encodeSpki(point: Uint8Array): Uint8Array {
  const bitString = [0x03, point.length + 1, 0x00]
  const bodyLength = P256_ALGORITHM.length + bitString.length + point.length
  const out = new Uint8Array(2 + bodyLength)
  out.set([0x30, bodyLength], 0)
  out.set(P256_ALGORITHM, 2)
  out.set(bitString, 2 + P256_ALGORITHM.length)
  out.set(point, 2 + P256_ALGORITHM.length + bitString.length)
  return out
}

/** The P-256 point of a SubjectPublicKeyInfo; throws if it is not a P-256 public key. */
export function decodeSpki(spki: Uint8Array): Uint8Array {
  const fail = () => new Error("Not a P-256 public key")
  if (spki[0] !== 0x30 || spki[1] !== spki.length - 2) {
    throw fail()
  }
  for (let i = 0; i < P256_ALGORITHM.length; i++) {
    if (spki[2 + i] !== P256_ALGORITHM[i]) {
      throw fail()
    }
  }
  let pos = 2 + P256_ALGORITHM.length
  if (spki[pos] !== 0x03 || spki[pos + 1] !== spki.length - pos - 2 || spki[pos + 2] !== 0x00) {
    throw fail()
  }
  pos += 3
  const point = spki.slice(pos)
  try {
    p256.Point.fromHex(point).assertValidity()
  } catch {
    throw fail()
  }
  return point
}

export class NobleChannelCrypto implements ChannelCrypto {
  async newKeyAgreement(): Promise<KeyAgreement> {
    const secretKey = p256.utils.randomSecretKey()
    const publicKey = Base64.encode(encodeSpki(p256.getPublicKey(secretKey, false)))

    return {
      publicKey,
      deriveKey: async (peerPublicKey, appId) => {
        const peer = decodeSpki(Base64.decode(peerPublicKey))
        // The compressed shared point is 0x02/0x03 ‖ x: the x-coordinate is what ECDH gives.
        const shared = p256.getSharedSecret(secretKey, peer, true).slice(1)
        const { salt, info } = hkdfInputs(appId)
        try {
          return await this.importKey(await this.hkdfSha256(shared, salt, info, KEY_LENGTH))
        } finally {
          shared.fill(0)
        }
      },
    }
  }

  async hkdfSha256(ikm: Uint8Array, salt: Uint8Array, info: Uint8Array, length: number) {
    return hkdf(sha256, ikm, salt, info, length)
  }

  async importKey(raw: Uint8Array): Promise<ChannelKey> {
    const key = raw.slice()
    raw.fill(0)
    return {
      async cipher(text) {
        const iv = randomIv()
        return join(iv, gcm(key, iv).encrypt(UTF8.encode(text)))
      },
      async decipher(value) {
        const { iv, sealed } = split(value)
        return UTF8.decode(gcm(key, iv).decrypt(sealed))
      },
    }
  }
}

/** Web Crypto when the context has it (HTTPS or localhost), otherwise the pure JS implementation. */
export function defaultChannelCrypto(): ChannelCrypto {
  const subtle = typeof crypto !== "undefined" ? crypto.subtle : undefined
  return subtle ? new WebChannelCrypto(subtle) : new NobleChannelCrypto()
}
