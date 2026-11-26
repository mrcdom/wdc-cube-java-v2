# Remote protocol: conformance fixtures

Language-neutral fixtures that a host or a client of the remote protocol runs in its tests, to show it speaks **version 1**. The protocol itself is specified in [docs/bridge-skeleton-protocol.md](../../../../docs/bridge-skeleton-protocol.md); the fixtures cover what changed with the version: the handshake that agrees on each connection's key (§6), the refusal of another version, and the ciphered values.

Who runs them:

| End | Runner |
|---|---|
| Host (`remote.backend`, with the WDC Store application) | `RemoteProtocolConformanceTest` in `br.com.wdc.store.tests` |
| Java bridge (`remote.bridge.java`) | `ClientConformanceTest` in `remote.bridge.java` |
| React bridge (`remote.bridge.react`) | `conformance.test.ts` (Vitest) |
| Flutter bridge (`remote.bridge.flutter`) | `conformance_test.dart` |

Each file is `{"description": "…", "steps": [ … ]}`, run in order, on a new session.

## Host fixtures (`host/`)

The runner opens a session (`GET /api/session/init`), a WebSocket to `/dispatcher/<appId>`, and runs the steps:

| Step | What the runner does |
|---|---|
| `{"send": {…}, "expect": {…}}` | Sends the message; the next message from the host must match `expect`. |
| `{"send": {…}, "expectNothing": true}` | Sends the message; no message may come (for a short while). |
| `{"expectClosed": <code>}` | The host closes the connection with that code. |

In what is sent, `"$clientKey"` stands for the public key of a new P-256 key pair the runner makes (Base64 SubjectPublicKeyInfo).

## Client fixtures (`client/`)

The runner starts the client against a fake WebSocket it controls (or a scripted host), and runs the steps:

| Step | What the runner does |
|---|---|
| `{"start": {}}` | Starts the client; its connection opens. |
| `{"expectSent": {…}}` | The next message the client sends must match. Keep-alives (`{"ping": true}`) are left out. |
| `{"expectNothingSent": true}` | The client sends nothing (other than keep-alives) for a while. |
| `{"receive": {…}}` | The host's message arrives. |
| `{"expectOutdated": true}` | The client knows the host refused its version, and will not reconnect. |
| `{"expectStorage": {"scope", "key", "value"}}` | The client's storage of that scope (`session`, `persistent`, `persistent-secure`) holds that value. |

In what the client receives:

- `"$serverKey"` stands for the public key of a new P-256 key pair the runner makes. From then on the runner knows the connection key: it derives it from its private key, the client's `clientKey` and the session id, as the protocol says (§6.2).
- `"$cipher:<text>"` stands for `<text>` ciphered with that connection key (§6.3), with a random IV.

## Matching

- An expected **object** matches when each of its fields matches the actual one; other actual fields are allowed. `"$absent"` as a value means the field must not be there.
- `"$any"` matches any present value; `"$string"` and `"$number"`, any value of that type.
- **Arrays** match element by element, with the same length.
- `{"$contains": {…}}` matches an array that has at least one element matching the object.
