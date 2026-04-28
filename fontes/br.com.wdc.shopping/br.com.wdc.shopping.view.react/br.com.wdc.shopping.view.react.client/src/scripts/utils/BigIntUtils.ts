import Base64 from './Base64'

export type BigIntValue = bigint | boolean | number | string

export default class BigIntUtils {
  static readonly toBuffer = bigintToBuf
  static readonly fromBuffer = bufToBigint
  static readonly modPow = modPow
  static readonly parse = parse
}

const HEX_STR_EXP = /^(0x)?([\da-fA-F]+)$/
const HEX_BYTE_EXP = /[\da-fA-F]{2}/g

function parseHex(a: string, prefix0x?: boolean, byteLength?: number) {
  const hexMatch = a.match(HEX_STR_EXP)
  if (hexMatch == null) {
    throw new RangeError("input must be a hexadecimal string, e.g. '0x124fe3a' or '0214f1b2'")
  }
  let hex = hexMatch[2]
  if (byteLength !== undefined) {
    if (byteLength < hex.length / 2) {
      throw new RangeError(`expected byte length ${byteLength} < input hex byte length ${Math.ceil(hex.length / 2)}`)
    }
    hex = hex.padStart(byteLength * 2, '0')
  }
  return prefix0x ? '0x' + hex : hex
}

function hexToBuf(hexStr: string): Uint8Array {
  let hex = parseHex(hexStr)
  hex = hex.padStart(Math.ceil(hex.length / 2) * 2, '0')
  const intArray = (hex.match(HEX_BYTE_EXP) ?? []).map((h) => parseInt(h, 16))
  return Uint8Array.from(intArray)
}

function bigintToHex(a: bigint) {
  if (a < 0) {
    throw RangeError('a should be a non-negative integer. Negative values are not supported')
  }
  return a.toString(16)
}

function bufToBigint(buf: Uint8Array) {
  let bits = 8n
  if (ArrayBuffer.isView(buf)) {
    bits = BigInt(buf.BYTES_PER_ELEMENT * 8)
  } else {
    buf = new Uint8Array(buf)
  }

  let ret = 0n
  for (const i of buf.values()) {
    const bi = BigInt(i)
    ret = (ret << bits) + bi
  }
  return ret
}

function bigintToBuf(a: BigIntValue): Uint8Array {
  if (typeof a !== 'bigint') a = BigInt(a)
  if (a < 0) {
    throw RangeError('a should be a non-negative integer. Negative values are not supported')
  }
  return hexToBuf(bigintToHex(a))
}

function eGcd(a: bigint, b: bigint) {
  if (typeof a !== 'bigint') a = BigInt(a)
  if (typeof b !== 'bigint') b = BigInt(b)

  if (a <= 0n || b <= 0n) throw new RangeError('a and b MUST be > 0') // a and b MUST be positive

  let x = 0n
  let y = 1n
  let u = 1n
  let v = 0n

  while (a !== 0n) {
    const q = b / a
    const r = b % a
    const m = x - u * q
    const n = y - v * q
    b = a
    a = r
    x = u
    y = v
    u = m
    v = n
  }
  return { g: b, x, y }
}

function toZn(a: bigint | number, n: bigint | number) {
  if (typeof a !== 'bigint') a = BigInt(a)
  if (typeof n !== 'bigint') n = BigInt(n)

  if (n <= 0n) {
    throw new RangeError('n must be > 0')
  }

  const aZn = a % n
  return aZn < 0n ? aZn + n : aZn
}

function modInv(a: bigint, n: bigint) {
  const egcd = eGcd(toZn(a, n), n)
  if (egcd.g !== 1n) {
    // modular inverse does not exist
    throw new RangeError(`${a.toString()} does not have inverse modulo ${n.toString()}`)
  } else {
    return toZn(egcd.x, n)
  }
}

function modPow(b: BigIntValue, e: BigIntValue, n: BigIntValue): bigint {
  if (typeof b !== 'bigint') b = BigInt(b)
  if (typeof e !== 'bigint') e = BigInt(e)
  if (typeof n !== 'bigint') n = BigInt(n)

  if (n <= 0n) {
    throw new RangeError('n must be > 0')
  } else if (n === 1n) {
    return 0n
  }

  b = toZn(b, n)

  if (e < 0n) {
    return modInv(modPow(b, -e, n), n)
  }

  let r = 1n
  while (e > 0) {
    if (e % 2n === 1n) {
      r = (r * b) % n
    }
    e = e / 2n
    b = b ** 2n % n
  }
  return r
}

function parseBigInt(numberString: string, keyspace: string) {
  let result = 0n
  const keyspaceLength = BigInt(keyspace.length)
  for (let i = 0; i < numberString.length; i++) {
    const value = keyspace.indexOf(numberString[i])
    if (value === -1) throw new Error('invalid string')
    result = result * keyspaceLength + BigInt(value)
  }
  return result
}

function parse(numberString: string, radix: number, options?: { urlSafe: boolean }) {
  if (radix === 36) {
    return parseBigInt(numberString, '0123456789abcdefghijklmnopqrstuvwxyz')
  }

  if (radix === 64) {
    if (options && options.urlSafe) {
      return bufToBigint(Base64.decodeUrlSafe(numberString))
    }
    return bufToBigint(Base64.decode(numberString))
  }

  throw new Error(`Not implement bigint parser for RADIX=${radix}`)
}
