/// Date/time formatting utilities.
library;

/// Formats milliseconds-since-epoch as "dd/MM/yyyy HH:mm".
/// Returns empty string for 0.
String formatDateTime(int millis) {
  if (millis == 0) return '';
  final d = DateTime.fromMillisecondsSinceEpoch(millis);
  final date =
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
  final time =
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';
  return '$date $time';
}

/// Formats milliseconds-since-epoch as "dd/MM/yyyy".
/// Returns a fallback message for 0.
String formatDate(int millis, {String fallback = 'Data indisponível'}) {
  if (millis == 0) return fallback;
  final d = DateTime.fromMillisecondsSinceEpoch(millis);
  return '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';
}

/// Formats a double as Brazilian currency "R$ 0.00".
String formatCurrency(double value) => 'R\$ ${value.toStringAsFixed(2)}';
