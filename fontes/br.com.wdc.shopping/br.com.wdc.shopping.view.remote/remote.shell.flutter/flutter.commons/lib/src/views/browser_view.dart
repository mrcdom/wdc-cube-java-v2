import 'package:flutter/material.dart';

import '../bridge/constants.dart';
import '../design_tokens.dart';
import '../widgets/status_banner.dart';
import 'base_view.dart';

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
    final state = viewState;
    final submitting = state['submitting'] == true;
    final error = state['error'] as Map<String, dynamic>?;
    final alertId = (state['alertId'] as num?)?.toInt() ?? 0;
    final alertArgs = (state['alertArgs'] as List?)?.cast<String>() ?? const <String>[];
    final contentViewId = state['contentViewId'] as String?;

    return Column(
      children: [
        if (submitting) const LinearProgressIndicator(),
        if (error != null) _ConnectionAlert(error: error, onRetry: coordinator.connect),
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
        message = args.isNotEmpty ? args[0] : 'Erro inesperado';
        detail = args.length > 1 ? args[1] : null;
      case -2:
        message = 'A URI ${args.isNotEmpty ? args[0] : ""} não está acessível';
        detail = args.length > 1 ? args[1] : null;
      default:
        message = args.isNotEmpty ? args[0] : 'Ocorreu um erro não esperado';
    }

    return Padding(
      padding: const EdgeInsets.all(16),
      child: StatusBanner(
        title: 'Aviso!',
        message: message,
        detail: detail,
        onDismiss: onDismiss,
        severity: StatusSeverity.error,
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
      timeText = minutes > 0 ? 'Conectando em ${minutes}m e ${seconds}s...' : 'Conectando em ${seconds}s...';
      showRetry = true;
    } else {
      timeText = 'Conectando agora...';
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
          Text('Não conectado. ', style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red.shade700, fontSize: 13)),
          Text(timeText, style: TextStyle(color: Colors.red.shade700, fontSize: 13)),
          if (showRetry)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: GestureDetector(
                onTap: onRetry,
                child: Text('Tentar agora',
                    style: TextStyle(
                        color: appAccent, decoration: TextDecoration.underline, fontSize: 13)),
              ),
            ),
        ],
      ),
    );
  }
}
