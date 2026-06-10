import 'dart:async';
import 'dart:convert';
import 'dart:js_interop';
import 'dart:typed_data';

import 'package:web/web.dart' as web;
import 'package:flutter_commons/flutter_commons.dart';

/// AES-GCM encrypted [ClientStorage] implementation for Flutter Web.
///
/// The AES-256 key is created with `extractable: false` and persisted in the
/// origin's IndexedDB (`_sec` database). Because the key is non-extractable,
/// its raw bytes are never accessible to JavaScript — not even via
/// `crypto.subtle.exportKey()`.
///
/// Values are stored in `localStorage` under a `sec.` prefix as
/// `base64(iv[12] || ciphertext)`. A fresh random IV is generated per write.
///
/// ### Usage
/// ```dart
/// final secureStorage = EncryptedWebStorage();
/// await secureStorage.initialize();  // before runApp / connectWebSocket
/// ```
///
/// After [initialize] resolves, [get]/[set]/[remove] are synchronous
/// (cache-backed). Writes persist asynchronously (fire-and-forget).
///
/// ### Fallback
/// If IndexedDB is unavailable (some private-browsing modes), the storage
/// works in-memory only — data is not persisted across reloads.
class EncryptedWebStorage implements ClientStorage {
  final _cache = <String, String>{};
  web.CryptoKey? _cryptoKey;

  static const _lsPrefix = 'sec.';

  @override
  ClientStorage get secure => this;

  @override
  String? get(String key) => _cache[key];

  @override
  void set(String key, String value) {
    _cache[key] = value;
    final k = _cryptoKey;
    if (k != null) {
      _encryptAndStore(key, value, k); // fire-and-forget
    }
  }

  @override
  void remove(String key) {
    _cache.remove(key);
    web.window.localStorage.removeItem('$_lsPrefix$key');
  }

  @override
  Map<String, String> get all => Map.unmodifiable(_cache);

  /// Initialises the AES-GCM key from IndexedDB and pre-decrypts all stored
  /// `sec.*` entries into the in-memory cache.
  Future<void> initialize() async {
    try {
      _cryptoKey = await _openOrCreateKey();
    } catch (_) {
      // IndexedDB unavailable — in-memory only
      return;
    }

    final key = _cryptoKey;
    if (key == null) return;

    final ls = web.window.localStorage;
    final futures = <Future<void>>[];
    for (var i = 0; i < ls.length; i++) {
      final rawKey = ls.key(i);
      if (rawKey == null || !rawKey.startsWith(_lsPrefix)) continue;
      final shortKey = rawKey.substring(_lsPrefix.length);
      final b64 = ls.getItem(rawKey);
      if (b64 == null || b64.isEmpty) continue;

      futures.add(
        _decryptValue(key, b64)
            .then((plaintext) {
              if (plaintext != null) _cache[shortKey] = plaintext;
              // null → stale key or corrupted; silently discard
            })
            .catchError((_) {}),
      );
    }
    await Future.wait(futures);
  }

  Future<void> _encryptAndStore(
    String key,
    String value,
    web.CryptoKey k,
  ) async {
    try {
      final b64 = await _encryptValue(k, value);
      web.window.localStorage.setItem('$_lsPrefix$key', b64);
    } catch (_) {
      // fire-and-forget; non-fatal
    }
  }
}

// ---------------------------------------------------------------------------
// IndexedDB helpers
// ---------------------------------------------------------------------------

Future<web.CryptoKey?> _openOrCreateKey() {
  final completer = Completer<web.CryptoKey?>();

  final req = web.window.indexedDB.open('_sec', 1);

  req.onupgradeneeded = ((web.IDBVersionChangeEvent e) {
    final db = (e.target as web.IDBOpenDBRequest).result as web.IDBDatabase;
    db.createObjectStore('k');
  }).toJS;

  req.onerror = ((web.Event _) {
    completer.completeError('IDB open failed');
  }).toJS;

  req.onsuccess = ((web.Event _) {
    final db = req.result as web.IDBDatabase;
    final tx = db.transaction('k'.toJS, 'readwrite');
    final store = tx.objectStore('k');
    final getReq = store.get(0.toJS);

    getReq.onerror = ((web.Event _) {
      completer.completeError('IDB get failed');
    }).toJS;

    getReq.onsuccess = ((web.Event _) {
      final existing = getReq.result;
      if (existing != null && existing.isA<web.CryptoKey>()) {
        completer.complete(existing as web.CryptoKey);
        return;
      }

      // Generate new non-extractable AES-256-GCM key
      final algo = _aesGcmKeyGenParams();
      final usages = ['encrypt'.toJS, 'decrypt'.toJS].toJS;

      web.window.crypto.subtle
          .generateKey(algo, false, usages)
          .toDart
          .then((jsKey) {
            final cryptoKey = jsKey as web.CryptoKey;
            final tx2 = db.transaction('k'.toJS, 'readwrite');
            tx2.objectStore('k').put(cryptoKey, 0.toJS);
            completer.complete(cryptoKey);
          })
          .catchError((e) => completer.completeError(e));
    }).toJS;
  }).toJS;

  return completer.future;
}

// ---------------------------------------------------------------------------
// Crypto helpers
// ---------------------------------------------------------------------------

/// Encrypts [text] with AES-GCM and returns `base64(iv[12] || ciphertext)`.
Future<String> _encryptValue(web.CryptoKey key, String text) async {
  final iv = _randomIv();
  final encoded = Uint8List.fromList(utf8.encode(text));
  final algo = _aesGcmParams(iv);

  final result = await web.window.crypto.subtle
      .encrypt(algo, key, encoded.toJS)
      .toDart;

  final ctBytes = (result as JSArrayBuffer).toDart.asUint8List();
  final combined = Uint8List(12 + ctBytes.length);
  combined.setRange(0, 12, iv);
  combined.setRange(12, combined.length, ctBytes);
  return base64.encode(combined);
}

/// Decrypts a `base64(iv[12] || ciphertext)` value. Returns `null` on failure.
Future<String?> _decryptValue(web.CryptoKey key, String b64) async {
  try {
    final combined = base64.decode(b64);
    if (combined.length <= 12) return null;

    final iv = Uint8List.fromList(combined.sublist(0, 12));
    final ct = Uint8List.fromList(combined.sublist(12));
    final algo = _aesGcmParams(iv);

    final result = await web.window.crypto.subtle
        .decrypt(algo, key, ct.toJS)
        .toDart;

    return utf8.decode((result as JSArrayBuffer).toDart.asUint8List());
  } catch (_) {
    return null;
  }
}

// ---------------------------------------------------------------------------
// JS param helpers (package:web uses JSObject for algorithm params)
// ---------------------------------------------------------------------------

@JS()
@staticInterop
@anonymous
class _AesGcmParams {}

extension type _AesGcmKeyGenParamsExt._(JSObject _) implements JSObject {
  external factory _AesGcmKeyGenParamsExt({
    required String name,
    required int length,
  });
}

extension type _AesGcmParamsExt._(JSObject _) implements JSObject {
  external factory _AesGcmParamsExt({
    required String name,
    required JSUint8Array iv,
  });
}

JSObject _aesGcmKeyGenParams() =>
    _AesGcmKeyGenParamsExt(name: 'AES-GCM', length: 256);

JSObject _aesGcmParams(Uint8List iv) =>
    _AesGcmParamsExt(name: 'AES-GCM', iv: iv.toJS);

Uint8List _randomIv() {
  final iv = Uint8List(12);
  web.window.crypto.getRandomValues(iv.toJS);
  return iv;
}
