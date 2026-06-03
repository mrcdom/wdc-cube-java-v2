import '../bridge/view_state_coordinator.dart';

String? _cachedBaseHttpUrl;

/// Resolves a relative asset path to a full URL based on the backend host.
///
/// Example: `resolveAssetUrl('./image/product/1.png')`
/// → `http://localhost:8080/image/product/1.png`
String resolveAssetUrl(String relativePath) {
  _cachedBaseHttpUrl ??= _computeBaseHttpUrl();
  final normalized = relativePath.startsWith('./') ? relativePath.substring(2) : relativePath;
  return '$_cachedBaseHttpUrl/$normalized';
}

String _computeBaseHttpUrl() {
  final wsUri = Uri.parse(ViewStateCoordinator.instance.baseWebSocketUrl);
  final scheme = wsUri.scheme == 'wss' ? 'https' : 'http';
  return '$scheme://${wsUri.host}:${wsUri.port}';
}
