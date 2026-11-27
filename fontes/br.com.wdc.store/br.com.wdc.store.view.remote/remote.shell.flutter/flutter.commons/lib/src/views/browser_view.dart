import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';

import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';
import '../design_tokens.dart';
import '../generated/messages.dart' show MessageKeys;
import '../i18n.dart';
import '../generated/store_tokens.dart';
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

  /// The host's alert of an unexpected error; its argument, when present, is the cause's message.
  static const _alertUnexpectedError = -1;

  @override
  Widget build(BuildContext context) {
    final first = args.isNotEmpty && args[0].isNotEmpty ? args[0] : null;
    final String message;
    if (code == _alertUnexpectedError) {
      message = first != null
          ? StoreI18n.text(MessageKeys.errorUnexpectedWithCause, {'cause': first})
          : StoreI18n.text(MessageKeys.errorUnexpected);
    } else {
      // an application's own alert brings its text as its first argument
      message = first ?? StoreI18n.text(MessageKeys.errorUnexpected);
    }

    return Padding(
      padding: const EdgeInsets.all(16),
      child: StatusBanner(
        title: StoreI18n.text(MessageKeys.alertTitle),
        message: message,
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
    return Padding(
      padding: const EdgeInsets.all(16),
      child: StatusBanner(
        message: StoreI18n.text(kIsWeb ? MessageKeys.connectionOutdatedPage : MessageKeys.connectionOutdatedApp),
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
      timeText = minutes > 0
          ? StoreI18n.text(MessageKeys.connectionReconnectingInMinutes, {'minutes': '$minutes', 'seconds': '$seconds'})
          : StoreI18n.text(MessageKeys.connectionReconnectingInSeconds, {'seconds': '$seconds'});
      showRetry = true;
    } else {
      timeText = StoreI18n.text(MessageKeys.connectionReconnectingNow);
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: StoreTokens.errorBackground,
        borderRadius: const BorderRadius.only(
          bottomLeft: Radius.circular(radiusSm),
          bottomRight: Radius.circular(radiusSm),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text('${StoreI18n.text(MessageKeys.connectionNotConnected)} ',
              style: const TextStyle(fontWeight: FontWeight.bold, color: StoreTokens.errorText, fontSize: 13)),
          Text(timeText, style: const TextStyle(color: StoreTokens.errorText, fontSize: 13)),
          if (showRetry)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: GestureDetector(
                onTap: onRetry,
                child: Text(StoreI18n.text(MessageKeys.connectionRetryNow),
                    style: const TextStyle(
                        color: appAccent, decoration: TextDecoration.underline, fontSize: 13)),
              ),
            ),
        ],
      ),
    );
  }
}
