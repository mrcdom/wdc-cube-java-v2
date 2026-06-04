// Smoke tests for flutter.mobile.
//
// The app is a WebSocket shell that connects to a remote backend — full
// integration tests require a live server. These tests cover only the widgets
// that are renderable without a network connection.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_mobile/main.dart';

void main() {
  testWidgets('ConnectionErrorApp renders error message, icon and endpoint', (
    WidgetTester tester,
  ) async {
    const endpoint = 'http://localhost:8080';
    await tester.pumpWidget(const ConnectionErrorApp(endpoint: endpoint));

    expect(find.byIcon(Icons.cloud_off), findsOneWidget);
    expect(find.text('Não foi possível conectar ao servidor.'), findsOneWidget);
    expect(find.text(endpoint), findsOneWidget);
  });
}
