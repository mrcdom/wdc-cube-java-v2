/// Date/time formatting in the current language's patterns, and currency in US dollars (en-US).
library;

import '../generated/messages.dart' show MessageKeys;
import '../i18n.dart';

/// Formats [date] with a catalog pattern: `yyyy` year, `MM` month, `dd` day, `HH` hour (0-23),
/// `h` hour (1-12), `mm` minute, `ss` second, `a` AM/PM; a run of a letter pads to its length,
/// any other character is copied.
String formatDatePattern(DateTime date, String pattern) {
  final out = StringBuffer();
  var i = 0;
  while (i < pattern.length) {
    final c = pattern[i];
    var j = i;
    while (j < pattern.length && pattern[j] == c) {
      j++;
    }
    final width = j - i;
    String pad(int n) => n.toString().padLeft(width, '0');
    switch (c) {
      case 'y':
        out.write(pad(date.year));
      case 'M':
        out.write(pad(date.month));
      case 'd':
        out.write(pad(date.day));
      case 'H':
        out.write(pad(date.hour));
      case 'h':
        out.write(pad(date.hour % 12 == 0 ? 12 : date.hour % 12));
      case 'm':
        out.write(pad(date.minute));
      case 's':
        out.write(pad(date.second));
      case 'a':
        out.write(date.hour < 12 ? 'AM' : 'PM');
      default:
        out.write(pattern.substring(i, j));
    }
    i = j;
  }
  return out.toString();
}

/// Formats milliseconds-since-epoch with the language's `format.dateTime`.
/// Returns empty string for 0.
String formatDateTime(int millis) {
  if (millis == 0) return '';
  final d = DateTime.fromMillisecondsSinceEpoch(millis);
  return formatDatePattern(d, StoreI18n.text(MessageKeys.formatDateTime));
}

/// Formats milliseconds-since-epoch with the language's `format.date`.
/// Returns the language's `format.dateUnavailable` (or [fallback]) for 0.
String formatDate(int millis, {String? fallback}) {
  if (millis == 0) return fallback ?? StoreI18n.text(MessageKeys.formatDateUnavailable);
  final d = DateTime.fromMillisecondsSinceEpoch(millis);
  return formatDatePattern(d, StoreI18n.text(MessageKeys.formatDate));
}

/// Formats a double as US dollars: "$1,234.56" (negative: "-$1,234.56").
String formatCurrency(double value) {
  final fixed = value.abs().toStringAsFixed(2);
  final dot = fixed.indexOf('.');
  final intPart = fixed.substring(0, dot);
  final grouped = StringBuffer();
  for (var i = 0; i < intPart.length; i++) {
    if (i > 0 && (intPart.length - i) % 3 == 0) grouped.write(',');
    grouped.write(intPart[i]);
  }
  final sign = value < 0 ? '-' : '';
  return '$sign\$$grouped${fixed.substring(dot)}';
}
