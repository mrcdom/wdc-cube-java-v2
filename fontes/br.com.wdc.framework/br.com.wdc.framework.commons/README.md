# WDC Framework — Commons

Utility library shared by all the other framework modules and by the WDC Store application.

---

## Packages

| Package | Description |
|---------|-------------|
| `codec` | **Base62** encoding |
| `concurrent` | `ScheduledExecutor` — scheduled execution wrapper |
| `convert` | `DateUtil` — date conversion/formatting |
| `function` | Functional interfaces with exceptions: `ThrowingConsumer`, `ThrowingFunction`, `ThrowingRunnable`, `ThrowingSupplier` |
| `gson` | JSON serialization/deserialization utilities (Gson) |
| `http` | `HttpTransport` — HTTP transport abstraction |
| `lang` | `CoerceUtils` — coercive conversion of primitive types |
| `log` | Cross-platform logging facade (`Log`, `JulLogFactory`, `Slf4jLogFactory`) |
| `security` | `ChannelCrypto` — the remote channel's primitives: ECDH P-256, HKDF-SHA256, AES-256-GCM, HMAC-SHA256 |
| `serialization` | Extensible binary serialization: `Externalizable`, `EntityGraph`, `ExtensibleObjectInput/Output`, `JsonStream*`, `MapOrList*`, `KeyedEntity`, `SerializationToken` |
| `sql` | `SqlDataSource`, `SqlDataSourceDelegate` — SQL connection abstraction |
| `storage` | `ClientStorage`, `InMemoryClientStorage`, `PreferencesClientStorage`, `EncryptedPreferencesClientStorage` — local key-value storage with AES-256-GCM encryption support |
| `util` | `Defer`, `HasCriteria`, `HasSlice`, `LambdaUtils`, `Rethrow` |

---

## Usage Highlights

### Cross-platform logging

```java
import br.com.wdc.framework.commons.log.Log;

private static final Log LOG = Log.getLogger(MyClass.class.getSimpleName());
LOG.info("message");
LOG.error("failure: " + ex.getMessage());
```

### Functional interfaces with exceptions

```java
import br.com.wdc.framework.commons.function.ThrowingConsumer;

ThrowingConsumer<String> action = value -> {
    Files.writeString(path, value); // may throw IOException
};
```

---

## Dependencies

- Gson
- SLF4J (when available, via `Slf4jLogFactory`)
- `java.util.logging` (fallback via `JulLogFactory`)

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.mrcdom.wdc.java</groupId>
    <artifactId>framework-commons</artifactId>
    <version>2.0.0</version>
</dependency>
```
