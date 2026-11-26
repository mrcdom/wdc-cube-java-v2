/**
 * Checks the built crypto fallback (src/main/webapp/js/crypto-compat.js) against known answers and against
 * the platform's Web Crypto, which is what the bridge uses in a secure context and agrees with the host's JCA.
 *
 * Run from the module root: npm run test:crypto-compat
 */
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { runInThisContext } from 'node:vm'

const bundle = fileURLToPath(new URL('../../main/webapp/js/crypto-compat.js', import.meta.url))
runInThisContext(readFileSync(bundle, 'utf8'), { filename: bundle })
const fb = globalThis.wdcCryptoFallback
const subtle = globalThis.crypto.subtle

const hex = (s) => Uint8Array.from(s.match(/../g), (h) => parseInt(h, 16))
const toHex = (b) => Buffer.from(b).toString('hex')
const utf8 = (s) => new TextEncoder().encode(s)

const tests = []
const test = (name, fn) => tests.push([name, fn])

test('HKDF-SHA256: RFC 5869 test case 1', () => {
  const okm = fb.hkdfSha256(hex('0b'.repeat(22)), hex('000102030405060708090a0b0c'), hex('f0f1f2f3f4f5f6f7f8f9'), 42)
  assert.equal(toHex(okm),
    '3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865')
})

test('ECDH: two fallback key pairs agree; the public key is a 91-byte SPKI', () => {
  const a = fb.ecdhKeyPair()
  const b = fb.ecdhKeyPair()
  assert.equal(a.publicKey.length, 91)
  const ab = fb.ecdhSharedSecret(a.privateKey, b.publicKey)
  const ba = fb.ecdhSharedSecret(b.privateKey, a.publicKey)
  assert.equal(ab.length, 32)
  assert.equal(toHex(ab), toHex(ba))
})

test('ECDH: a fallback key pair agrees with a Web Crypto key pair (both ways)', async () => {
  const a = fb.ecdhKeyPair()
  const b = await subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' }, false, ['deriveBits'])
  const bSpki = new Uint8Array(await subtle.exportKey('spki', b.publicKey))
  const aPub = await subtle.importKey('spki', a.publicKey, { name: 'ECDH', namedCurve: 'P-256' }, false, [])
  const fromSubtle = new Uint8Array(await subtle.deriveBits({ name: 'ECDH', public: aPub }, b.privateKey, 256))
  assert.equal(toHex(fb.ecdhSharedSecret(a.privateKey, bSpki)), toHex(fromSubtle))
})

test('ECDH: a peer key that is not a P-256 SPKI is refused', () => {
  const a = fb.ecdhKeyPair()
  assert.throws(() => fb.ecdhSharedSecret(a.privateKey, new Uint8Array(91)))
  const offCurve = a.publicKey.slice()
  offCurve[offCurve.length - 1] ^= 1
  assert.throws(() => fb.ecdhSharedSecret(a.privateKey, offCurve))
})

test('HKDF: the connection key matches Web Crypto HKDF', async () => {
  const ikm = crypto.getRandomValues(new Uint8Array(32))
  const salt = utf8('part1.part2')
  const info = utf8('wdc-cube-remote/aes-256-gcm/v1')
  const base = await subtle.importKey('raw', ikm, 'HKDF', false, ['deriveBits'])
  const expected = new Uint8Array(await subtle.deriveBits({ name: 'HKDF', hash: 'SHA-256', salt, info }, base, 256))
  assert.equal(toHex(fb.hkdfSha256(ikm, salt, info, 32)), toHex(expected))
})

test('AES-GCM: round trip, Web Crypto interop, tamper and wrong key refused', async () => {
  const key = crypto.getRandomValues(new Uint8Array(32))
  const iv = crypto.getRandomValues(new Uint8Array(12))
  const text = 'a value with ç and 🙂'
  const sealed = fb.aesGcmEncrypt(key, iv, utf8(text))
  assert.equal(sealed.length, utf8(text).length + 16)
  assert.equal(fb.aesGcmDecrypt(key, iv, sealed), text)

  const cryptoKey = await subtle.importKey('raw', key, 'AES-GCM', false, ['encrypt', 'decrypt'])
  const opened = await subtle.decrypt({ name: 'AES-GCM', iv }, cryptoKey, sealed)
  assert.equal(new TextDecoder().decode(opened), text)
  const bySubtle = new Uint8Array(await subtle.encrypt({ name: 'AES-GCM', iv }, cryptoKey, utf8(text)))
  assert.equal(fb.aesGcmDecrypt(key, iv, bySubtle), text)

  const tampered = sealed.slice()
  tampered[0] ^= 1
  assert.throws(() => fb.aesGcmDecrypt(key, iv, tampered))
  const tamperedTag = sealed.slice()
  tamperedTag[tamperedTag.length - 1] ^= 1
  assert.throws(() => fb.aesGcmDecrypt(key, iv, tamperedTag))
  assert.throws(() => fb.aesGcmDecrypt(crypto.getRandomValues(new Uint8Array(32)), iv, sealed))
})

let failed = 0
for (const [name, fn] of tests) {
  try {
    await fn()
    console.log('ok   ' + name)
  } catch (e) {
    failed++
    console.log('FAIL ' + name + '\n     ' + (e && e.message))
  }
}
console.log(`${tests.length - failed}/${tests.length} passed`)
process.exit(failed ? 1 : 0)
