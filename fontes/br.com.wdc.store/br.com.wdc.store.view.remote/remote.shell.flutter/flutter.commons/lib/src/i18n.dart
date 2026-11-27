import 'dart:ui' show PlatformDispatcher;

import 'generated/messages.dart' as messages;

/// The language the views show: one of the catalog's, picked from the platform locale at startup.
abstract final class StoreI18n {
  /// The current catalog language (`en-US`, `pt-BR`), the platform's until [init] or a test sets it.
  static String language = _platformLanguage();

  /// Picks the language from [tag], or from the platform locale when [tag] is absent.
  static void init([String? tag]) {
    language = messages.languageOf(tag ?? PlatformDispatcher.instance.locale.toLanguageTag());
  }

  /// The text of [key] in the current language, each `{name}` replaced by its argument.
  static String text(String key, [Map<String, String>? args]) => messages.text(language, key, args);

  /// The text of a presenter message as it travels (`{"key", "args"}`), in the current language.
  static String message(Object? message) => messages.translate(language, message);

  static String _platformLanguage() => messages.languageOf(PlatformDispatcher.instance.locale.toLanguageTag());
}
