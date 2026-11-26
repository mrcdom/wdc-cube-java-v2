import 'dart:convert';
import 'dart:io' show HttpClient, Platform;

import 'package:flutter/material.dart';
import 'package:flutter_commons/flutter_commons.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Resolves the base HTTP URL from:
/// 1. --dart-define=WDC_ENDPOINT=http://host:port
/// 2. WDC_ENDPOINT environment variable
/// 3. Default: http://localhost:8080 (use machine IP for real device/simulator)
String _resolveBaseHttpUrl() {
  const compiled = String.fromEnvironment('WDC_ENDPOINT');
  if (compiled.isNotEmpty) return compiled;
  final env = Platform.environment['WDC_ENDPOINT'];
  if (env != null && env.isNotEmpty) return env;
  return 'http://localhost:8080';
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
late FlutterSecureStorage _securePrefs;

/// Fetches a session id (appId) from the backend; null on failure.
Future<String?> _fetchSessionInit(String baseHttpUrl) async {
  try {
    final client = HttpClient();
    final uri = Uri.parse('$baseHttpUrl/api/session/init');
    final request = await client.getUrl(uri);
    final response = await request.close();
    if (response.statusCode == 200) {
      final body = await response.transform(utf8.decoder).join();
      final json = jsonDecode(body) as Map<String, dynamic>;
      return json['appId'] as String;
    }
  } catch (e) {
    debugPrint('Failed to fetch session init: $e');
  }
  return null;
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  _prefs = await SharedPreferences.getInstance();
  _securePrefs = const FlutterSecureStorage();

  final baseHttpUrl = _resolveBaseHttpUrl();
  final baseWsUrl = _toWsUrl(baseHttpUrl);

  // Fetch a fresh session id from the server
  final appId = await _fetchSessionInit(baseHttpUrl);
  if (appId == null) {
    runApp(ConnectionErrorApp(endpoint: baseHttpUrl));
    return;
  }

  // Persist for potential reconnection
  _prefs.setString('app_id', appId);

  // Build ClientStorage instances backed by SharedPreferences / FlutterSecureStorage.
  //
  // secureStorage uses an in-memory cache pre-populated from FlutterSecureStorage.
  // This is necessary because FlutterSecureStorage is async-only: get() and all()
  // must be synchronous for the WebSocket bootstrap payload. Without the cache,
  // all() returns {} → server never sees the token → tryAutoLogin() never runs.
  final secureCache = Map<String, String>.from(await _securePrefs.readAll());
  final secureStorage = DelegateClientStorage(
    get: (key) => secureCache[key],
    set: (key, value) {
      secureCache[key] = value;
      _securePrefs.write(key: key, value: value);
    },
    remove: (key) {
      secureCache.remove(key);
      _securePrefs.delete(key: key);
    },
    all: () => Map.unmodifiable(secureCache),
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
      final result = <String, String>{};
      for (final key in _prefs.getKeys()) {
        if (key.startsWith('app_') || key == 'req_seq' || key == 'last_path') {
          continue;
        }
        final v = _prefs.getString(key);
        if (v != null) result[key] = v;
      }
      return result;
    },
    secureFactory: () => secureStorage,
  );
  final sessionStorage = InMemoryClientStorage();

  final coordinator = ScopeCoordinator(
    CoordinatorConfig(
      appId: appId,
      baseWebSocketUrl: baseWsUrl,
      sessionStorage: sessionStorage,
      persistentStorage: persistentStorage,
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
    // Fetch a new session id and restart
    final newAppId = await _fetchSessionInit(baseHttpUrl);
    if (newAppId != null) {
      _prefs.setString('app_id', newAppId);
      coordinator.useSession(newAppId);
    }
    _prefs.remove('req_seq');
    _prefs.remove('last_path');
    coordinator.onStop();
    coordinator.onStart();
  };

  registerAllViews(coordinator);

  runApp(const StoreMobileApp());
}

class StoreMobileApp extends StatelessWidget {
  const StoreMobileApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WDC Store',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF0D66D0)),
        useMaterial3: true,
      ),
      home: const Scaffold(body: SafeArea(child: BrowserView())),
    );
  }
}

class ConnectionErrorApp extends StatelessWidget {
  final String endpoint;
  const ConnectionErrorApp({super.key, required this.endpoint});

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
                'Could not connect to the server.',
                style: TextStyle(fontSize: 18),
              ),
              const SizedBox(height: 8),
              Text(
                endpoint,
                style: const TextStyle(fontSize: 14, color: Colors.grey),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
