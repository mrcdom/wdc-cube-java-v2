import 'dart:convert';
import 'dart:math';
import 'dart:typed_data';

import 'package:pointycastle/export.dart';

import '../utils/rsa.dart';

/// Handles RSA encryption of password + PBKDF2 key derivation + AES-GCM cipher.
class DataSecurity {
  late Uint8List _iv;
  late Uint8List _aesKey;
  String _signature = '';
  RSA? _rsa;

  static final _secureRandom = Random.secure();

  void updateSecurityKey(String appSKey) {
    _rsa = RSA.fromSecurityKey(appSKey);
  }

  void updateSecretWithRandomPassword() {
    final pwd = base64Url.encode(_randomBytes(12)).replaceAll('=', '');
    final pwdBytes = utf8.encode(pwd);
    updateSecret(pwdBytes);
  }

  void updateSecret(Uint8List password) {
    final salt = _randomBytes(16);
    _iv = _randomBytes(12);

    // Derive AES-256 key from password using PBKDF2-SHA256
    final derivator = PBKDF2KeyDerivator(HMac(SHA256Digest(), 64))
      ..init(Pbkdf2Parameters(salt, 250000, 32));
    _aesKey = derivator.process(password);

    // RSA-encrypt the password
    final cryptedPwd = _rsa!.encryptToBase36(password);

    // Compose signature: {encryptedPwd}.{salt_b64url}.{iv_b64url}
    final saltB64 = base64Url.encode(salt).replaceAll('=', '');
    final ivB64 = base64Url.encode(_iv).replaceAll('=', '');
    _signature = '$cryptedPwd.$saltB64.$ivB64';
  }

  String getSignature() => _signature;

  /// Whether the security context is initialized and ready for cipher operations.
  bool get isReady => _signature.isNotEmpty;

  /// Encrypts [text] with AES-256-GCM and returns base64-encoded ciphertext.
  String b64Cipher(String text) {
    final plainBytes = utf8.encode(text);
    final cipher = GCMBlockCipher(AESEngine())
      ..init(true, AEADParameters(KeyParameter(_aesKey), 128, _iv, Uint8List(0)));

    final output = Uint8List(cipher.getOutputSize(plainBytes.length));
    var len = cipher.processBytes(plainBytes, 0, plainBytes.length, output, 0);
    len += cipher.doFinal(output, len);
    return base64Encode(Uint8List.view(output.buffer, 0, len));
  }

  /// Decrypts a base64-encoded AES-256-GCM ciphertext back to a string.
  String b64Decipher(String b64CipheredText) {
    final cipherBytes = base64Decode(b64CipheredText);
    final cipher = GCMBlockCipher(AESEngine())
      ..init(false, AEADParameters(KeyParameter(_aesKey), 128, _iv, Uint8List(0)));

    final plainText = Uint8List(cipher.getOutputSize(cipherBytes.length));
    var offset = cipher.processBytes(cipherBytes, 0, cipherBytes.length, plainText, 0);
    offset += cipher.doFinal(plainText, offset);
    return utf8.decode(plainText.sublist(0, offset));
  }

  static Uint8List _randomBytes(int length) {
    return Uint8List.fromList(
      List.generate(length, (_) => _secureRandom.nextInt(256)),
    );
  }
}
