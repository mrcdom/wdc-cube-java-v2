import 'package:flutter/material.dart';

import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';
import '../design_tokens.dart';
import '../widgets/status_banner.dart';


/// Actions
const _onAlertOk = 1;

/// The top-level browser view that manages WebSocket lifecycle,
/// connection errors, alerts, and delegates to the root content view.
class BrowserView extends BaseView {
  const BrowserView({super.key}) : super(vsid: browserVsid);

  static const viewId = '7b32e816a191';

  @override
  State<BrowserView> createState() => _BrowserViewState();
}

class _BrowserViewState extends BaseViewState<BrowserView> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      coordinator.onStart();
    });
  }

  @override
  void dispose() {
    coordinator.onStop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final submitting = scope['submitting'] == true;
    final error = scope['error'] as Map<String, dynamic>?;
    final outdated = scope['outdated'] == true;
    final alertId = (scope['alertId'] as num?)?.toInt() ?? 0;
    final alertArgs = (scope['alertArgs'] as List?)?.cast<String>() ?? const <String>[];
    final contentViewId = scope['contentViewId'] as String?;

    return Column(
      children: [
        if (submitting) const LinearProgressIndicator(),
        if (outdated) const _OutdatedAlert(),
        if (error != null && !outdated) _ConnectionAlert(error: error, onRetry: coordinator.connect),
        if (alertId != 0)
          _AppAlert(
            code: alertId,
            args: alertArgs,
            onDismiss: _emitAlertOk,
          ),
        Expanded(child: _buildContent(contentViewId)),
      ],
    );
  }

  Widget _buildContent(String? contentViewId) {
    if (contentViewId != null) {
      return _createView(contentViewId);
    }
    return const Center(child: CircularProgressIndicator());
  }

  Widget _createView(String vsid) {
    return slot(vsid);
  }

  void _emitAlertOk() {
    submit(_onAlertOk);
  }
}

// :: Internal - AppAlert

class _AppAlert extends StatelessWidget {
  final int code;
  final List<String> args;
  final VoidCallback? onDismiss;

  const _AppAlert({required this.code, required this.args, this.onDismiss});

  @override
  Widget build(BuildContext context) {
    String message;
    String? detail;
    switch (code) {
      case -1:
        message = args.isNotEmpty ? args[0] : 'Unexpected error';
        detail = args.length > 1 ? args[1] : null;
      case -2:
        message = 'The URI ${args.isNotEmpty ? args[0] : ""} is not accessible';
        detail = args.length > 1 ? args[1] : null;
      default:
        message = args.isNotEmpty ? args[0] : 'An unexpected error occurred';
    }

    return Padding(
      padding: const EdgeInsets.all(16),
      child: StatusBanner(
        title: 'Warning!',
        message: message,
        detail: detail,
        onDismiss: onDismiss,
        severity: StatusSeverity.error,
      ),
    );
  }
}

// :: Internal - OutdatedAlert

/// The host does not speak this client's protocol version: the page (or app)
/// is out of date, and the client does not reconnect.
class _OutdatedAlert extends StatelessWidget {
  const _OutdatedAlert();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.all(16),
      child: StatusBanner(
        title: 'This version is out of date.',
        message: 'The server no longer speaks this version of the app. Reload the page, or update the app, to continue.',
        severity: StatusSeverity.warning,
      ),
    );
  }
}

// :: Internal - ConnectionAlert

class _ConnectionAlert extends StatelessWidget {
  final Map<String, dynamic> error;
  final VoidCallback onRetry;

  const _ConnectionAlert({required this.error, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final delay = (error['delay'] as num?)?.toInt() ?? 0;

    String timeText;
    bool showRetry = false;
    if (delay > 0) {
      var seconds = delay ~/ 1000;
      var minutes = 0;
      if (seconds > 60) {
        minutes = seconds ~/ 60;
        seconds = seconds - minutes * 60;
      }
      timeText = minutes > 0 ? 'Reconnecting in ${minutes}m ${seconds}s...' : 'Reconnecting in ${seconds}s...';
      showRetry = true;
    } else {
      timeText = 'Reconnecting now...';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.red.shade50,
        borderRadius: const BorderRadius.only(
          bottomLeft: Radius.circular(radiusSm),
          bottomRight: Radius.circular(radiusSm),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text('Not connected. ', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red.shade700, fontSize: 13)),
          Text(timeText, style: TextStyle(color: Colors.red.shade700, fontSize: 13)),
          if (showRetry)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: GestureDetector(
                onTap: onRetry,
                child: Text('Retry now',
                    style: TextStyle(
                        color: appAccent, decoration: TextDecoration.underline, fontSize: 13)),
              ),
            ),
        ],
      ),
    );
  }
}
