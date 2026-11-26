# 0023. The remote channel: a key agreed per connection, HMAC signatures, a versioned protocol

- **Date:** 2026-11-26
- **Status:** Accepted; supersedes the paragraph on the security of the channel of [0014](0014-server-driven-remote-views.md)

## Context

The remote channel ([0014](0014-server-driven-remote-views.md)) ciphers some values on top of TLS — the password typed in the login form, the client storage — and signs session ids and the addresses it returns for the history. The scheme it had was weak on every count:

- the client sent a password ciphered with **textbook RSA** (no padding) under a 512-bit key, delivered in a cookie;
- the AES-GCM key came from that password, and **one IV served the whole session**: whoever sees two ciphered values learns their XOR and can forge authentication tags;
- signatures were RSA-512 reduced by MD5;
- nothing told the host which version of the messages a client spoke, so an old page or app simply failed.

## Decision

**Protocol version 1** ([bridge-skeleton-protocol.md §6](../bridge-skeleton-protocol.md#6-security-handshake--the-connection-key-protocol-version-1)):

- **A key per connection.** The first message of every WebSocket connection is the handshake: `{"protocol": 1, "clientKey"}` with an ephemeral ECDH P-256 public key; the host answers `{"protocol": 1, "serverKey"}`. Both derive an AES-256 key with HKDF-SHA256, the session id as salt and `wdc-cube-remote/aes-256-gcm/v1` as info. Nothing else goes out before the answer.
- **An IV per value.** A ciphered value is `Base64(IV ‖ ciphertext ‖ tag)`, with a random 12-byte IV of its own.
- **HMAC signatures.** Session ids and addresses are signed with HMAC-SHA256 (16 bytes, Base62) under `[remote] signingKey`, generated into the local configuration on the first start, and verified in constant time.
- **A versioned handshake.** A first message without version 1 is answered `{"error": "unsupported-protocol", "protocols": [1]}` and closed with `4002`; the client does not reconnect and tells the user it is out of date.
- **Conformance fixtures**, language-neutral, in `framework.cube.remote/conformance`: the host (`RemoteProtocolConformanceTest`) and the Java, React and Flutter clients run them in their tests.

Every end changes together: the host, the Java bridge (SWT shell, probe), the React, TeaVM and Flutter bridges, and the Codename One shell. Codename One has no ECDH or HKDF, so its shell carries a small P-256 implementation of its own, cross-checked against the JVM's in its tests. The browser clients keep a JavaScript implementation of the primitives for plain HTTP, where Web Crypto is not available.

## Consequences

- Forward secrecy: a recorded connection cannot be deciphered later, even with the server's secrets; the server holds no ciphering key at all.
- A reconnection agrees on a new key; the storage bootstrap goes ciphered with it.
- The signing key decides which sessions and addresses a host accepts: instances that share sessions share it, and it must be kept secret.
- A breaking change of the wire for any client of the previous scheme, which is refused with a message instead of failing.
- Scopes and events are still not ciphered by the protocol: their confidentiality rests on TLS.

## Alternatives considered

- **RSA-OAEP with a server key.** Fixes the padding, but keeps a long-lived key to manage and gives no forward secrecy.
- **Rely on TLS alone.** TLS is assumed; the extra layer keeps sensitive form values out of logs and of proxies that terminate TLS.
- **Require a secure context in the browser** instead of a JavaScript fallback: less code, but the store could no longer be opened by address on a local network over plain HTTP.
