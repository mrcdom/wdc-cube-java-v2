import 'package:flutter/material.dart';

import '../bridge/view_state_coordinator.dart';
import 'base_view.dart';

/// RootView — shows the content view pushed by the server,
/// or an error if something went wrong.
class RootView extends BaseView {
  const RootView({super.key, required super.vsid});

  static const viewId = 'f2d345c4a610';

  @override
  State<RootView> createState() => _RootViewState();
}

class _RootViewState extends BaseViewState<RootView> {
  @override
  Widget build(BuildContext context) {
    final state = viewState;
    final errorMessage = state['errorMessage'] as String?;
    final contentViewId = state['contentViewId'] as String?;

    if (errorMessage != null) {
      return _ErrorMessage(message: errorMessage);
    }

    if (contentViewId != null) {
      final view = ViewStateCoordinator.instance.createView(contentViewId);
      if (view is Widget) return view;
    }

    return const _ErrorMessage(message: 'Falta conteúdo para a página inicial');
  }
}

class _ErrorMessage extends StatelessWidget {
  final String message;
  const _ErrorMessage({required this.message});

  @override
  Widget build(BuildContext context) {
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
          Expanded(child: Text(message, style: TextStyle(color: Colors.red.shade700))),
        ],
      ),
    );
  }
}
