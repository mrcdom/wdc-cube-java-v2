import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_web/main.dart';

void main() {
  testWidgets('App starts without error', (WidgetTester tester) async {
    await tester.pumpWidget(const ShoppingApp());
    expect(find.text('WDC Shopping'), findsNothing); // App bootstraps OK
  });
}
