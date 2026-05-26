// :: Standard - RFC 2045 - Non URL safe

let getRfc2045Table = () => {
  const b64chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
  const lookup = new Map(Array.from(b64chars).map((char, i) => [char, i]))
  const response = { b64chars, lookup }
  getRfc2045Table = () => response
  return response
}

const base64Encode = (binData: Uint8Array) => {
  const { b64chars } = getRfc2045Table()
  let result = ''

  // Process 3 bytes at a time
  for (let i = 0; i < binData.length; i += 3) {
    const chunk =
      (binData[i] << 16) |
      (i + 1 < binData.length ? binData[i + 1] << 8 : 0) |
      (i + 2 < binData.length ? binData[i + 2] : 0)

    // Convert to 4 base64 characters
    result += b64chars[(chunk >> 18) & 63]
    result += b64chars[(chunk >> 12) & 63]
    result += i + 1 < binData.length ? b64chars[(chunk >> 6) & 63] : '='
    result += i + 2 < binData.length ? b64chars[chunk & 63] : '='
  }

  return result
}

const base64Decode = (str: string) => {
  const { lookup } = getRfc2045Table()

  // Remove padding if present
  const unpadded = str.replace(/=+$/, '')
  const binData = new Uint8Array(Math.floor((unpadded.length * 3) / 4))

  for (let i = 0, j = 0; i < unpadded.length; i += 4) {
    const chunk =
      (lookup.get(unpadded[i]) << 18) |
      (lookup.get(unpadded[i + 1]) << 12) |
      ((i + 2 < unpadded.length ? lookup.get(unpadded[i + 2]) : 0) << 6) |
      (i + 3 < unpadded.length ? lookup.get(unpadded[i + 3]) : 0)

    binData[j++] = (chunk >> 16) & 255
    if (i + 2 < unpadded.length) binData[j++] = (chunk >> 8) & 255
    if (i + 3 < unpadded.length) binData[j++] = chunk & 255
  }

  return binData
}

// :: RFC 4648 - Url Safe

let getRfc4648Table = () => {
  const b64chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_'
  const lookup = new Map(Array.from(b64chars).map((char, i) => [char, i]))
  const response = { b64chars, lookup }
  getRfc4648Table = () => response
  return response
}

const base64UrlEncode = (binData: Uint8Array) => {
  const { b64chars } = getRfc4648Table()

  let result = ''

  for (let i = 0; i < binData.length; i += 3) {
    const chunk =
      (binData[i] << 16) |
      (i + 1 < binData.length ? binData[i + 1] << 8 : 0) |
      (i + 2 < binData.length ? binData[i + 2] : 0)

    result += b64chars[(chunk >> 18) & 63]
    result += b64chars[(chunk >> 12) & 63]
    result += i + 1 < binData.length ? b64chars[(chunk >> 6) & 63] : ''
    result += i + 2 < binData.length ? b64chars[chunk & 63] : ''
  }

  return result
}

const base64UrlDecode = (str: string) => {
  const { lookup } = getRfc4648Table()

  // Pad to multiple of 4
  const padLen = str.length % 4
  const padded = str + '='.repeat(padLen ? 4 - padLen : 0)

  const binData = new Uint8Array(Math.floor((padded.length * 3) / 4))

  for (let i = 0, j = 0; i < padded.length; i += 4) {
    const chunk =
      (lookup.get(padded[i]) << 18) |
      (lookup.get(padded[i + 1]) << 12) |
      ((padded[i + 2] !== '=' ? lookup.get(padded[i + 2]) : 0) << 6) |
      (padded[i + 3] !== '=' ? lookup.get(padded[i + 3]) : 0)

    binData[j++] = (chunk >> 16) & 255
    if (padded[i + 2] !== '=') binData[j++] = (chunk >> 8) & 255
    if (padded[i + 3] !== '=') binData[j++] = chunk & 255
  }

  return binData
}

const Base64 = new (class {
  // RFC 2045 - URL unsafe

  encode(binData: Uint8Array): string {
    return base64Encode(binData)
  }

  decode(base64String: string): Uint8Array {
    return base64Decode(base64String)
  }

  // RFC 4648 - Url Safe

  encodeUrlSafe(binData: Uint8Array): string {
    return base64UrlEncode(binData)
  }

  decodeUrlSafe(base64Url: string): Uint8Array {
    return base64UrlDecode(base64Url)
  }
})()

export default Base64
