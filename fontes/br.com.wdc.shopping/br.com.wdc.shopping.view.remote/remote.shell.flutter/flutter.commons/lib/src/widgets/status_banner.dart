import 'package:flutter/material.dart';

import '../design_tokens.dart';

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
      StatusSeverity.error => (Colors.red.shade50, Colors.red.shade200, Colors.red.shade700, Icons.error_outline),
      StatusSeverity.success => (Colors.green.shade50, Colors.green.shade200, Colors.green.shade700, Icons.check_circle),
      StatusSeverity.warning => (Colors.orange.shade50, Colors.orange.shade200, Colors.orange.shade700, Icons.warning_amber),
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
          Icon(icon, color: fgColor, size: 18),
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
            TextButton(onPressed: onDismiss, child: const Text('Ok')),
        ],
      ),
    );
  }
}
