# 0024. Sign-in with SCRAM-SHA-256; failed sign-ins slow the account down

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

The sign-in was a challenge–response with HMAC, keyed by the MD5 of the password, and the database stored that same MD5 (base 36, unsalted). Whoever read the user table could sign in as any user without knowing a password, and an unsalted MD5 is cheap to reverse. Nothing slowed down guessing: a client could try passwords as fast as the server answered. The security architecture listed both as known gaps.

Fixing it also exposed that an access token was resolved by its **user**, not by its session: with two sessions of the same user, the server could pick the other session's RSA key, and a password a client encrypted for the user API was then stored as it came.

## Decision

**Sign-in follows SCRAM-SHA-256** (RFC 5802 and 7677), the scheme PostgreSQL uses:

1. `POST /api/auth/challenge {userName}` returns a single-use nonce bound to that user, the salt and the iteration count of the user's verifier.
2. The client derives the key with PBKDF2 and proves it knows the password without sending it: `POST /api/auth/login {userName, proof, nonce}` returns the tokens and the **server's signature**.
3. The client checks that signature before keeping the session: a server that does not hold the user's verifier is not the real one.

- **What is stored** is a verifier in PostgreSQL's encoding, `SCRAM-SHA-256$<iterations>:<salt>$<StoredKey>:<ServerKey>`, in `EN_USER.PASSWORD` (now `VARCHAR(255)`), with a random 16-byte salt and **4096 iterations** kept per verifier, so the count can be raised later. `PasswordVerifiers` is the only place that makes one: the seed, the user API and the conversion of older databases.
- **The password goes in as its MD5 digest** (`PasswordUtil.hashPassword`), the form stored before. That is what lets `DBCreate` turn each stored digest into the verifier of the same password, on H2 and PostgreSQL, without knowing any password; the digest adds nothing to the security and takes nothing from it.
- **`Scram` and `ScramVerifier` live in `framework.domain`** and compute through the `CryptoProvider` (which gained `sha256`), so the server, the JVM clients and the browser (teavm.web, with SHA-256 and HMAC in Java compiled by TeaVM) run the same code. They are tested against the RFC 7677 vector.
- **An unknown user** gets a made-up salt, stable for that name (derived from the JWT secret), and the server checks the proof against a decoy verifier: the answers and the work do not tell whether the user exists.
- **The user API** turns the password it receives into a verifier. With security on, that password must come encrypted with the session's RSA key; one that does not decrypt is refused (400), never stored as it came.
- **Failed sign-ins of one account make it wait** (`LoginThrottle`): five free failures, then 30 seconds doubling up to 15 minutes; a success clears the count, and failures older than an hour are forgotten. A wait, not a lock. It is counted in the database (`EN_LOGIN_FAILURE`: the login, the failures in a row, the last one in UTC), so attempts spread over several instances count as one; the wait is worked out when an attempt is checked, and the only write instances can race on is one `UPDATE` that adds one. It applies whichever way the sign-in comes in — the REST API or a view whose presenters run on the server.
- **`/api/auth` is limited per client address** (`AuthRateLimit`, `[security] auth.requestsPerMinute`, default 60, `0` = no limit): a fixed window per minute, in each instance's memory.
- **Both limits answer 429** (`TooManyRequestsException`, with `Retry-After` and `retryAfter` in the body); the REST client throws the same exception back, and the login screen says how long to wait.
- **An access token carries its session** (`sid`), and the server resolves it to that session: each session has its own RSA key, and the persistent "remember me" token, which has no session, does not open the API.

## Consequences

- Reading the database no longer lets anyone sign in; guessing a password costs the full PBKDF2 work per guess, and online guessing is slowed per account and per address.
- Existing databases convert on the first start, and every user keeps signing in with the same password.
- Every client derives the key on the user's device: 4096 iterations took about 15 ms in the browser (teavm.web, and the API explorer on Web Crypto) and less on the JVM.
- Someone who knows a login can make its owner wait, never lock them out.
- With N instances, the per-address limit is up to N times the configured one; behind a proxy every client is the proxy's address, so deployments limit per address at the proxy.
- The client of `/api/auth` changed: `challenge` is a `POST` with the user, and `login` takes a proof instead of a digest.

## Alternatives considered

- **A salted slow hash checked on the server (bcrypt, Argon2) with the password sent over TLS:** simpler, but the password reaches the server, and the presenters of the remote hosts would handle it in the clear.
- **OPAQUE:** stronger against a server compromise, but it needs elliptic-curve primitives that the browser client (compiled by TeaVM) does not have synchronously, and no standard Java implementation.
- **The per-account count in memory:** with several instances each would count its own, multiplying the free attempts.
