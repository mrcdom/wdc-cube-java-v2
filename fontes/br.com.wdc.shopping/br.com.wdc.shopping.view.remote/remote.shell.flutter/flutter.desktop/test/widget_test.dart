// Smoke tests for flutter.desktop.
//
// The app is a WebSocket shell that connects to a remote backend — full
// integration tests require a live server. These tests cover only the widgets
// that are renderable without a network connection.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_desktop/main.dart';

void main() {
  testWidgets('ConnectionErrorApp renders error message and icon', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const ConnectionErrorApp());

    expect(find.byIcon(Icons.cloud_off), findsOneWidget);
    expect(find.text('Não foi possível conectar ao servidor.'), findsOneWidget);
  });
}
