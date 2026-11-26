/**
 * The cryptography of the sign-in: SCRAM-SHA-256 (RFC 5802, RFC 7677), over the password's MD5 digest in the format
 * the server's `PasswordUtil.hashPassword` gives.
 *
 * The server never receives the password, and stores only a verifier. At sign-in it sends a nonce, the salt and the
 * iteration count; the client derives the key with PBKDF2, sends a proof that it knows the password, and checks the
 * server's signature — only who holds the user's verifier can compute it. A proof is good for one nonce, used once.
 */

const MD5_K = Int32Array.from({ length: 64 }, (_, i) => Math.floor(Math.abs(Math.sin(i + 1)) * 2 ** 32));

const MD5_S = [
  7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
  5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
  4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
  6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
];

const rotl = (x, n) => ((x << n) | (x >>> (32 - n))) >>> 0;
const add = (...xs) => xs.reduce((a, b) => (a + b) >>> 0, 0);

/** MD5 (RFC 1321) — returns the 16 bytes of the digest. */
export function md5(text) {
  const utf8 = new TextEncoder().encode(text);
  const bitLen = utf8.length * 8;
  const totalLen = (((utf8.length + 8) >>> 6) + 1) << 6;

  const buf = new Uint8Array(totalLen);
  buf.set(utf8);
  buf[utf8.length] = 0x80;

  const view = new DataView(buf.buffer);
  view.setUint32(totalLen - 8, bitLen >>> 0, true);
  view.setUint32(totalLen - 4, Math.floor(bitLen / 2 ** 32), true);

  let [a0, b0, c0, d0] = [0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476];

  for (let offset = 0; offset < totalLen; offset += 64) {
    const m = new Uint32Array(16);
    for (let j = 0; j < 16; j++) m[j] = view.getUint32(offset + j * 4, true);

    let [a, b, c, d] = [a0, b0, c0, d0];
    for (let i = 0; i < 64; i++) {
      let f;
      let g;
      if (i < 16) { f = (b & c) | (~b & d); g = i; }
      else if (i < 32) { f = (d & b) | (~d & c); g = (5 * i + 1) % 16; }
      else if (i < 48) { f = b ^ c ^ d; g = (3 * i + 5) % 16; }
      else { f = c ^ (b | ~d); g = (7 * i) % 16; }

      const temp = d;
      d = c;
      c = b;
      b = add(b, rotl(add(a, f >>> 0, MD5_K[i], m[g]), MD5_S[i]));
      a = temp;
    }
    a0 = add(a0, a); b0 = add(b0, b); c0 = add(c0, c); d0 = add(d0, d);
  }

  const out = new Uint8Array(16);
  const dv = new DataView(out.buffer);
  dv.setUint32(0, a0, true);
  dv.setUint32(4, b0, true);
  dv.setUint32(8, c0, true);
  dv.setUint32(12, d0, true);
  return out;
}

/**
 * The bytes as Java's `BigInteger` reads them: **signed**, in two's complement.
 *
 * The server stores the password as `new BigInteger(md5).toString(36)`, and that constructor is signed — when the
 * first byte is ≥ 0x80 the number is negative, and the text starts with `-`. Reading the same bytes as if they were
 * always positive produces a different string, and the login fails for about half of all passwords.
 */
export function bytesToSignedBase36(bytes) {
  const negative = bytes[0] >= 0x80;

  let magnitude = 0n;
  if (negative) {
    // Two's complement: invert the bits and add 1 to get the magnitude.
    const inverted = bytes.map((b) => ~b & 0xff);
    for (const b of inverted) magnitude = (magnitude << 8n) | BigInt(b);
    magnitude += 1n;
  } else {
    for (const b of bytes) magnitude = (magnitude << 8n) | BigInt(b);
  }

  return (negative ? '-' : '') + magnitude.toString(36);
}

/** The password digest in the format the server stores. */
export const passwordDigest = (password) => bytesToSignedBase36(md5(password));

const encoder = new TextEncoder();

const fromBase64 = (text) => Uint8Array.from(atob(text), (c) => c.charCodeAt(0));
const toBase64 = (bytes) => btoa(String.fromCharCode(...bytes));

/** HMAC-SHA256 of bytes, via Web Crypto. */
async function hmac(key, data) {
  const cryptoKey = await crypto.subtle.importKey('raw', key, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return new Uint8Array(await crypto.subtle.sign('HMAC', cryptoKey, data));
}

/**
 * The client's side of the SCRAM-SHA-256 exchange, for the challenge the server sent: the `proof` to send and the
 * server signature to expect back (`expectedServerSignature`), both in Base64.
 */
export async function scramResponse(userName, password, { nonce, salt, iterations }) {
  const base = await crypto.subtle.importKey('raw', encoder.encode(passwordDigest(password)), 'PBKDF2', false, [
    'deriveBits',
  ]);
  const salted = new Uint8Array(
    await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt: fromBase64(salt), iterations }, base, 256),
  );
  const authMessage = encoder.encode(`n=${userName},r=${nonce}`);

  const clientKey = await hmac(salted, encoder.encode('Client Key'));
  const storedKey = new Uint8Array(await crypto.subtle.digest('SHA-256', clientKey));
  const signature = await hmac(storedKey, authMessage);
  const proof = clientKey.map((b, i) => b ^ signature[i]);

  const serverKey = await hmac(salted, encoder.encode('Server Key'));
  const expectedServerSignature = await hmac(serverKey, authMessage);

  return { proof: toBase64(proof), expectedServerSignature: toBase64(expectedServerSignature) };
}
