import 'dart:html' as html;

import 'package:flutter/material.dart';
import 'package:flutter_commons/flutter_commons.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';

void main() {
  // Disable Flutter's internal URL management — we handle navigation ourselves
  setUrlStrategy(null);

  final appId = _resolveAppId();
  final appSKey = _consumeCookie('app_skey');
  final protocol = html.window.location.protocol == 'http:' ? 'ws://' : 'wss://';
  final baseWsUrl = '$protocol${html.window.location.host}';

  final coordinator = ViewStateCoordinator(CoordinatorConfig(
    appId: appId,
    securityKey: appSKey,
    baseWebSocketUrl: baseWsUrl,
    onSetCookie: (name, value) {
      html.document.cookie = '$name=$value; path=/';
    },
    onPersistRequestSeq: (count) {
      html.window.sessionStorage['req_seq'] = count.toString();
    },
    onRestoreRequestSeq: () {
      final saved = html.window.sessionStorage['req_seq'];
      if (saved != null && saved.isNotEmpty) {
        final parsed = int.tryParse(saved);
        if (parsed != null && parsed > 0) return parsed;
      }
      return 0;
    },
  ));

  // Read initial path from URL hash (e.g. #home?userId=0&sign=abc → home?userId=0&sign=abc)
  final hash = html.window.location.hash;
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
    final currentHash = html.window.location.hash;
    final expected = '#$uri';
    if (currentHash != expected) {
      if (firstUriResponse || navigatingFromPopState) {
        // First response (boot) or response to back/forward:
        // replaceState updates hash without creating a new history entry.
        html.window.history.replaceState(null, '', expected);
      } else {
        // Normal navigation: create a new history entry for back button.
        html.window.history.pushState(null, '', expected);
      }
    }
    firstUriResponse = false;
    navigatingFromPopState = false;
  };

  // Listen for browser back/forward (popstate only fires on history traversal,
  // NOT on pushState/replaceState — so no loop).
  html.window.onPopState.listen((_) {
    final newHash = html.window.location.hash;
    final newPath = newHash.length > 1 ? newHash.substring(1) : '/';
    if (newPath != coordinator.path) {
      navigatingFromPopState = true;
      coordinator.path = newPath;
      coordinator.setFormField(browserVsid, 'p.path', newPath);
      coordinator.submit(browserVsid, -2);
    }
  });

  registerAllViews(coordinator);

  runApp(const ShoppingApp());
}

/// Resolves app_id: sessionStorage takes priority (survives refresh),
/// cookie is only used on the very first load (then removed).
/// Matches React's behavior: sessionStorage > cookie > fallback.
String _resolveAppId() {
  // Always consume (remove) the cookie so it doesn't interfere
  final fromCookie = _consumeCookie('app_id');

  // SessionStorage survives F5 — use it if present
  final stored = html.window.sessionStorage['app_id'];
  if (stored != null && stored.isNotEmpty) {
    return stored;
  }

  // First load: use the cookie value and persist to sessionStorage
  if (fromCookie != null) {
    html.window.sessionStorage['app_id'] = fromCookie;
    return fromCookie;
  }

  // Fallback: generate a fake ID (server won't validate it)
  return '${DateTime.now().millisecondsSinceEpoch}.fake';
}

/// Reads and removes a cookie by name. Returns null if not found.
String? _consumeCookie(String name) {
  final cookies = html.document.cookie ?? '';
  for (final part in cookies.split(';')) {
    final trimmed = part.trim();
    if (trimmed.startsWith('$name=')) {
      final value = trimmed.substring(name.length + 1);
      // Remove cookie
      html.document.cookie = '$name=; path=/; max-age=0';
      return value;
    }
  }
  return null;
}

class ShoppingApp extends StatelessWidget {
  const ShoppingApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'WDC Shopping',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF0D66D0)),
        useMaterial3: true,
        filledButtonTheme: FilledButtonThemeData(
          style: FilledButton.styleFrom(
            backgroundColor: const Color(0xFF2563EB),
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          ),
        ),
      ),
      home: const Scaffold(
        body: BrowserView(),
      ),
    );
  }
}
