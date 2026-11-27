// End to end against a running remote host (WDC_ENDPOINT, by default http://localhost:8080 — on the Android
// emulator, http://10.0.2.2:8080): signs in as jdoe, buys a product and sees the receipt.
//
// The texts come from the catalog, so it passes in any of the device's languages. It starts from an empty device
// storage: the app reopens where it was left (the last address, the session), which a test cannot count on.
//
//   flutter test integration_test -d <device> [--dart-define=WDC_ENDPOINT=http://host:port]
import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_commons/flutter_commons.dart';

import 'package:flutter_desktop/main.dart' as app;

Future<void> pumpUntil(
  WidgetTester tester,
  Finder finder, {
  Duration timeout = const Duration(seconds: 30),
}) async {
  final end = DateTime.now().add(timeout);
  while (DateTime.now().isBefore(end)) {
    await tester.pump(const Duration(milliseconds: 200));
    if (finder.evaluate().isNotEmpty) return;
  }
  fail('Not found in ${timeout.inSeconds}s: $finder');
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('signs in, buys a product and sees the receipt', (tester) async {
    // what earlier runs left (the desktop keeps all of it in its preferences): the address to reopen, the remote
    // session, the persistent sign-in
    await (await SharedPreferences.getInstance()).clear();
    app.main();

    // The product's card in the catalog (the purchase history may show the same name), which shows the Home in every
    // layout
    final product = find.descendant(
      of: find.byWidgetPredicate(
        (w) => w.runtimeType.toString() == '_ProductCard',
      ),
      matching: find.text('2GB Flash Drive'),
    );

    final signIn = find.text(StoreI18n.text(MessageKeys.loginSignIn));
    await pumpUntil(tester, signIn);
    await tester.enterText(find.byType(TextField).at(0), 'jdoe');
    await tester.enterText(find.byType(TextField).at(1), 'jdoe');
    await tester.tap(signIn);

    await pumpUntil(tester, product);
    await tester.ensureVisible(product.first);
    await tester.tap(product.first);
    await pumpUntil(tester, find.text(StoreI18n.text(MessageKeys.productAddToCart)));
    await tester.tap(find.text(StoreI18n.text(MessageKeys.productAddToCart)));
    await pumpUntil(tester, find.text(StoreI18n.text(MessageKeys.cartCheckout)));
    await tester.tap(find.text(StoreI18n.text(MessageKeys.cartCheckout)));
    await pumpUntil(tester, find.text(StoreI18n.text(MessageKeys.receiptCompleted)));
  });
}
