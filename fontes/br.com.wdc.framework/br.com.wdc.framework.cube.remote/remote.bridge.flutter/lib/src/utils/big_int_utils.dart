import 'dart:convert';
import 'dart:typed_data';

/// BigInt utility functions for RSA operations.
class BigIntUtils {
  BigIntUtils._();

  /// Converts a BigInt to a Uint8List (big-endian, unsigned).
  static Uint8List toBuffer(BigInt value) {
    if (value < BigInt.zero) {
      throw RangeError('value must be non-negative');
    }
    if (value == BigInt.zero) return Uint8List.fromList([0]);

    final hex = value.toRadixString(16);
    final padded = hex.length.isOdd ? '0$hex' : hex;
    final bytes = Uint8List(padded.length ~/ 2);
    for (var i = 0; i < bytes.length; i++) {
      bytes[i] = int.parse(padded.substring(i * 2, i * 2 + 2), radix: 16);
    }
    return bytes;
  }

  /// Converts a Uint8List (big-endian, unsigned) to a BigInt.
  static BigInt fromBuffer(Uint8List buf) {
    var result = BigInt.zero;
    for (final byte in buf) {
      result = (result << 8) + BigInt.from(byte);
    }
    return result;
  }

  /// Parses a number string in the given [radix] (supports 36 and 64).
  /// For radix 64, interprets [numberString] as base64 (or URL-safe base64).
  static BigInt parse(String numberString, int radix, {bool urlSafe = false}) {
    if (radix == 36) {
      return _parseBigInt(numberString);
    }
    if (radix == 64) {
      final bytes = _decodeBase64(numberString, urlSafe: urlSafe);
      return fromBuffer(bytes);
    }
    throw ArgumentError('Unsupported radix: $radix');
  }

  static final _base36 = BigInt.from(36);

  static BigInt _parseBigInt(String numberString) {
    var result = BigInt.zero;
    for (var i = 0; i < numberString.length; i++) {
      final c = numberString.codeUnitAt(i);
      int value;
      if (c >= 0x30 && c <= 0x39) {
        value = c - 0x30; // '0'-'9'
      } else if (c >= 0x61 && c <= 0x7A) {
        value = c - 0x61 + 10; // 'a'-'z'
      } else {
        throw FormatException('Invalid character: ${numberString[i]}');
      }
      result = result * _base36 + BigInt.from(value);
    }
    return result;
  }

  /// Decodes base64/base64url using dart:convert.
  static Uint8List _decodeBase64(String input, {bool urlSafe = false}) {
    if (urlSafe) {
      return base64Url.decode(base64Url.normalize(input));
    }
    // Standard base64 — ensure proper padding
    var normalized = input;
    final remainder = normalized.length % 4;
    if (remainder == 2) {
      normalized += '==';
    } else if (remainder == 3) {
      normalized += '=';
    }
    return base64Decode(normalized);
  }
}
