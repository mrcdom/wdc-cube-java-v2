import 'package:material_ui/material_ui.dart';
import 'package:flutter_commons/flutter_commons.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  tearDown(() => StoreI18n.init('en-US'));

  test('the platform tag maps to a catalog language', () {
    expect(languageOf('pt-BR'), 'pt-BR');
    expect(languageOf('pt-PT'), 'pt-BR');
    expect(languageOf('pt_BR'), 'pt-BR');
    expect(languageOf('en-GB'), 'en-US');
    expect(languageOf('fr-FR'), defaultLanguage);
    expect(languageOf(null), defaultLanguage);
  });

  test('dates follow the language patterns; prices stay in US dollars', () {
    final morning = DateTime(2026, 3, 7, 9, 5).millisecondsSinceEpoch;
    final evening = DateTime(2026, 3, 7, 21, 30).millisecondsSinceEpoch;

    StoreI18n.init('en-US');
    expect(formatDate(morning), '03/07/2026');
    expect(formatDateTime(morning), '03/07/2026 9:05 AM');
    expect(formatDateTime(evening), '03/07/2026 9:30 PM');
    expect(formatDate(0), 'Date unavailable');
    expect(formatCurrency(1234.5), r'$1,234.50');

    StoreI18n.init('pt-BR');
    expect(formatDate(morning), '07/03/2026');
    expect(formatDateTime(evening), '07/03/2026 21:30');
    expect(formatDate(0), StoreI18n.text(MessageKeys.formatDateUnavailable));
    expect(formatCurrency(1234.5), r'$1,234.50');
  });

  test('a presenter message translates from its wire map, with its args', () {
    final wire = {
      'key': MessageKeys.errorSigninTooManySeconds,
      'args': {'seconds': '30'},
    };
    StoreI18n.init('pt-BR');
    final pt = StoreI18n.message(wire);
    expect(pt, contains('30'));
    expect(pt, isNot(contains('{seconds}')));
    StoreI18n.init('en-US');
    expect(StoreI18n.message(wire), 'Too many failed sign-ins. Try again in 30 s.');
    expect(StoreI18n.message(null), '');
  });

  testWidgets('a status banner draws the error tokens and the translated OK', (tester) async {
    StoreI18n.init('pt-BR');
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: StatusBanner(message: 'x', onDismiss: () {})),
    ));
    expect(find.text(StoreI18n.text(MessageKeys.alertOk)), findsOneWidget);
    final text = tester.widget<Text>(find.text('x'));
    expect(text.style?.color, StoreTokens.errorText);
  });
}
