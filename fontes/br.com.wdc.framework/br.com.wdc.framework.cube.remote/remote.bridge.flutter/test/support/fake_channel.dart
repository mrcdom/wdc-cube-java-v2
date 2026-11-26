import 'dart:async';
import 'dart:convert';

import 'package:stream_channel/stream_channel.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

/// A WebSocket channel the test drives: what the client sends is queued
/// (keep-alives left out), and the test pushes what the host says.
class FakeChannel with StreamChannelMixin<dynamic> implements WebSocketChannel {
  final StreamController<dynamic> _incoming = StreamController<dynamic>();
  final List<String> _sent = [];
  Completer<void>? _sentSignal;
  late final FakeSink _sink = FakeSink(this);
  int? _closeCode;

  /// The host's [message] arrives.
  void receive(String message) => _incoming.add(message);

  /// The host closes the connection with [code].
  void serverClose(int code) {
    _closeCode = code;
    _incoming.close();
  }

  /// Whether the client closed the connection.
  bool get closedByClient => _sink._done.isCompleted;

  void _onSent(String message) {
    if (jsonDecode(message) case {'ping': true}) return;
    _sent.add(message);
    _sentSignal?.complete();
    _sentSignal = null;
  }

  /// The next message the client sends within [timeout], or null.
  Future<String?> nextSent(Duration timeout) async {
    final deadline = DateTime.now().add(timeout);
    while (_sent.isEmpty) {
      final left = deadline.difference(DateTime.now());
      if (left <= Duration.zero) return null;
      final signal = _sentSignal ??= Completer<void>();
      await signal.future.timeout(left, onTimeout: () {});
    }
    return _sent.removeAt(0);
  }

  @override
  Stream<dynamic> get stream => _incoming.stream;

  @override
  WebSocketSink get sink => _sink;

  @override
  Future<void> get ready => Future.value();

  @override
  String? get protocol => 'wdc';

  @override
  int? get closeCode => _closeCode;

  @override
  String? get closeReason => null;
}

class FakeSink implements WebSocketSink {
  final FakeChannel _channel;
  final Completer<void> _done = Completer<void>();

  FakeSink(this._channel);

  @override
  void add(dynamic data) => _channel._onSent(data as String);

  @override
  void addError(Object error, [StackTrace? stackTrace]) {}

  @override
  Future<void> addStream(Stream<dynamic> stream) => stream.forEach(add);

  @override
  Future<void> close([int? closeCode, String? closeReason]) {
    _channel._closeCode ??= closeCode ?? 1000;
    if (!_done.isCompleted) {
      _done.complete();
      _channel._incoming.close();
    }
    return _done.future;
  }

  @override
  Future<void> get done => _done.future;
}
