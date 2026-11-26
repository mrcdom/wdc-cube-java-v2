// Runs the client fixtures of the remote protocol (../conformance/client/*.json,
// described in ../conformance/README.md) against the bridge, over a fake
// WebSocket channel the test controls.
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:pointycastle/export.dart' show ECPrivateKey;
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

import 'support/fake_channel.dart';

const _appId = 'conformance.session';
const _quietPeriod = Duration(milliseconds: 300);
const _expectTimeout = Duration(seconds: 2);

void main() {
  final fixtures =
      Directory('../conformance/client')
          .listSync()
          .whereType<File>()
          .where((f) => f.path.endsWith('.json'))
          .toList()
        ..sort((a, b) => a.path.compareTo(b.path));

  test('the client fixtures are there', () {
    expect(fixtures, isNotEmpty);
  });

  for (final file in fixtures) {
    final fixture = jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
    final name = file.uri.pathSegments.last;
    test('$name: ${fixture['description']}', () async {
      final runner = _Runner();
      try {
        for (final step in fixture['steps'] as List<dynamic>) {
          await runner.run(step as Map<String, dynamic>);
        }
      } finally {
        runner.stop();
      }
    });
  }
}

class _Runner {
  final FakeChannel channel = FakeChannel();
  late final ScopeCoordinator coordinator;
  final InMemoryClientStorage sessionStorage = InMemoryClientStorage();
  final InMemoryClientStorage persistentStorage = InMemoryClientStorage();

  /// The client's handshake key, once sent.
  String? clientKey;

  /// The connection key, once the runner answered the handshake.
  Uint8List? connectionKey;

  _Runner() {
    coordinator = ScopeCoordinator(
      CoordinatorConfig(
        appId: _appId,
        baseWebSocketUrl: 'ws://conformance.invalid',
        sessionStorage: sessionStorage,
        persistentStorage: persistentStorage,
        connectChannel: (uri, protocols) => channel,
      ),
    );
  }

  Future<void> run(Map<String, dynamic> step) async {
    if (step.containsKey('start')) {
      coordinator.onStart();
      await pumpEventQueue();
    } else if (step.containsKey('expectSent')) {
      final sent = await channel.nextSent(_expectTimeout);
      expect(sent, isNotNull, reason: 'the client sent nothing');
      final message = jsonDecode(sent!) as Map<String, dynamic>;
      if (message.containsKey('clientKey')) {
        clientKey = message['clientKey'] as String?;
      }
      _expectMatch(step['expectSent'], message, r'$');
    } else if (step.containsKey('expectNothingSent')) {
      final sent = await channel.nextSent(_quietPeriod);
      expect(sent, isNull, reason: 'the client sent $sent');
    } else if (step.containsKey('receive')) {
      channel.receive(jsonEncode(_resolve(step['receive'])));
      await pumpEventQueue();
    } else if (step.containsKey('expectOutdated')) {
      expect(coordinator.isOutdated, step['expectOutdated']);
      expect(
        coordinator.viewMap[browserVsid]?.values['outdated'],
        step['expectOutdated'] == true ? isTrue : isNot(isTrue),
      );
    } else if (step.containsKey('expectStorage')) {
      final expected = step['expectStorage'] as Map<String, dynamic>;
      final storage = switch (expected['scope']) {
        'session' => sessionStorage,
        'persistent' => persistentStorage,
        'persistent-secure' => persistentStorage.secure,
        final other => throw ArgumentError('Unknown storage scope $other'),
      };
      expect(storage.get(expected['key'] as String), expected['value']);
    } else {
      throw ArgumentError('Unknown step $step');
    }
  }

  void stop() {
    coordinator.onStop();
  }

  /// What the runner sends, with `$serverKey` and `$cipher:<text>` replaced.
  Object? _resolve(Object? value) {
    if (value is Map) {
      return {
        for (final e in value.entries) e.key as String: _resolve(e.value),
      };
    }
    if (value is List) {
      return [for (final item in value) _resolve(item)];
    }
    if (value == r'$serverKey') {
      final pair = ChannelCrypto.newKeyPair();
      final ECPrivateKey own = pair.privateKey;
      connectionKey = ChannelCrypto.deriveKey(
        own,
        ChannelCrypto.decodePublicKey(clientKey!),
        _appId,
      );
      return ChannelCrypto.encodePublicKey(pair.publicKey);
    }
    if (value is String && value.startsWith(r'$cipher:')) {
      return ChannelCrypto.cipher(
        connectionKey!,
        value.substring(r'$cipher:'.length),
      );
    }
    return value;
  }
}

/// The matching rules of conformance/README.md.
void _expectMatch(Object? expected, Object? actual, String path) {
  if (expected is Map && expected.containsKey(r'$contains')) {
    expect(actual, isA<List<dynamic>>(), reason: path);
    final wanted = expected[r'$contains'];
    final found = (actual as List).any((item) {
      try {
        _expectMatch(wanted, item, path);
        return true;
      } on TestFailure {
        return false;
      }
    });
    expect(found, isTrue, reason: '$path has no element matching $wanted');
  } else if (expected is Map) {
    expect(actual, isA<Map<String, dynamic>>(), reason: path);
    final actualMap = actual as Map<String, dynamic>;
    for (final entry in expected.entries) {
      final field = '$path.${entry.key}';
      if (entry.value == r'$absent') {
        expect(
          actualMap.containsKey(entry.key),
          isFalse,
          reason: '$field is present',
        );
      } else {
        expect(
          actualMap.containsKey(entry.key),
          isTrue,
          reason: '$field is absent',
        );
        _expectMatch(entry.value, actualMap[entry.key], field);
      }
    }
  } else if (expected is List) {
    expect(actual, isA<List<dynamic>>(), reason: path);
    final actualList = actual as List;
    expect(actualList.length, expected.length, reason: '$path length');
    for (var i = 0; i < expected.length; i++) {
      _expectMatch(expected[i], actualList[i], '$path[$i]');
    }
  } else if (expected == r'$any') {
    // Any present value.
  } else if (expected == r'$string') {
    expect(actual, isA<String>(), reason: path);
  } else if (expected == r'$number') {
    expect(actual, isA<num>(), reason: path);
  } else {
    expect(actual, expected, reason: path);
  }
}
