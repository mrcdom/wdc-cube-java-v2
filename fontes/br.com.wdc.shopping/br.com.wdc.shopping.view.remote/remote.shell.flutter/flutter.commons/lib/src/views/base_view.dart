import 'package:flutter/widgets.dart';

import '../bridge/view_scope.dart';
import '../bridge/view_state_coordinator.dart';

/// Base widget for views driven by the remote bridge.
///
/// Binds to a [ViewScope] via [vsid] and rebuilds when the server
/// pushes new state.
abstract class BaseView extends StatefulWidget {
  final String vsid;

  const BaseView({super.key, required this.vsid});
}

/// Base state for [BaseView] widgets.
///
/// Subclasses override [build] and read state from [scope].
abstract class BaseViewState<T extends BaseView> extends State<T> {
  late final ViewScope scope;

  ViewStateCoordinator get coordinator => ViewStateCoordinator.instance;

  Map<String, dynamic> get viewState => scope.state;

  @override
  void initState() {
    super.initState();
    scope = coordinator.viewMap[widget.vsid] ?? ViewScope(widget.vsid);
    coordinator.viewMap[widget.vsid] = scope;
    scope.forceUpdate = _rebuild;
  }

  @override
  void dispose() {
    if (scope.forceUpdate == _rebuild) {
      scope.forceUpdate = () {};
    }
    super.dispose();
  }

  void _rebuild() {
    if (mounted) setState(() {});
  }

  /// Submits an action event to the server.
  void submit(int eventId) {
    coordinator.submit(widget.vsid, eventId);
  }

  /// Sets a form field value before submitting.
  void setFormField(String fieldName, dynamic value) {
    coordinator.setFormField(widget.vsid, fieldName, value);
  }
}
