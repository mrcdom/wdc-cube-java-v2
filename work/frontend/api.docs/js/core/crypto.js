/**
 * A criptografia que o login exige: MD5 da senha no mesmo formato do servidor, e HMAC-SHA256 do desafio.
 *
 * O servidor nunca recebe a senha. Ele guarda o resumo MD5 dela e, no login, manda um nonce; o cliente responde com
 * `HMAC-SHA256(resumo, usuário + nonce)`. Quem interceptar a resposta não consegue reusá-la — o nonce só vale uma vez.
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

/** MD5 (RFC 1321) — devolve os 16 bytes do resumo. */
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
 * Os bytes como o `BigInteger` do Java os lê: **com sinal**, em complemento de dois.
 *
 * O servidor guarda a senha como `new BigInteger(md5).toString(36)`, e esse construtor é com sinal — quando o primeiro
 * byte é ≥ 0x80 o número é negativo, e o texto começa com `-`. Ler os mesmos bytes como se fossem sempre positivos
 * produz outra string, e o login falha para cerca de metade das senhas. Foi o que acontecia aqui antes: o usuário
 * `beotrano` do próprio seed não conseguia entrar.
 */
export function bytesToSignedBase36(bytes) {
  const negative = bytes[0] >= 0x80;

  let magnitude = 0n;
  if (negative) {
    // Complemento de dois: inverte os bits e soma 1 para obter o módulo.
    const inverted = bytes.map((b) => ~b & 0xff);
    for (const b of inverted) magnitude = (magnitude << 8n) | BigInt(b);
    magnitude += 1n;
  } else {
    for (const b of bytes) magnitude = (magnitude << 8n) | BigInt(b);
  }

  return (negative ? '-' : '') + magnitude.toString(36);
}

/** O resumo da senha no formato que o servidor guarda. */
export const passwordDigest = (password) => bytesToSignedBase36(md5(password));

/** HMAC-SHA256 em hexadecimal, via Web Crypto. */
export async function hmacSha256(key, data) {
  const encoder = new TextEncoder();
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    encoder.encode(key),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign'],
  );
  const signature = await crypto.subtle.sign('HMAC', cryptoKey, encoder.encode(data));
  return Array.from(new Uint8Array(signature), (b) => b.toString(16).padStart(2, '0')).join('');
}
