import 'dart:convert';
import 'dart:io' show HttpClient, Platform;

import 'package:material_ui/material_ui.dart';
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
  // The language of the texts: the device's, once
  StoreI18n.init();
  _prefs = await SharedPreferences.getInstance();

  final baseHttpUrl = _resolveBaseHttpUrl();
  final baseWsUrl = _toWsUrl(baseHttpUrl);

  // Fetch a fresh session id from the server
  final appId = await _fetchSessionInit(baseHttpUrl);
  if (appId == null) {
    runApp(const ConnectionErrorApp());
    return;
  }

  // Persist for potential reconnection
  _prefs.setString('app_id', appId);

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
      final result = <String, String>{};
      for (final k in _prefs.getKeys().where((k) => k.startsWith('sec.'))) {
        final v = _prefs.getString(k);
        if (v != null) result[k.substring(4)] = v;
      }
      return result;
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

  runApp(const StoreDesktopApp());
}

class StoreDesktopApp extends StatelessWidget {
  const StoreDesktopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: StoreI18n.text(MessageKeys.appName),
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: StoreTokens.primary),
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
              const Icon(Icons.cloud_off, size: 64, color: StoreTokens.textMuted),
              const SizedBox(height: 16),
              Text(
                StoreI18n.text(MessageKeys.errorConnection),
                style: const TextStyle(fontSize: StoreTokens.fontSizeLg),
              ),
              const SizedBox(height: 8),
              Text(
                _resolveBaseHttpUrl(),
                style: const TextStyle(fontSize: StoreTokens.fontSizeSm, color: StoreTokens.textSecondary),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
