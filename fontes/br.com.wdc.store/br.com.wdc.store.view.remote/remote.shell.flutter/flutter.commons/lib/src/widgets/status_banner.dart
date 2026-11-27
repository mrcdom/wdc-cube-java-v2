import 'package:flutter/material.dart';

import '../design_tokens.dart';
import '../generated/messages.dart' show MessageKeys;
import '../generated/store_tokens.dart';
import '../i18n.dart';

enum StatusSeverity { error, success, warning }

/// A generic status banner supporting error, success, and warning severities.
/// Replaces inline banner patterns across views.
class StatusBanner extends StatelessWidget {
  final String message;
  final String? title;
  final String? detail;
  final VoidCallback? onDismiss;
  final StatusSeverity severity;

  const StatusBanner({
    super.key,
    required this.message,
    this.title,
    this.detail,
    this.onDismiss,
    this.severity = StatusSeverity.error,
  });

  @override
  Widget build(BuildContext context) {
    final (bgColor, borderColor, fgColor, icon) = switch (severity) {
      StatusSeverity.error => (StoreTokens.errorBackground, StoreTokens.errorBorder, StoreTokens.errorText, Icons.error_outline),
      StatusSeverity.success => (StoreTokens.successBackground, StoreTokens.successBorder, StoreTokens.successText, Icons.check_circle),
      StatusSeverity.warning => (StoreTokens.warningBackground, StoreTokens.warningBorder, StoreTokens.warningText, Icons.warning_amber),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: BorderRadius.circular(radiusSm),
        border: Border.all(color: borderColor),
      ),
      child: Row(
        children: [
          Icon(icon, color: severity == StatusSeverity.warning ? StoreTokens.warning : fgColor, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (title != null)
                  Text(title!, style: TextStyle(fontWeight: FontWeight.bold, color: fgColor, fontSize: 13)),
                Text(message, style: TextStyle(color: fgColor, fontSize: 13)),
                if (detail != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(detail!, style: TextStyle(color: fgColor, fontSize: 11)),
                  ),
              ],
            ),
          ),
          if (onDismiss != null)
            TextButton(onPressed: onDismiss, child: Text(StoreI18n.text(MessageKeys.alertOk))),
        ],
      ),
    );
  }
}
