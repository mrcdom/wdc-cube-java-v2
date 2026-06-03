import 'package:flutter/material.dart';

import '../bridge/constants.dart';
import '../bridge/view_state_coordinator.dart';
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
        if (error != null) _ConnectionAlert(error: error),
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
    final widget = coordinator.createView(vsid);
    if (widget is Widget) return widget;
    return const SizedBox.shrink();
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

    return Container(
      margin: const EdgeInsets.all(16),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.red.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.red.shade200),
      ),
      child: Row(
        children: [
          Icon(Icons.error_outline, color: Colors.red.shade700),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Aviso!',
                    style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red.shade700, fontSize: 13)),
                Text(message, style: TextStyle(color: Colors.red.shade700, fontSize: 13)),
                if (detail != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(detail, style: TextStyle(color: Colors.red.shade700, fontSize: 11)),
                  ),
              ],
            ),
          ),
          TextButton(onPressed: onDismiss, child: const Text('Ok')),
        ],
      ),
    );
  }
}

// :: Internal - ConnectionAlert

class _ConnectionAlert extends StatelessWidget {
  final Map<String, dynamic> error;

  const _ConnectionAlert({required this.error});

  @override
  Widget build(BuildContext context) {
    final delay = (error['delay'] as num?)?.toInt() ?? 0;
    final coordinator = ViewStateCoordinator.instance;

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
          bottomLeft: Radius.circular(8),
          bottomRight: Radius.circular(8),
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
                onTap: () => coordinator.connect(),
                child: Text('Tentar agora',
                    style: TextStyle(
                        color: Theme.of(context).colorScheme.primary, decoration: TextDecoration.underline, fontSize: 13)),
              ),
            ),
        ],
      ),
    );
  }
}
