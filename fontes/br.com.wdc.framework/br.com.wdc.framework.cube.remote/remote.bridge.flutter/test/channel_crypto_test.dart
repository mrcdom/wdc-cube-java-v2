import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:pointycastle/export.dart' show ECPrivateKey, ECCurve_secp256r1;
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

Uint8List _hex(String hex) => Uint8List.fromList([
  for (var i = 0; i < hex.length; i += 2)
    int.parse(hex.substring(i, i + 2), radix: 16),
]);

String _toHex(List<int> bytes) =>
    bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();

void main() {
  final key = ChannelCrypto.randomBytes(32);

  test('the same text ciphered twice differs, and both decipher', () {
    final first = ChannelCrypto.cipher(key, 'secret ç 🙂');
    final second = ChannelCrypto.cipher(key, 'secret ç 🙂');
    expect(first, isNot(second));
    expect(ChannelCrypto.decipher(key, first), 'secret ç 🙂');
    expect(ChannelCrypto.decipher(key, second), 'secret ç 🙂');
    // IV(12) ‖ ciphertext ‖ tag(16)
    expect(
      base64Decode(first).length,
      12 + utf8.encode('secret ç 🙂').length + 16,
    );
  });

  test('a tampered value is refused', () {
    final bytes = base64Decode(ChannelCrypto.cipher(key, 'secret'));
    for (final index in [0, 12, bytes.length - 1]) {
      final tampered = Uint8List.fromList(bytes);
      tampered[index] ^= 0x01;
      expect(
        () => ChannelCrypto.decipher(key, base64Encode(tampered)),
        throwsArgumentError,
      );
    }
  });

  test('a value ciphered with another key is refused', () {
    final value = ChannelCrypto.cipher(key, 'secret');
    expect(
      () => ChannelCrypto.decipher(ChannelCrypto.randomBytes(32), value),
      throwsArgumentError,
    );
  });

  test('a public key goes through SubjectPublicKeyInfo and back', () {
    final pair = ChannelCrypto.newKeyPair();
    final encoded = ChannelCrypto.encodePublicKey(pair.publicKey);
    final bytes = base64Decode(encoded);
    expect(bytes.length, 91);
    expect(
      _toHex(bytes.sublist(0, 27)),
      '3059301306072a8648ce3d020106082a8648ce3d03010703420004',
    );
    final decoded = ChannelCrypto.decodePublicKey(encoded);
    expect(decoded.Q, pair.publicKey.Q);
    expect(ChannelCrypto.encodePublicKey(decoded), encoded);
  });

  test('a key that is not a P-256 point is refused', () {
    final bytes = base64Decode(
      ChannelCrypto.encodePublicKey(ChannelCrypto.newKeyPair().publicKey),
    );
    final offCurve = Uint8List.fromList(bytes)..[bytes.length - 1] ^= 0x01;
    expect(
      () => ChannelCrypto.decodePublicKey(base64Encode(offCurve)),
      throwsArgumentError,
    );
    final otherCurve = Uint8List.fromList(bytes)..[20] ^= 0x01;
    expect(
      () => ChannelCrypto.decodePublicKey(base64Encode(otherCurve)),
      throwsArgumentError,
    );
    expect(
      () => ChannelCrypto.decodePublicKey('not base64!'),
      throwsArgumentError,
    );
    expect(
      () => ChannelCrypto.decodePublicKey(base64Encode(bytes.sublist(0, 60))),
      throwsArgumentError,
    );
  });

  test('both ends of an ECDH derive the same key', () {
    final client = ChannelCrypto.newKeyPair();
    final server = ChannelCrypto.newKeyPair();
    const appId = 'abc.def';
    final clientKey = ChannelCrypto.deriveKey(
      client.privateKey,
      ChannelCrypto.decodePublicKey(
        ChannelCrypto.encodePublicKey(server.publicKey),
      ),
      appId,
    );
    final serverKey = ChannelCrypto.deriveKey(
      server.privateKey,
      ChannelCrypto.decodePublicKey(
        ChannelCrypto.encodePublicKey(client.publicKey),
      ),
      appId,
    );
    expect(clientKey.length, 32);
    expect(clientKey, serverKey);
    expect(
      ChannelCrypto.deriveKey(client.privateKey, server.publicKey, 'other.id'),
      isNot(clientKey),
    );
  });

  test('DataSecurity agrees on a key with its peer', () {
    final security = DataSecurity();
    expect(security.isReady, isFalse);
    final clientKey = security.beginHandshake();
    final server = ChannelCrypto.newKeyPair();
    security.completeHandshake(
      ChannelCrypto.encodePublicKey(server.publicKey),
      'abc.def',
    );
    expect(security.isReady, isTrue);
    final serverSide = ChannelCrypto.deriveKey(
      server.privateKey,
      ChannelCrypto.decodePublicKey(clientKey),
      'abc.def',
    );
    expect(ChannelCrypto.decipher(serverSide, security.b64Cipher('hi')), 'hi');
    security.reset();
    expect(security.isReady, isFalse);
    expect(() => security.b64Cipher('hi'), throwsStateError);
  });

  test(
    'the key and a ciphered value agree with the JVM end (ChannelCrypto.java)',
    () {
      // Made by the JDK: the client's private scalar, the host's public key, the
      // key the JDK derived for session 'app.id', and 'olá 🙂' ciphered with it.
      final own = ECPrivateKey(
        BigInt.parse(
          '18ff4cb45111827f6fc81937a2af08272d2ab402f7fcbe5ffa0b50a1271a5b78',
          radix: 16,
        ),
        ECCurve_secp256r1(),
      );
      final key = ChannelCrypto.deriveKey(
        own,
        ChannelCrypto.decodePublicKey(
          'MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUeoBtjym6V3CClskA6IxJZjBvXizgeXbzHJgiKRqews5rkxZl0qcJsA65KWnNDoKmI1SMZyGu/R67mv+TQD9Ag==',
        ),
        'app.id',
      );
      expect(
        _toHex(key),
        '6677ccebc0bf15e882bb7e3b4e1641032b62ba5b4f6ea2d83d00581273e677d8',
      );
      expect(
        ChannelCrypto.decipher(
          key,
          '4I4qti9keSk7Uky9b3goP32zQwJ88Ee9fEqpWUN9noqHomyGbg==',
        ),
        'olá 🙂',
      );
    },
  );

  test('HKDF-SHA256 matches RFC 5869, test case 1', () {
    final okm = ChannelCrypto.hkdfSha256(
      Uint8List.fromList(List.filled(22, 0x0b)),
      _hex('000102030405060708090a0b0c'),
      _hex('f0f1f2f3f4f5f6f7f8f9'),
      42,
    );
    expect(
      _toHex(okm),
      '3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865',
    );
  });
}
