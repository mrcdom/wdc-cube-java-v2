import BigIntUtils, { type BigIntValue } from './BigIntUtils'

export default class RSA {
  static readonly N65537 = 65537n

  #publicExponent: bigint
  #privateKey: bigint
  #publicKey: bigint

  constructor(publicExponent: BigIntValue, privateKey: BigIntValue, publicKey: BigIntValue) {
    if (typeof publicExponent !== 'bigint') publicExponent = BigInt(publicExponent)
    if (typeof privateKey !== 'bigint') privateKey = BigInt(privateKey)
    if (typeof publicKey !== 'bigint') publicKey = BigInt(publicKey)

    this.#publicExponent = publicExponent
    this.#privateKey = privateKey
    this.#publicKey = publicKey
  }

  getPublicExponent() {
    return this.#publicExponent
  }

  getPublicKey() {
    return this.#publicKey
  }

  getPrivateKey() {
    return this.#privateKey
  }

  encrypt(message: BigIntValue) {
    return BigIntUtils.modPow(message, this.#publicExponent, this.#publicKey)
  }

  decrypt(encrypted: BigIntValue) {
    return BigIntUtils.modPow(encrypted, this.#privateKey, this.#publicKey)
  }

  toString() {
    return [
      '{',
      'publicExponent: ',
      this.#publicExponent.toString(16),
      ', publicKey: ',
      this.#publicKey.toString(16),
      ', private: ',
      this.#privateKey.toString(16),
      '}',
    ].join()
  }
}
