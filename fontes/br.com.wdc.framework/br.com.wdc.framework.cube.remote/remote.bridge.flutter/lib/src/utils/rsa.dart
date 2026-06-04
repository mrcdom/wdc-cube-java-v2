import 'dart:convert';
import 'dart:typed_data';

import 'big_int_utils.dart';

/// Minimal RSA implementation using only the public key for encryption.
class RSA {
  final BigInt publicExponent;
  final BigInt publicKey;

  RSA({required this.publicExponent, required this.publicKey});

  /// Creates an RSA instance from the server-provided security key string.
  /// Format: "{exponent_base36}:{key_base36}"
  factory RSA.fromSecurityKey(String skey) {
    final parts = skey.split(':');
    if (parts.length != 2) {
      throw ArgumentError('Invalid security key format');
    }
    final publicExponent = BigIntUtils.parse(parts[0], 36);
    final publicKey = BigIntUtils.parse(parts[1], 36);
    return RSA(publicExponent: publicExponent, publicKey: publicKey);
  }

  /// Encrypts [message] using RSA public key: message^e mod n.
  BigInt encrypt(BigInt message) {
    return message.modPow(publicExponent, publicKey);
  }

  /// Encrypts a byte array and returns the result as a base-36 string.
  String encryptToBase36(Uint8List message) {
    final messageAsBase64 = base64Encode(message);
    final messageAsUtf8 = utf8.encode(messageAsBase64);
    final messageAsBigInt = BigIntUtils.fromBuffer(messageAsUtf8);
    final encrypted = encrypt(messageAsBigInt);
    return encrypted.toRadixString(36);
  }
}
