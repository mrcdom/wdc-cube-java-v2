import 'package:flutter/widgets.dart';

import '../bridge/view_scope.dart';
import '../bridge/scope_coordinator.dart';

/// Base widget for views driven by the remote bridge.
///
/// Binds to a [ViewScope] via [vsid] and rebuilds when the server
/// pushes a new scope.
abstract class BaseView extends StatefulWidget {
  final String vsid;

  const BaseView({super.key, required this.vsid});
}

/// Base state for [BaseView] widgets.
///
/// Subclasses override [build] and read the server scope from [scope].
abstract class BaseViewState<T extends BaseView> extends State<T> {
  late final ViewScope viewScope;

  ScopeCoordinator get coordinator => ScopeCoordinator.instance;

  Map<String, dynamic> get scope => viewScope.values;

  @override
  void initState() {
    super.initState();
    viewScope = coordinator.viewMap[widget.vsid] ?? ViewScope(widget.vsid);
    coordinator.viewMap[widget.vsid] = viewScope;
    viewScope.forceUpdate = _rebuild;
    coordinator.viewGarbageCollector.mount(widget.vsid);
  }

  @override
  void dispose() {
    coordinator.viewGarbageCollector.unmount(widget.vsid);
    if (viewScope.forceUpdate == _rebuild) {
      viewScope.forceUpdate = ViewScope.noop;
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

  /// Sets a form field that travels ciphered with the connection key (e.g.
  /// `p.password`).
  void setSecretFormField(String fieldName, String value) {
    coordinator.setSecretFormField(widget.vsid, fieldName, value);
  }

  /// Creates the widget for a child view slot.
  Widget slot(String viewId) {
    return coordinator.createViewWidget(viewId);
  }
}
