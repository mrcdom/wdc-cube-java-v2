import 'view_state_coordinator.dart';

/// Server-authoritative view garbage collector.
///
/// The server periodically informs which views are active.
/// The client removes from viewMap any view that:
///   1. Is NOT in the server's active list
///   2. Is NOT currently mounted (widget alive)
class ViewGarbageCollector {
  final ViewStateCoordinator app;
  final Set<String> _mountedViews = {};

  ViewGarbageCollector(this.app);

  void mount(String vsid) {
    _mountedViews.add(vsid);
  }

  void unmount(String vsid) {
    _mountedViews.remove(vsid);
  }

  /// Removes specific views released by the server (eager GC, ~15s).
  /// Only removes if not currently mounted.
  void release(List<dynamic> releasedViews) {
    for (final vsid in releasedViews) {
      if (vsid is String && !_mountedViews.contains(vsid)) {
        app.viewMap.remove(vsid);
      }
    }
  }

  /// Full reconciliation: removes any view not listed by the server
  /// and not mounted (full sweep, ~5min).
  void sweep(List<dynamic> activeViews) {
    final serverActive = activeViews.whereType<String>().toSet();
    app.viewMap.removeWhere(
      (vsid, _) => !serverActive.contains(vsid) && !_mountedViews.contains(vsid),
    );
  }
}
