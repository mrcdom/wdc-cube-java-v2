const encoder = new TextEncoder()
const decoder = new TextDecoder()

const UTF8 = new (class UTF8 {
  encode(input?: string): Uint8Array {
    return encoder.encode(input)
  }

  decode(input?: AllowSharedBufferSource, options?: TextDecodeOptions): string {
    return decoder.decode(input, options)
  }
})()

export default UTF8
