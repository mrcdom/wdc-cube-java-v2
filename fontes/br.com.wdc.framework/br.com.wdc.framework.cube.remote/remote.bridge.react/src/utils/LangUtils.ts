export function deleteProperties(objectToClean: object) {
  const target = objectToClean as Record<string | symbol, unknown>
  for (const x in objectToClean) {
    if (target.hasOwnProperty(x)) {
      delete target[x]
    }
  }
}

export function makeUniqueId(): string {
  if (typeof crypto.randomUUID === "function") {
    return crypto.randomUUID()
  }
  // Fallback para contextos não-seguros (HTTP puro): UUID v4 via getRandomValues
  const b = crypto.getRandomValues(new Uint8Array(16))
  b[6] = (b[6] & 0x0f) | 0x40 // version 4
  b[8] = (b[8] & 0x3f) | 0x80 // variant bits
  const h = Array.from(b, (x) => x.toString(16).padStart(2, "0")).join("")
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`
}
