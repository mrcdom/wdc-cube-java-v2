/// Date/time and currency formatting utilities (en-US).
library;

String _two(int n) => n.toString().padLeft(2, '0');

/// Formats milliseconds-since-epoch as "MM/dd/yyyy h:mm AM|PM" (12-hour clock).
/// Returns empty string for 0.
String formatDateTime(int millis) {
  if (millis == 0) return '';
  final d = DateTime.fromMillisecondsSinceEpoch(millis);
  final hour12 = d.hour % 12 == 0 ? 12 : d.hour % 12;
  final period = d.hour < 12 ? 'AM' : 'PM';
  return '${_two(d.month)}/${_two(d.day)}/${d.year} $hour12:${_two(d.minute)} $period';
}

/// Formats milliseconds-since-epoch as "MM/dd/yyyy".
/// Returns a fallback message for 0.
String formatDate(int millis, {String fallback = 'Date unavailable'}) {
  if (millis == 0) return fallback;
  final d = DateTime.fromMillisecondsSinceEpoch(millis);
  return '${_two(d.month)}/${_two(d.day)}/${d.year}';
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
