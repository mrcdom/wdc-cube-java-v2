# WDC Shopping - iOS (RoboVM)

Aplicativo iOS nativo para o WDC Shopping, utilizando **MobiVM/RoboVM** para compilar Java em código nativo ARM64.

## Arquitetura

```
┌─────────────────────────────────────────────┐
│            iOS App (RoboVM)                  │
│  ┌───────────────────────────────────────┐   │
│  │  UIKit Views (Java → Native ARM64)    │   │
│  │  LoginViewRoboVM, HomeViewRoboVM...   │   │
│  └───────────────┬───────────────────────┘   │
│                  │                            │
│  ┌───────────────┴───────────────────────┐   │
│  │  Infraestrutura                       │   │
│  │  AbstractViewRoboVM, RenderLoop,      │   │
│  │  ViewSlot, ScheduledExecutorAdapter   │   │
│  └───────────────┬───────────────────────┘   │
│                  │                            │
│  ┌───────────────┴───────────────────────┐   │
│  │  Camadas Compartilhadas (JARs)        │   │
│  │  framework.commons / framework.cube   │   │
│  │  shopping.domain / presentation       │   │
│  │  shopping.api-client                  │   │
│  └───────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
         │ HTTP/REST
         ▼
   Backend Remoto (Javalin)
```

O app iOS reutiliza **100% da lógica de apresentação** (presenters, view states, navegação) e conecta-se ao backend via `api-client` REST, exatamente como o módulo Android.

## Pré-requisitos

- **macOS** com Xcode instalado (Command Line Tools)
- **Java 17+** (para compilação local do módulo)
- **Java 26** (para compilar os módulos compartilhados com perfil `android-compat`)
- **RoboVM/MobiVM 2.3.24** (Gradle plugin, baixado automaticamente)
- Módulos compartilhados em `~/.m2/repository` (mavenLocal)

## Como Compilar

### 1. Compilar dependências compartilhadas

```bash
cd fontes
./build-robovm-deps.sh
```

Isso compila `framework.commons`, `framework.cube`, `shopping.domain`, `shopping.presentation` e `shopping.api-client` com o perfil `android-compat` (Java 22, sem preview) e instala em mavenLocal.

### 2. Executar no Simulador iOS

```bash
cd fontes/br.com.wdc.shopping/br.com.wdc.shopping.view.robovm
./gradlew launchIPhoneSimulator
```

### 3. Executar em Dispositivo Real

```bash
./gradlew launchIOSDevice
```

Requer certificado de desenvolvimento Apple e perfil de provisionamento.

### 4. Gerar IPA para Distribuição

```bash
./gradlew createIPA
```

## Estrutura do Módulo

```
br.com.wdc.shopping.view.robovm/
├── build.gradle.kts          # Gradle + RoboVM plugin
├── settings.gradle.kts       # Repositórios Maven
├── gradle.properties         # Configurações JVM
├── robovm.xml                # Configuração RoboVM (main class, frameworks, archs)
├── Info.plist.xml            # Metadados do app iOS
└── src/main/java/br/com/wdc/shopping/view/robovm/
    ├── ShoppingRoboVMMain.java            # UIApplicationDelegate (entry point)
    ├── ShoppingRoboVMApplication.java     # Extends ShoppingApplication
    ├── AbstractViewRoboVM.java            # Base class para views
    ├── RoboVMRenderLoop.java              # NSTimer ~60fps render loop
    ├── RoboVMViewSlot.java                # Thread-safe view slot
    ├── ScheduledExecutorRoboVMAdapter.java # Main thread dispatch
    └── impl/
        ├── RootViewRoboVM.java
        ├── LoginViewRoboVM.java
        ├── HomeViewRoboVM.java
        ├── CartViewRoboVM.java
        ├── ProductViewRoboVM.java
        ├── ReceiptViewRoboVM.java
        ├── ProductsPanelViewRoboVM.java
        └── PurchasesPanelViewRoboVM.java
```

## Padrão de Implementação

Segue o mesmo padrão MVP do módulo Swing/Android:

1. **View factories** registradas estaticamente em `ShoppingRoboVMApplication`
2. **AbstractViewRoboVM** implementa `CubeView`, com dirty-marking para render loop
3. **RoboVMRenderLoop** usa `NSTimer` no main thread para flush periódico (~60fps)
4. **RoboVMViewSlot** gerencia referências de sub-views thread-safe
5. **ScheduledExecutorRoboVMAdapter** despacha tasks para a main thread via `NSOperationQueue`

## Sobre o RoboVM/MobiVM

- **Fork ativo**: [MobiVM/RoboVM](https://github.com/MobiVM/robovm) v2.3.24
- **Compilação AOT**: Java bytecode → LLVM IR → ARM64 nativo
- **Sem JVM em runtime**: execução nativa como app Objective-C/Swift
- **UIKit bindings**: acesso completo às APIs nativas iOS via `robovm-cocoatouch`

## Notas Importantes

- Os módulos compartilhados devem ser compilados com o perfil `android-compat` (Java 22, sem preview features) para garantir compatibilidade com o parser de class files do RoboVM.
- O RoboVM **não suporta** Swing, AWT ou JavaFX. Toda a UI é feita com UIKit nativo.
- Reflexão funciona mas pode exigir `forceLinkClasses` no `robovm.xml` para evitar remoção pelo tree shaker.
- O projeto RoboVM/MobiVM é mantido por um mantenedor principal — considere este risco para produção.

## Comparação: Android vs iOS

| Aspecto | Android | iOS (RoboVM) |
|---------|---------|--------------|
| UI | Jetpack Compose (Kotlin) | UIKit (Java) |
| Build | Gradle + AGP | Gradle + RoboVM Plugin |
| Compilação | DEX (D8/R8) | AOT via LLVM |
| Runtime | ART | Nativo ARM64 |
| Dependências | mavenLocal (android-compat) | mavenLocal (android-compat) |
| Render Loop | Choreographer | NSTimer |
| Main Thread | Main Looper | NSOperationQueue.mainQueue |
