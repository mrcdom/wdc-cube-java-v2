import 'dart:typed_data';

import 'package:pointycastle/export.dart';

import 'channel_crypto.dart';

/// The key of the current WebSocket connection, agreed by the handshake of
/// protocol version 1 (`docs/bridge-skeleton-protocol.md`, section 6).
///
/// Each connection starts with [beginHandshake] (a new ephemeral key pair,
/// whose public half goes in the client's first message) and, on the host's
/// answer, [completeHandshake] derives the connection key. Until then
/// [isReady] is false and nothing can be ciphered.
class DataSecurity {
  /// The version of the remote protocol this client speaks.
  static const int protocol = 1;

  ECPrivateKey? _ownKey;
  Uint8List? _key;

  /// Starts the handshake of a new connection and returns the client's public
  /// key (Base64 SubjectPublicKeyInfo). Forgets the previous connection's key.
  String beginHandshake() {
    final pair = ChannelCrypto.newKeyPair();
    _ownKey = pair.privateKey;
    _key = null;
    return ChannelCrypto.encodePublicKey(pair.publicKey);
  }

  /// Derives the connection key from the host's public key ([serverKey],
  /// Base64 SubjectPublicKeyInfo) and the session id ([appId]).
  ///
  /// Throws [ArgumentError] if [serverKey] is not a P-256 public key, and
  /// [StateError] if no handshake was begun.
  void completeHandshake(String serverKey, String appId) {
    final own = _ownKey;
    if (own == null) {
      throw StateError('No handshake was begun');
    }
    _key = ChannelCrypto.deriveKey(
      own,
      ChannelCrypto.decodePublicKey(serverKey),
      appId,
    );
    _ownKey = null;
  }

  /// Forgets the connection key (the connection is gone).
  void reset() {
    _ownKey = null;
    _key = null;
  }

  /// Whether the connection key is agreed and values can be ciphered.
  bool get isReady => _key != null;

  /// Ciphers [text] with the connection key, with a random IV of its own.
  String b64Cipher(String text) => ChannelCrypto.cipher(_requireKey(), text);

  /// Deciphers a value ciphered with the connection key.
  ///
  /// Throws [ArgumentError] if it does not authenticate.
  String b64Decipher(String value) =>
      ChannelCrypto.decipher(_requireKey(), value);

  Uint8List _requireKey() {
    final key = _key;
    if (key == null) {
      throw StateError('The connection key is not agreed yet');
    }
    return key;
  }
}
