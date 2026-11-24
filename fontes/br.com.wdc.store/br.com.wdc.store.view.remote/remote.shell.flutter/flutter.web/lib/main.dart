import 'dart:js_interop';

import 'package:web/web.dart' as web;

import 'package:flutter/material.dart';
import 'package:flutter_commons/flutter_commons.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';

import 'encrypted_web_storage.dart';

void main() async {
  // Disable Flutter's internal URL management — we handle navigation ourselves
  setUrlStrategy(null);

  final appId = _resolveAppId();
  final appSKey = _consumeCookie('app_skey');
  final protocol = web.window.location.protocol == 'http:' ? 'ws://' : 'wss://';
  final baseWsUrl = '$protocol${web.window.location.host}';

  // syncNamespace '~rf:' qualifies keys for WebSocket sync and isolates this
  // shell's data from other shells sharing the same origin.
  // Stored as '~rf:key' in localStorage; 'all()' strips the namespace.
  const syncNamespace = '~rf:';
  final secureStorage = EncryptedWebStorage(syncNamespace);
  await secureStorage.initialize();
  final persistentStorage = _webStorage(
    web.window.localStorage,
    syncNamespace,
    () => secureStorage,
  );
  // The session scope survives a page reload (the cart lives there), not the end of the tab.
  final sessionStorage = _webStorage(
    web.window.sessionStorage,
    syncNamespace,
    () => InMemoryClientStorage(),
  );

  final coordinator = ViewStateCoordinator(
    CoordinatorConfig(
      appId: appId,
      sessionStorage: sessionStorage,
      persistentStorage: persistentStorage,
      securityKey: appSKey,
      baseWebSocketUrl: baseWsUrl,
      onSetCookie: (name, value) {
        web.document.cookie = '$name=$value; path=/';
      },
      onPersistRequestSeq: (count) {
        web.window.sessionStorage.setItem('req_seq', count.toString());
      },
      onRestoreRequestSeq: () {
        final saved = web.window.sessionStorage.getItem('req_seq');
        if (saved != null && saved.isNotEmpty) {
          final parsed = int.tryParse(saved);
          if (parsed != null && parsed > 0) return parsed;
        }
        return 0;
      },
    ),
  );

  // Read initial path from URL hash (e.g. #home?rt-user=0&sign=abc → home?rt-user=0&sign=abc)
  final hash = web.window.location.hash;
  if (hash.length > 1) {
    coordinator.path = hash.substring(1);
  }

  // Track whether we're currently handling a back/forward event
  var navigatingFromPopState = false;
  // First URI response replaces the current entry (no back to blank page)
  var firstUriResponse = true;

  // When backend sends a URI update, sync it to the browser URL hash.
  // Uses pushState/replaceState which don't trigger popstate or page reload.
  coordinator.onUriChanged = (uri) {
    coordinator.path = uri;
    final currentHash = web.window.location.hash;
    final expected = '#$uri';
    if (currentHash != expected) {
      if (firstUriResponse || navigatingFromPopState) {
        // First response (boot) or response to back/forward:
        // replaceState updates hash without creating a new history entry.
        web.window.history.replaceState(null, '', expected);
      } else {
        // Normal navigation: create a new history entry for back button.
        web.window.history.pushState(null, '', expected);
      }
    }
    firstUriResponse = false;
    navigatingFromPopState = false;
  };

  // Listen for browser back/forward (popstate only fires on history traversal,
  // NOT on pushState/replaceState — so no loop).
  web.window.onpopstate = ((web.Event _) {
    final newHash = web.window.location.hash;
    final newPath = newHash.length > 1 ? newHash.substring(1) : '/';
    if (newPath != coordinator.path) {
      navigatingFromPopState = true;
      coordinator.path = newPath;
      coordinator.setFormField(browserVsid, 'p.path', newPath);
      coordinator.submit(browserVsid, -2);
    }
  }).toJS;

  coordinator.onSessionInvalid = () {
    // Discards the stored app_id before reloading — otherwise the reload
    // reconnects with the same invalid id and loops forever.
    web.window.sessionStorage.removeItem('app_id');
    web.window.location.reload();
  };

  registerAllViews(coordinator);

  runApp(const StoreApp());
}

/// Resolves app_id: sessionStorage takes priority (survives refresh),
/// cookie is only used on the very first load (then removed).
/// Matches React's behavior: sessionStorage > cookie > fallback.
String _resolveAppId() {
  // Always consume (remove) the cookie so it doesn't interfere
  final fromCookie = _consumeCookie('app_id');

  // SessionStorage survives F5 — use it if present
  final stored = web.window.sessionStorage.getItem('app_id');
  if (stored != null && stored.isNotEmpty) {
    return stored;
  }

  // First load: use the cookie value and persist to sessionStorage
  if (fromCookie != null) {
    web.window.sessionStorage.setItem('app_id', fromCookie);
    return fromCookie;
  }

  // Fallback: generate a fake ID (server won't validate it)
  return '${DateTime.now().millisecondsSinceEpoch}.fake';
}

/// Reads and removes a cookie by name. Returns null if not found.
String? _consumeCookie(String name) {
  final cookies = web.document.cookie;
  for (final part in cookies.split(';')) {
    final trimmed = part.trim();
    if (trimmed.startsWith('$name=')) {
      final value = trimmed.substring(name.length + 1);
      // Remove cookie
      web.document.cookie = '$name=; path=/; max-age=0';
      return value;
    }
  }
  return null;
}

class StoreApp extends StatelessWidget {
  const StoreApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WDC Store',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF0D66D0)),
        useMaterial3: true,
      ),
      home: const Scaffold(body: BrowserView()),
    );
  }
}

/// A client storage scope over a browser [store]: keys live under [syncNamespace], which
/// `all()` strips. The `sec.` keys belong to the secure view (ciphertext) and stay out of `all()`.
DelegateClientStorage _webStorage(
  web.Storage store,
  String syncNamespace,
  ClientStorage Function() secureFactory,
) {
  final securePrefix = '${syncNamespace}sec.';
  return DelegateClientStorage(
    get: (key) => store.getItem('$syncNamespace$key'),
    set: (key, value) => store.setItem('$syncNamespace$key', value),
    remove: (key) => store.removeItem('$syncNamespace$key'),
    all: () {
      final result = <String, String>{};
      for (var i = 0; i < store.length; i++) {
        final rawKey = store.key(i);
        if (rawKey == null ||
            !rawKey.startsWith(syncNamespace) ||
            rawKey.startsWith(securePrefix)) {
          continue;
        }
        final v = store.getItem(rawKey);
        if (v != null) result[rawKey.substring(syncNamespace.length)] = v;
      }
      return result;
    },
    secureFactory: secureFactory,
  );
}
