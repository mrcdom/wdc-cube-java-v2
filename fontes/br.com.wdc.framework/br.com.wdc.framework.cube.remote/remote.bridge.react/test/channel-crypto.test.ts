import { describe, expect, test } from "vitest"

import { type ChannelCrypto, NobleChannelCrypto, WebChannelCrypto, decodeSpki } from "@/bridge/ChannelCrypto"
import Base64 from "@/utils/Base64"
import { HostKeys, hostCipher, hostDecipher } from "./remote-host"

const APP_ID = "3kQ9vXbT0aLmZ2pR7yNcW4eHuJ8sDfGi.Vt5rQ1nMxB6kZp3a"

const hex = (text: string) => Uint8Array.from(text.match(/../g)!.map((byte) => Number.parseInt(byte, 16)))
const toHex = (bytes: Uint8Array) => Buffer.from(bytes).toString("hex")

const implementations: [string, () => ChannelCrypto][] = [
  ["Web Crypto", () => new WebChannelCrypto(crypto.subtle)],
  ["pure JS", () => new NobleChannelCrypto()],
]

for (const [name, make] of implementations) {
  describe(`channel crypto (${name})`, () => {
    const channel = make()

    test("HKDF-SHA256 is that of RFC 5869 (test case 1)", async () => {
      const okm = await channel.hkdfSha256(
        new Uint8Array(22).fill(0x0b),
        hex("000102030405060708090a0b0c"),
        hex("f0f1f2f3f4f5f6f7f8f9"),
        42,
      )
      expect(toHex(okm)).toBe("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")
    })

    test("both ends of an agreement derive the same key", async () => {
      const client = await channel.newKeyAgreement()
      const server = await channel.newKeyAgreement()
      const clientKey = await client.deriveKey(server.publicKey, APP_ID)
      const serverKey = await server.deriveKey(client.publicKey, APP_ID)
      expect(await serverKey.decipher(await clientKey.cipher("from the client"))).toBe("from the client")
      expect(await clientKey.decipher(await serverKey.cipher("from the server"))).toBe("from the server")
    })

    test("the key is the host's: Node's ECDH and HKDF agree with it", async () => {
      const client = await channel.newKeyAgreement()
      const host = new HostKeys()
      const hostKey = host.deriveKey(client.publicKey, APP_ID)
      const clientKey = await client.deriveKey(host.serverKey, APP_ID)
      expect(hostDecipher(hostKey, await clientKey.cipher("ç 🙂"))).toBe("ç 🙂")
      expect(await clientKey.decipher(hostCipher(hostKey, "ç 🙂"))).toBe("ç 🙂")
    })

    test("another appId derives another key", async () => {
      const client = await channel.newKeyAgreement()
      const server = await channel.newKeyAgreement()
      const clientKey = await client.deriveKey(server.publicKey, APP_ID)
      const serverKey = await server.deriveKey(client.publicKey, APP_ID + "x")
      await expect(serverKey.decipher(await clientKey.cipher("text"))).rejects.toThrow()
    })

    test("the public key is a P-256 SubjectPublicKeyInfo", async () => {
      const agreement = await channel.newKeyAgreement()
      const spki = Base64.decode(agreement.publicKey)
      expect(spki.length).toBe(91)
      expect(decodeSpki(spki).length).toBe(65)
    })

    test("ciphering twice gives different values, and both decipher", async () => {
      const key = await channel.importKey(crypto.getRandomValues(new Uint8Array(32)))
      const first = await key.cipher("the same text")
      const second = await key.cipher("the same text")
      expect(first).not.toBe(second)
      expect(Base64.decode(first).length).toBe(12 + "the same text".length + 16)
      expect(await key.decipher(first)).toBe("the same text")
      expect(await key.decipher(second)).toBe("the same text")
    })

    test("a tampered byte is refused", async () => {
      const key = await channel.importKey(crypto.getRandomValues(new Uint8Array(32)))
      const bytes = Base64.decode(await key.cipher("do not touch"))
      for (const index of [0, 12, bytes.length - 1]) {
        const tampered = bytes.slice()
        tampered[index] ^= 0x01
        await expect(key.decipher(Base64.encode(tampered))).rejects.toThrow()
      }
    })

    test("a value ciphered with another key is refused", async () => {
      const key = await channel.importKey(crypto.getRandomValues(new Uint8Array(32)))
      const other = await channel.importKey(crypto.getRandomValues(new Uint8Array(32)))
      await expect(other.decipher(await key.cipher("text"))).rejects.toThrow()
    })

    test("a peer key that is not P-256 is refused", async () => {
      const agreement = await channel.newKeyAgreement()
      const spki = Base64.decode((await channel.newKeyAgreement()).publicKey)
      spki[spki.length - 1] ^= 0x01 // no longer a point on the curve
      await expect(agreement.deriveKey(Base64.encode(spki), APP_ID)).rejects.toThrow()
    })
  })
}

test("the two implementations interoperate", async () => {
  const web = await new WebChannelCrypto(crypto.subtle).newKeyAgreement()
  const noble = await new NobleChannelCrypto().newKeyAgreement()
  const webKey = await web.deriveKey(noble.publicKey, APP_ID)
  const nobleKey = await noble.deriveKey(web.publicKey, APP_ID)
  expect(await nobleKey.decipher(await webKey.cipher("across"))).toBe("across")
  expect(await webKey.decipher(await nobleKey.cipher("back"))).toBe("back")
})
