import 'package:flutter/material.dart';

import '../widgets/error_banner.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

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
      return Padding(
        padding: const EdgeInsets.all(16),
        child: ErrorBanner(message: errorMessage),
      );
    }

    if (contentViewId != null) {
      return slot(contentViewId);
    }

    return const Padding(
      padding: EdgeInsets.all(16),
      child: ErrorBanner(message: 'Falta conteúdo para a página inicial'),
    );
  }
}
