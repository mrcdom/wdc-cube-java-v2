// Smoke tests for flutter.web.
//
// The app is a WebSocket shell that connects to a remote backend — full
// integration tests require a live server. These tests cover only the widgets
// that are renderable without a network connection.
//
// Note: ShoppingApp immediately calls main() side-effects (WebSocket connect,
// browser history setup) which are not testable in the Flutter test environment.
// There are no local-only widgets in this target beyond MaterialApp itself.

void main() {
  // No unit-testable widgets without a live backend.
  // Integration tests belong in the br.com.wdc.shopping.tests module.
}
