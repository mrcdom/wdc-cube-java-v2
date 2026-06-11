import { pbkdf2 } from "@noble/hashes/pbkdf2"
import { sha256 } from "@noble/hashes/sha2"
import { gcm } from "@noble/ciphers/aes"

import BigIntUtils from "@/utils/BigIntUtils"
import RSA from "@/utils/RSA"
import UTF8 from "@/utils/UTF8"
import Base64 from "@/utils/Base64"

// crypto.subtle só existe em secure contexts (HTTPS ou localhost). Em HTTP puro
// (ex.: QA acessado por IP), cai no fallback JS puro (@noble) com os MESMOS
// parâmetros (PBKDF2-SHA256 250k → AES-256-GCM, tag de 128 bits anexada) — o
// servidor decifra os dois caminhos de forma idêntica.
const SUBTLE: SubtleCrypto | undefined =
  typeof crypto !== "undefined" && crypto.subtle ? crypto.subtle : undefined

const PBKDF2_ITERATIONS = 250000

class RsaHelper {
  private readonly __rsa: RSA

  constructor(skey: string) {
    const [exponent, key] = skey.split(/:/)

    const publicExponent = BigIntUtils.parse(exponent, 36)
    const privateKey = 0n
    const publicKey = BigIntUtils.parse(key, 36)
    this.__rsa = new RSA(publicExponent, privateKey, publicKey)
  }

  encryptToBase36(message: Uint8Array) {
    const messageAsSafeBytes = UTF8.encode(Base64.encode(message))
    const messageAsBigint = BigIntUtils.fromBuffer(messageAsSafeBytes)

    const messageEncryptedAsBigInt = this.__rsa.encrypt(messageAsBigint)
    return messageEncryptedAsBigInt.toString(36)
  }
}

export class DataSecurity {
  private __iv!: Uint8Array<ArrayBuffer>
  private __key: CryptoKey | null = null
  private __keyBytes: Uint8Array | null = null
  private __signature!: string
  private __rsa!: RsaHelper

  updateSecurityKey(appSKey: string) {
    this.__rsa = new RsaHelper(appSKey)
  }

  async updateSecretWithRandomPassword() {
    const pwd = Base64.encodeUrlSafe(globalThis.crypto.getRandomValues(new Uint8Array(12)))
    const pwdBuf = UTF8.encode(pwd)
    await this.updateSecret(pwdBuf)
  }

  async updateSecret(password: Uint8Array) {
    const salt = crypto.getRandomValues(new Uint8Array(16)) as Uint8Array<ArrayBuffer>
    this.__iv = crypto.getRandomValues(new Uint8Array(12)) as Uint8Array<ArrayBuffer>

    if (SUBTLE) {
      const rawKey = await SUBTLE.importKey("raw", password as Uint8Array<ArrayBuffer>, { name: "PBKDF2" }, false, [
        "deriveKey",
      ])

      this.__key = await SUBTLE.deriveKey(
        { name: "PBKDF2", salt, iterations: PBKDF2_ITERATIONS, hash: "SHA-256" },
        rawKey,
        { name: "AES-GCM", length: 256 },
        false,
        ["encrypt", "decrypt"],
      )
    } else {
      this.__keyBytes = pbkdf2(sha256, password, salt, { c: PBKDF2_ITERATIONS, dkLen: 32 })
    }

    const cryptedPwd = this.__rsa.encryptToBase36(password)

    this.__signature = `${cryptedPwd}.${Base64.encodeUrlSafe(salt)}.${Base64.encodeUrlSafe(this.__iv)}`
  }

  getSignature() {
    return this.__signature
  }

  async b64Cipher(text: string) {
    const textAsUtf8 = UTF8.encode(text)
    if (SUBTLE && this.__key) {
      const ciphered = await SUBTLE.encrypt(
        { name: "AES-GCM", iv: this.__iv },
        this.__key,
        textAsUtf8 as Uint8Array<ArrayBuffer>,
      )
      return Base64.encode(new Uint8Array(ciphered))
    }
    return Base64.encode(gcm(this.__keyBytes!, this.__iv).encrypt(textAsUtf8))
  }

  async b64Decipher(b64CipheredText: string) {
    const ciphered = Base64.decode(b64CipheredText)
    if (SUBTLE && this.__key) {
      const decrypted = await SUBTLE.decrypt(
        { name: "AES-GCM", iv: this.__iv },
        this.__key,
        ciphered as Uint8Array<ArrayBuffer>,
      )
      return UTF8.decode(decrypted)
    }
    return UTF8.decode(gcm(this.__keyBytes!, this.__iv).decrypt(ciphered))
  }
}
