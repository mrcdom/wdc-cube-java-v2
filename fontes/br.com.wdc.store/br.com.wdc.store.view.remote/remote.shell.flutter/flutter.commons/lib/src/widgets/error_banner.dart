import 'package:flutter/material.dart';

import 'status_banner.dart';

/// Convenience error banner — delegates to [StatusBanner] with error severity.
class ErrorBanner extends StatelessWidget {
  final String message;

  const ErrorBanner({super.key, required this.message});

  @override
  Widget build(BuildContext context) {
    return StatusBanner(message: message, severity: StatusSeverity.error);
  }
}
