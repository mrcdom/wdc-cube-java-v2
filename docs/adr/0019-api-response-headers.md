# 0019. Security headers, cache and Server-Timing on every API response

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

The REST API answers with user data, and its responses carried no common headers. Nothing stopped a browser from sniffing a type, a page from framing the API, an address from leaking in a `Referer`, or a proxy or browser from caching a user's answer. Nor was there a way to tell, from the browser, how much of a slow request was spent on the server.

## Decision

One `after` handler on `/api/*`, `ApiResponseHeaders` (`persistence.rest`, registered by `RepositoryApiRoutes`), sets on every response — errors and refusals included:

| Header | Value |
|---|---|
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `no-referrer` |
| `Cache-Control` | `no-store`, unless the route already set one |
| `Server-Timing` | `total;dur=<ms>`, measured from a `before` handler on the same path |

## Consequences

- A new route gets the headers without doing anything; a route that may be cached sets its own `Cache-Control`, which is kept.
- The browser's developer tools show the server time next to the network time.
- `ApiResponseHeadersTest` checks the headers.

See [data-layer.md → Response headers](../data-layer.md#response-headers).
