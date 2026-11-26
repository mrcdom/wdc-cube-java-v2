// The bridge's side of protocol version 1 beyond the conformance fixtures:
// secret form fields, reconnection, and the refusal by close code alone.
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:remote_bridge_flutter/remote_bridge_flutter.dart';

import 'support/fake_channel.dart';

const _appId = 'lifecycle.session';
const _timeout = Duration(seconds: 2);

class _Host {
  final List<FakeChannel> channels = [];
  final List<Uri> uris = [];
  late final ScopeCoordinator coordinator;
  final InMemoryClientStorage persistent = InMemoryClientStorage();
  Uint8List? key;

  _Host() {
    coordinator = ScopeCoordinator(
      CoordinatorConfig(
        appId: _appId,
        baseWebSocketUrl: 'ws://lifecycle.invalid',
        persistentStorage: persistent,
        connectChannel: (uri, protocols) {
          uris.add(uri);
          final channel = FakeChannel();
          channels.add(channel);
          return channel;
        },
      ),
    );
  }

  FakeChannel get channel => channels.last;

  Future<Map<String, dynamic>> nextSent() async {
    final sent = await channel.nextSent(_timeout);
    expect(sent, isNotNull, reason: 'the client sent nothing');
    return jsonDecode(sent!) as Map<String, dynamic>;
  }

  /// Answers the handshake the client sent on the current channel.
  Future<void> answerHandshake() async {
    final hello = await nextSent();
    expect(hello.keys.toSet(), {'protocol', 'clientKey'});
    final pair = ChannelCrypto.newKeyPair();
    key = ChannelCrypto.deriveKey(
      pair.privateKey,
      ChannelCrypto.decodePublicKey(hello['clientKey'] as String),
      _appId,
    );
    channel.receive(
      jsonEncode({
        'protocol': 1,
        'serverKey': ChannelCrypto.encodePublicKey(pair.publicKey),
      }),
    );
    await pumpEventQueue();
  }
}

void main() {
  test(
    'a secret field made before the handshake goes out ciphered after it',
    () async {
      final host = _Host();
      host.coordinator.onStart();
      host.coordinator.setSecretFormField(
        'c677cda52d14:1',
        'p.password',
        'pa55',
      );
      host.coordinator.submit('c677cda52d14:1', 1);
      await pumpEventQueue();

      await host.answerHandshake();
      final request = await host.nextSent();
      final form = request['c677cda52d14:1'] as Map<String, dynamic>;
      final password = form['p.password'] as String;
      expect(password, isNot('pa55'));
      expect(ChannelCrypto.decipher(host.key!, password), 'pa55');
      expect(request['event'], ['$browserVsid:-1', 'c677cda52d14:1:1']);
      host.coordinator.onStop();
    },
  );

  test(
    'a reconnection agrees on a new key and ciphers the bootstrap storage with it',
    () async {
      final host = _Host();
      host.persistent.set('authToken', 'tok-1');
      host.coordinator.onStart();
      await pumpEventQueue();
      await host.answerHandshake();
      final firstKey = host.key!;
      final first = await host.nextSent();
      final firstToken =
          (first['storage'] as Map)['persistent']['authToken'] as String;
      expect(ChannelCrypto.decipher(firstKey, firstToken), 'tok-1');

      host.channel.serverClose(1006);
      await pumpEventQueue();
      expect(host.coordinator.isConnected, isFalse);
      host.coordinator.connect();
      await pumpEventQueue();
      expect(host.channels, hasLength(2));

      await host.answerHandshake();
      expect(host.key, isNot(firstKey));
      final bootstrap = await host.nextSent();
      expect(bootstrap['event'], isEmpty);
      final token =
          (bootstrap['storage'] as Map)['persistent']['authToken'] as String;
      expect(ChannelCrypto.decipher(host.key!, token), 'tok-1');
      expect(
        () => ChannelCrypto.decipher(firstKey, token),
        throwsArgumentError,
      );
      host.coordinator.onStop();
    },
  );

  test(
    'close 4002 alone marks the app out of date, with no reconnection',
    () async {
      final host = _Host();
      var outdated = 0;
      host.coordinator.onOutdated = () => outdated++;
      host.coordinator.onStart();
      await pumpEventQueue();
      await host.nextSent();

      host.channel.serverClose(closeUnsupportedProtocol);
      await pumpEventQueue();
      expect(host.coordinator.isOutdated, isTrue);
      expect(outdated, 1);
      expect(host.coordinator.viewMap[browserVsid]!.values['outdated'], isTrue);
      host.coordinator.connect();
      await pumpEventQueue();
      expect(host.channels, hasLength(1));
      host.coordinator.onStop();
    },
  );

  test('after close 4001, the shell moves to a new session id and the next connection opens on it', () async {
    final host = _Host();
    host.coordinator.onSessionInvalid = () {
      host.coordinator.useSession('second.session');
      host.coordinator.onStop();
      host.coordinator.onStart();
    };
    host.coordinator.onStart();
    await pumpEventQueue();
    await host.nextSent();

    host.channel.serverClose(closeReloadRequired);
    await pumpEventQueue();

    expect(host.coordinator.id, 'second.session');
    expect(host.uris.last.path, '/dispatcher/second.session');
    expect(host.uris.first.path, '/dispatcher/$_appId');
    final hello = await host.nextSent();
    expect(hello.keys.toSet(), {'protocol', 'clientKey'});
    host.coordinator.onStop();
  });

  test('a server key that is not a P-256 point drops the connection', () async {
    final host = _Host();
    host.coordinator.onStart();
    await pumpEventQueue();
    await host.nextSent();
    host.channel.receive(jsonEncode({'protocol': 1, 'serverKey': 'AAAA'}));
    await pumpEventQueue();
    expect(host.coordinator.isConnected, isFalse);
    expect(host.coordinator.dataSecurity.isReady, isFalse);
    expect(host.channel.closedByClient, isTrue);
    expect(
      await host.channel.nextSent(const Duration(milliseconds: 200)),
      isNull,
    );
    host.coordinator.onStop();
  });
}
