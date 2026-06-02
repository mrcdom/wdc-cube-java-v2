# WDC Framework — Commons

Biblioteca de utilitários compartilhados por todos os demais módulos do framework e pela aplicação Shopping.

---

## Pacotes

| Pacote | Descrição |
|--------|-----------|
| `codec` | Codificação **Base62** |
| `concurrent` | `ScheduledExecutor` — wrapper de execução agendada |
| `convert` | `DateUtil` — conversão/formatação de datas |
| `function` | Interfaces funcionais com exceções: `ThrowingConsumer`, `ThrowingFunction`, `ThrowingRunnable`, `ThrowingSupplier` |
| `gson` | Utilitários de serialização/deserialização JSON (Gson) |
| `http` | `HttpTransport` — abstração de transporte HTTP |
| `lang` | `CoerceUtils` — conversão coerciva de tipos primitivos |
| `log` | Façade de logging multiplataforma (`Log`, `JulLogFactory`, `Slf4jLogFactory`) |
| `security` | `RSA` — operações de criptografia RSA |
| `serialization` | Serialização binária extensível: `Externalizable`, `EntityGraph`, `ExtensibleObjectInput/Output`, `JsonStream*`, `MapOrList*`, `KeyedEntity`, `SerializationToken` |
| `sql` | `SqlDataSource`, `SqlDataSourceDelegate` — abstração de conexão SQL |
| `storage` | `ClientStorage`, `InMemoryClientStorage`, `PreferencesClientStorage` — armazenamento local key-value |
| `util` | `Defer`, `HasCriteria`, `LambdaUtils`, `Rethrow`, `TransactionContext` |

---

## Destaques de Uso

### Logging multiplataforma

```java
import br.com.wdc.framework.commons.log.Log;

private static final Log LOG = Log.getLogger(MinhaClasse.class.getSimpleName());
LOG.info("mensagem");
LOG.error("falha: " + ex.getMessage());
```

### Interfaces funcionais com exceção

```java
import br.com.wdc.framework.commons.function.ThrowingConsumer;

ThrowingConsumer<String> action = value -> {
    Files.writeString(path, value); // pode lançar IOException
};
```

---

## Dependências

- Gson
- SLF4J (quando disponível, via `Slf4jLogFactory`)
- `java.util.logging` (fallback via `JulLogFactory`)

## Coordenadas Maven

```xml
<dependency>
    <groupId>br.com.wdc.framework</groupId>
    <artifactId>br.com.wdc.framework.commons</artifactId>
    <version>1.0.0</version>
</dependency>
```
