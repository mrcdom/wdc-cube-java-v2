import 'dart:convert';
import 'dart:io' show HttpClient, Platform;

import 'package:flutter/material.dart';
import 'package:flutter_commons/flutter_commons.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Resolves the base HTTP URL from:
/// 1. --dart-define=WDC_ENDPOINT=http://host:port
/// 2. WDC_ENDPOINT environment variable
/// 3. Default: http://localhost:8080
String _resolveBaseHttpUrl() {
  const compiled = String.fromEnvironment('WDC_ENDPOINT');
  final endpoint = compiled.isNotEmpty
      ? compiled
      : Platform.environment['WDC_ENDPOINT'] ?? 'http://localhost:8080';
  return endpoint;
}

/// Converts an HTTP URL to WebSocket URL.
String _toWsUrl(String httpUrl) {
  if (httpUrl.startsWith('https://')) {
    return httpUrl.replaceFirst('https://', 'wss://');
  }
  if (httpUrl.startsWith('http://')) {
    return httpUrl.replaceFirst('http://', 'ws://');
  }
  return httpUrl;
}

late SharedPreferences _prefs;

/// Fetches session credentials from the backend.
/// Returns {appId, appSKey} or null on failure.
Future<Map<String, String>?> _fetchSessionInit(String baseHttpUrl) async {
  try {
    final client = HttpClient();
    final uri = Uri.parse('$baseHttpUrl/api/session/init');
    final request = await client.getUrl(uri);
    final response = await request.close();
    if (response.statusCode == 200) {
      final body = await response.transform(utf8.decoder).join();
      final json = jsonDecode(body) as Map<String, dynamic>;
      return {
        'appId': json['appId'] as String,
        'appSKey': json['appSKey'] as String,
      };
    }
  } catch (e) {
    debugPrint('Failed to fetch session init: $e');
  }
  return null;
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  _prefs = await SharedPreferences.getInstance();

  final baseHttpUrl = _resolveBaseHttpUrl();
  final baseWsUrl = _toWsUrl(baseHttpUrl);

  // Fetch fresh credentials from the server
  final session = await _fetchSessionInit(baseHttpUrl);
  if (session == null) {
    runApp(const ConnectionErrorApp());
    return;
  }

  final appId = session['appId']!;
  final appSKey = session['appSKey']!;

  // Persist for potential reconnection
  _prefs.setString('app_id', appId);
  _prefs.setString('app_skey', appSKey);

  // Build ClientStorage instances backed by SharedPreferences / FlutterSecureStorage.
  //
  // On macOS the app runs in a sandbox. FlutterSecureStorage (Keychain) requires
  // the keychain-access-groups entitlement — without it readAll() returns empty.
  // For desktop we fall back to SharedPreferences with a 'sec.' key prefix, which
  // is protected by the OS sandbox and avoids the Keychain entitlement requirement.
  // The values are still ciphered over the WebSocket regardless.
  final secureStorage = DelegateClientStorage(
    get: (key) => _prefs.getString('sec.$key'),
    set: (key, value) => _prefs.setString('sec.$key', value),
    remove: (key) => _prefs.remove('sec.$key'),
    all: () {
      return {
        for (final k in _prefs.getKeys().where((k) => k.startsWith('sec.')))
          k.substring(4): _prefs.getString(k)!,
      };
    },
    secureFactory: () => InMemoryClientStorage(), // already IS secure
  );
  final persistentStorage = DelegateClientStorage(
    get: (key) => _prefs.getString(key),
    set: (key, value) {
      _prefs.setString(key, value);
    },
    remove: (key) {
      _prefs.remove(key);
    },
    all: () {
      return {
        for (final key in _prefs.getKeys().where(
          (k) =>
              !k.startsWith('app_') &&
              !k.startsWith('sec.') &&
              k != 'req_seq' &&
              k != 'last_path',
        ))
          key: _prefs.getString(key)!,
      };
    },
    secureFactory: () => secureStorage,
  );
  final sessionStorage = InMemoryClientStorage();

  final coordinator = ViewStateCoordinator(
    CoordinatorConfig(
      appId: appId,
      securityKey: appSKey,
      baseWebSocketUrl: baseWsUrl,
      sessionStorage: sessionStorage,
      persistentStorage: persistentStorage,
      onSetCookie: (name, value) {
        _prefs.setString(name, value);
      },
      onPersistRequestSeq: (count) {
        _prefs.setInt('req_seq', count);
      },
      onRestoreRequestSeq: () {
        return _prefs.getInt('req_seq') ?? 0;
      },
    ),
  );

  // Restore last navigation path (survives app restart)
  final lastPath = _prefs.getString('last_path');
  if (lastPath != null && lastPath.isNotEmpty) {
    coordinator.path = lastPath;
  }

  // Persist navigation path on every URI change from server
  coordinator.onUriChanged = (uri) {
    coordinator.path = uri;
    _prefs.setString('last_path', uri);
  };

  coordinator.onSessionInvalid = () async {
    // Fetch new credentials and restart
    final newSession = await _fetchSessionInit(baseHttpUrl);
    if (newSession != null) {
      _prefs.setString('app_id', newSession['appId']!);
      _prefs.setString('app_skey', newSession['appSKey']!);
    }
    _prefs.remove('req_seq');
    _prefs.remove('last_path');
    coordinator.onStop();
    coordinator.onStart();
  };

  registerAllViews(coordinator);

  runApp(const ShoppingDesktopApp());
}

class ShoppingDesktopApp extends StatelessWidget {
  const ShoppingDesktopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WDC Shopping',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF0D66D0)),
        useMaterial3: true,
      ),
      home: const Scaffold(body: BrowserView()),
    );
  }
}

class ConnectionErrorApp extends StatelessWidget {
  const ConnectionErrorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off, size: 64, color: Colors.grey),
              const SizedBox(height: 16),
              const Text(
                'Não foi possível conectar ao servidor.',
                style: TextStyle(fontSize: 18),
              ),
              const SizedBox(height: 8),
              Text(
                _resolveBaseHttpUrl(),
                style: const TextStyle(fontSize: 14, color: Colors.grey),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
