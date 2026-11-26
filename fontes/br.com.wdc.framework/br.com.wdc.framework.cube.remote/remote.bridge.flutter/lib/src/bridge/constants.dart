const String browserVid = '7b32e816a191';
const String browserVsid = '$browserVid:0';

/// Keepalive interval to prevent "Idle Timeout" errors (15 seconds).
const Duration keepAliveInterval = Duration(seconds: 15);

/// Close code: invalid/forged `appId`, or a handshake with an invalid key.
const int closeReloadRequired = 4001;

/// Close code: the handshake did not carry a protocol version the host speaks.
const int closeUnsupportedProtocol = 4002;
