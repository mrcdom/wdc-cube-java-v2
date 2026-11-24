/// Holds the scope of a single remote view: its [values]
/// mirror the JSON map pushed by the server.
class ViewScope {
  static void noop() {}

  final String vsid;
  final Map<String, dynamic> _values = {};

  /// Callback to trigger UI rebuild (set by the widget layer).
  void Function() forceUpdate = noop;

  ViewScope(this.vsid);

  Map<String, dynamic> get values => _values;

  /// Replaces all values with [newValues] and notifies listeners.
  void setValues(Map<String, dynamic> newValues) {
    _values.clear();
    _values.addAll(newValues);
    forceUpdate();
  }

  /// Sets a single field and notifies listeners (partial update).
  void patchField(String key, dynamic value) {
    _values[key] = value;
    forceUpdate();
  }

  /// Removes a single field and notifies listeners.
  void removeField(String key) {
    _values.remove(key);
    forceUpdate();
  }

  // Convenience typed accessors.
  String? getString(String key) => _values[key] as String?;
  int getInt(String key) => (_values[key] as num?)?.toInt() ?? 0;
  bool getBool(String key) => (_values[key] as bool?) ?? false;
  dynamic operator [](String key) => _values[key];
}

