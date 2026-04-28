export const NOOP_VOID = () => void 0 as void
export const NOOP_PROMISE_VOID = async () => void 0 as void

export const CAUTHED = (reason: unknown) => {
  console.error('Unexpected', reason)
}

export const BROWSER_VID = '7b32e816a191'
export const BROWSER_VSID = `${BROWSER_VID}:0`

// Keepalive interval to prevent "Idle Timeout" errors
// Reduced to 15 seconds to ensure WebSocket stays active while page is open
export const KEEP_ALIVE_INTERVAL = 15 * 1000
