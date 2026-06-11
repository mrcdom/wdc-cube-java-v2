# remote.bridge.react

Bridge TypeScript/React para o protocolo Host/Shell do **Cube MVP Remoto**. Implementa o lado cliente (shell) do protocolo no browser — conecta ao `remote.host` via WebSocket, faz o handshake de segurança RSA/AES-GCM, gerencia ViewStates e disponibiliza hooks React para os componentes de UI renderizarem estados e emitirem eventos.

É a contraparte React dos bridges Java (`remote.bridge.java`) e TeaVM (`remote.bridge.teavm`). Consumido por `remote.shell.react` (aplicação shopping) e qualquer outro shell React sobre o mesmo protocolo.

> Módulo `packaging=pom` — contém apenas código fonte TypeScript/React; não produz JAR.

## Estrutura

```
src/
  bridge/
    ViewStateCoordinator.ts   # Gerencia sessão WS, despacha eventos, distribui ViewState updates
    ViewScope.ts              # Hook/context React: expõe ViewState reativo para componentes
    DataSecurity.ts           # Handshake RSA + AES-GCM (Web Crypto API)
    FlushRequestContext.ts    # Serialização e envio de eventos ao host
    ReconnectController.ts    # Backoff progressivo em reconexão WS
    ViewGarbageCollector.ts   # Cleanup de views desalocadas
    types.ts                  # Tipos compartilhados (HostResponse, ViewStateSnapshot, ...)
    constants.ts              # Constantes de protocolo
    index.tsx                 # Entrypoint público do bridge
  utils/
    RSA.ts                    # RSA OAEP encrypt/decrypt via Web Crypto API
    Base64.ts                 # Base64 encode/decode
    UTF8.ts                   # TextEncoder/TextDecoder helpers
    BigIntUtils.ts            # Operações BigInt para criptografia RSA
    LangUtils.ts              # Utilitários gerais (throttle, etc.)
```

## Integração com um Shell React

```tsx
// Inicializa o bridge
const coordinator = new ViewStateCoordinator(wsUrl)

// Em um componente:
const viewState = ViewScope.use<LoginViewState>("login")
coordinator.submit({ eventCode: "LOGIN", formData: { username, password } })
```

## Segurança

O bridge implementa o mesmo protocolo de segurança que os bridges Java e TeaVM:

1. **Handshake**: RSA-OAEP (chave pública do servidor) para cifrar a senha AES
2. **Dados sensíveis**: AES-256-GCM por sessão, chave derivada via PBKDF2 (250 000 iterações)
3. **Assinatura de navegação**: parâmetro `sign` em cada intent, validado pelo host
