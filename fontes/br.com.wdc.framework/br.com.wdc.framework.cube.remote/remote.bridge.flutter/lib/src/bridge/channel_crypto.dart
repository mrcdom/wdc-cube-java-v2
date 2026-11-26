import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';

import 'package:pointycastle/export.dart';

/// The cryptography of the remote channel, protocol version 1
/// (`docs/bridge-skeleton-protocol.md`, section 6).
///
/// - **Key agreement**, per connection: each end makes an ephemeral ECDH P-256
///   key pair ([newKeyPair]) and sends its public key as Base64 of the X.509
///   SubjectPublicKeyInfo ([encodePublicKey]).
/// - **Connection key**: HKDF-SHA256 over the ECDH shared secret (the 32-byte
///   x-coordinate), with the session id (UTF-8) as salt and [keyInfo] as info,
///   32 bytes, used as an AES-256 key ([deriveKey]).
/// - **A ciphered value**: AES-256-GCM with a 128-bit tag and a random 12-byte
///   IV of its own, written as `Base64(IV ‖ ciphertext ‖ tag)` ([cipher],
///   [decipher]).
class ChannelCrypto {
  ChannelCrypto._();

  /// The HKDF info that names what the derived key is for.
  static const String keyInfo = 'wdc-cube-remote/aes-256-gcm/v1';

  static const int _ivLength = 12;
  static const int _tagBits = 128;
  static const int _keyLength = 32;
  static const int _coordinateLength = 32;

  /// The DER of a P-256 SubjectPublicKeyInfo up to the uncompressed point:
  /// SEQUENCE { SEQUENCE { id-ecPublicKey, prime256v1 }, BIT STRING (0 unused bits) }.
  static final Uint8List _spkiPrefix = _hex(
    '3059301306072a8648ce3d020106082a8648ce3d030107034200',
  );

  // P-256 (FIPS 186-4, D.1.2.3): y² = x³ - 3x + b over GF(p).
  static final BigInt _p = BigInt.parse(
    'ffffffff00000001000000000000000000000000ffffffffffffffffffffffff',
    radix: 16,
  );
  static final BigInt _b = BigInt.parse(
    '5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b',
    radix: 16,
  );

  static final ECDomainParameters _domain = ECCurve_secp256r1();
  static final Random _random = Random.secure();

  /// A new ephemeral ECDH P-256 key pair.
  static AsymmetricKeyPair<ECPublicKey, ECPrivateKey> newKeyPair() {
    final seeded = FortunaRandom()..seed(KeyParameter(randomBytes(32)));
    final generator = ECKeyGenerator()
      ..init(ParametersWithRandom(ECKeyGeneratorParameters(_domain), seeded));
    final pair = generator.generateKeyPair();
    return AsymmetricKeyPair(
      pair.publicKey as ECPublicKey,
      pair.privateKey as ECPrivateKey,
    );
  }

  /// The public key as Base64 of its X.509 SubjectPublicKeyInfo.
  static String encodePublicKey(ECPublicKey key) {
    final q = key.Q!;
    final out = BytesBuilder(copy: false)
      ..add(_spkiPrefix)
      ..addByte(0x04)
      ..add(_unsigned(q.x!.toBigInteger()!, _coordinateLength))
      ..add(_unsigned(q.y!.toBigInteger()!, _coordinateLength));
    return base64Encode(out.toBytes());
  }

  /// Reads a peer's public key.
  ///
  /// Throws [ArgumentError] if it is not a P-256 public key in Base64
  /// SubjectPublicKeyInfo, with the point on the curve.
  static ECPublicKey decodePublicKey(String encoded) {
    final Uint8List bytes;
    try {
      bytes = base64Decode(encoded);
    } on FormatException catch (caught) {
      throw ArgumentError('Not a P-256 public key: $caught');
    }
    final expectedLength = _spkiPrefix.length + 1 + 2 * _coordinateLength;
    if (bytes.length != expectedLength) {
      throw ArgumentError('Not a P-256 public key: wrong length');
    }
    for (var i = 0; i < _spkiPrefix.length; i++) {
      if (bytes[i] != _spkiPrefix[i]) {
        throw ArgumentError('Not a P-256 public key: wrong algorithm');
      }
    }
    var offset = _spkiPrefix.length;
    if (bytes[offset++] != 0x04) {
      throw ArgumentError('Not a P-256 public key: not an uncompressed point');
    }
    final x = _bigInt(bytes.sublist(offset, offset + _coordinateLength));
    final y = _bigInt(bytes.sublist(offset + _coordinateLength));
    if (x >= _p || y >= _p || !_isOnCurve(x, y)) {
      throw ArgumentError(
        'Not a P-256 public key: the point is not on the curve',
      );
    }
    return ECPublicKey(_domain.curve.createPoint(x, y), _domain);
  }

  /// The connection key both ends derive: HKDF-SHA256 over the ECDH secret,
  /// salted with the session id.
  static Uint8List deriveKey(
    ECPrivateKey own,
    ECPublicKey peer,
    String sessionId,
  ) {
    final agreement = ECDHBasicAgreement()..init(own);
    final shared = _unsigned(
      agreement.calculateAgreement(peer),
      _coordinateLength,
    );
    try {
      return hkdfSha256(
        shared,
        Uint8List.fromList(utf8.encode(sessionId)),
        Uint8List.fromList(utf8.encode(keyInfo)),
        _keyLength,
      );
    } finally {
      shared.fillRange(0, shared.length, 0);
    }
  }

  /// HKDF with HMAC-SHA256 (RFC 5869): extract, then expand to [length] bytes.
  static Uint8List hkdfSha256(
    Uint8List ikm,
    Uint8List salt,
    Uint8List info,
    int length,
  ) {
    final derivator = HKDFKeyDerivator(SHA256Digest())
      ..init(HkdfParameters(ikm, length, salt, info));
    final out = Uint8List(length);
    derivator.deriveKey(null, 0, out, 0);
    return out;
  }

  /// Ciphers [text] with [key]: `Base64(IV ‖ ciphertext ‖ tag)`, with a random IV.
  static String cipher(Uint8List key, String text) {
    final iv = randomBytes(_ivLength);
    final plain = utf8.encode(text);
    final gcm = GCMBlockCipher(AESEngine())
      ..init(
        true,
        AEADParameters(KeyParameter(key), _tagBits, iv, Uint8List(0)),
      );
    final sealed = Uint8List(gcm.getOutputSize(plain.length));
    var length = gcm.processBytes(plain, 0, plain.length, sealed, 0);
    length += gcm.doFinal(sealed, length);
    final out = BytesBuilder(copy: false)
      ..add(iv)
      ..add(Uint8List.sublistView(sealed, 0, length));
    return base64Encode(out.toBytes());
  }

  /// Deciphers a value made by [cipher], at any end.
  ///
  /// Throws [ArgumentError] if it is not Base64, is too short, or fails
  /// authentication (tampered, or ciphered with another key).
  static String decipher(Uint8List key, String value) {
    final Uint8List bytes;
    try {
      bytes = base64Decode(value);
    } on FormatException catch (caught) {
      throw ArgumentError('A ciphered value is Base64: $caught');
    }
    if (bytes.length < _ivLength + _tagBits ~/ 8) {
      throw ArgumentError('A ciphered value is too short');
    }
    final iv = Uint8List.sublistView(bytes, 0, _ivLength);
    final sealed = Uint8List.sublistView(bytes, _ivLength);
    final gcm = GCMBlockCipher(AESEngine())
      ..init(
        false,
        AEADParameters(KeyParameter(key), _tagBits, iv, Uint8List(0)),
      );
    final plain = Uint8List(gcm.getOutputSize(sealed.length));
    try {
      var length = gcm.processBytes(sealed, 0, sealed.length, plain, 0);
      length += gcm.doFinal(plain, length);
      return utf8.decode(Uint8List.sublistView(plain, 0, length));
    } on InvalidCipherTextException catch (caught) {
      throw ArgumentError('A ciphered value did not authenticate: $caught');
    }
  }

  /// [length] bytes from the platform's cryptographic generator.
  static Uint8List randomBytes(int length) {
    final out = Uint8List(length);
    for (var i = 0; i < length; i++) {
      out[i] = _random.nextInt(256);
    }
    return out;
  }

  static bool _isOnCurve(BigInt x, BigInt y) {
    final left = (y * y) % _p;
    final right = (x * x * x - BigInt.from(3) * x + _b) % _p;
    return left == right;
  }

  /// [value] as a big-endian unsigned integer of exactly [length] bytes.
  static Uint8List _unsigned(BigInt value, int length) {
    final out = Uint8List(length);
    var rest = value;
    for (var i = length - 1; i >= 0; i--) {
      out[i] = (rest & BigInt.from(0xff)).toInt();
      rest = rest >> 8;
    }
    if (rest != BigInt.zero) {
      throw ArgumentError('The value does not fit in $length bytes');
    }
    return out;
  }

  static BigInt _bigInt(List<int> bytes) {
    var result = BigInt.zero;
    for (final b in bytes) {
      result = (result << 8) | BigInt.from(b);
    }
    return result;
  }

  static Uint8List _hex(String hex) {
    final out = Uint8List(hex.length ~/ 2);
    for (var i = 0; i < out.length; i++) {
      out[i] = int.parse(hex.substring(2 * i, 2 * i + 2), radix: 16);
    }
    return out;
  }
}
