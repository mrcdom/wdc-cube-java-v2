/// Represents the state scope of a single remote view,
/// mirroring the JSON map pushed by the server.
class ViewScope {
  static void noop() {}

  final String vsid;
  final Map<String, dynamic> _state = {};

  /// Callback to trigger UI rebuild (set by the widget layer).
  void Function() forceUpdate = noop;

  ViewScope(this.vsid);

  Map<String, dynamic> get state => _state;

  /// Replaces the entire state with [newState] and notifies listeners.
  void setState(Map<String, dynamic> newState) {
    _state.clear();
    _state.addAll(newState);
    forceUpdate();
  }

  // Convenience typed accessors.
  String? getString(String key) => _state[key] as String?;
  int getInt(String key) => (_state[key] as num?)?.toInt() ?? 0;
  bool getBool(String key) => (_state[key] as bool?) ?? false;
  dynamic operator [](String key) => _state[key];
}

