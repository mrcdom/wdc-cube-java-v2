# 🛒 WeDoCode Shopping

Um **sistema de e-commerce completo** construído com arquitetura **Cube MVP**, demonstrando como a mesma lógica de negócio pode alimentar interfaces totalmente diferentes — **React (web)**, **Vaadin (web server-side)**, **Swing (desktop)**, **Gluon (desktop/iOS/Android)** e **TeaVM (web/desktop/Android/iOS)** — sem duplicar uma única linha de código de apresentação.

> **Cinco frontends. Mesma alma.**

---

## O que você vai encontrar

✅ Catálogo de produtos com imagens e descrições  
✅ Carrinho de compras com adição, remoção e cálculo de totais  
✅ Fluxo completo de compra com recibo  
✅ Histórico de compras paginado  
✅ Autenticação segura (RSA + AES-GCM na versão web)  
✅ Banco H2 embarcado — zero configuração para rodar  
✅ Virtual Threads (Java 21+) — servidor web ultra-leve  
✅ Fat JAR (~11 MB) — deploy trivial

---

## Rode em 3 comandos

```bash
# 1. Clone
git clone https://github.com/mrcdom/wdc-cube-java-v2.git
cd wdc-cube-java-v2/fontes

# 2. Build (requer Java 21 + Maven 3.9+)
export JAVA_HOME=<caminho-para-jdk-21>  # ex: /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
mvn clean package -DskipTests

# 3. Execute
java -jar \
  br.com.wdc.shopping/br.com.wdc.shopping/br.com.wdc.shopping.backend/target/br.com.wdc.shopping.backend-1.0.0.jar
```

Abra **http://localhost:8080** e entre com `admin` / `admin`.

---

## Ou rode a versão Gluon (Desktop / iOS / Android)

```bash
# Desktop (JVM)
cd br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/br.com.wdc.shopping.view.gluon.desktop
mvn javafx:run

# iOS (simulador)
cd br.com.wdc.shopping/br.com.wdc.shopping.view.gluon/br.com.wdc.shopping.view.gluon.ios
./build-sim.sh
```

Mesma aplicação, mesmo banco, mesma lógica — rodando nativamente em Desktop, iOS e Android via GraalVM.

---

## Ou rode a versão Web Server-Side (Vaadin)

```bash
export JAVA_HOME=<caminho-para-jdk-21>
cd br.com.wdc.shopping/br.com.wdc.shopping.view.vaadin
java -cp "$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout):target/classes" \
  br.com.wdc.shopping.view.vaadin.ShoppingVaadinMain
```

Abra **http://localhost:8090**. UI inteiramente server-side com Vaadin 24 + Lumo theme. Mesmos presenters, mesmo domínio.

---

## Ou rode a versão Desktop (Swing + FlatLaf)

```bash
export JAVA_HOME=<caminho-para-jdk-21>
cd br.com.wdc.shopping/br.com.wdc.shopping.view.swing
java -cp "$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout):target/classes" \
  br.com.wdc.shopping.view.swing.ShoppingSwingMain
```

Aplicação desktop com Java Swing + FlatLaf (Material look-and-feel). Mesmos presenters, mesmo domínio, banco H2 embarcado.

---

## Ou rode a versão TeaVM (Web / Desktop / Android / iOS)

```bash
# Build TeaVM (compila Java → JavaScript)
cd br.com.wdc.shopping/br.com.wdc.shopping.view.teavm
bash build.sh

# Desktop nativo (Tauri)
cd br.com.wdc.shopping.view.teavm.native
cargo tauri dev
```

Java compilado para JavaScript via TeaVM, empacotado como app nativo com Tauri 2. Mesmos presenters, mesmo domínio — rodando em 4 plataformas.

---

## Módulos

| Módulo | Responsabilidade |
|--------|-----------------|
| [`domain`](br.com.wdc.shopping.domain/) | Modelos, repositórios (interfaces), critérios, exceções |
| [`persistence`](br.com.wdc.shopping.persistence/) | Implementações SQL (JDBI + Command Pattern), DDL, DSL |
| [`presentation`](br.com.wdc.shopping.presentation/) | Presenters hierárquicos, ViewStates, serviços, navegação |
| [`scripts`](br.com.wdc.shopping.scripts/) | Scripts DDL (DBCreate, DBReset) |
| [`view.react`](br.com.wdc.shopping.view.react/) | Frontend web completo (React + Javalin + WebSocket) |
| [`view.vaadin`](br.com.wdc.shopping.view.vaadin/) | Frontend web server-side (Vaadin 24 + Jetty 12 + Lumo theme) |
| [`view.swing`](br.com.wdc.shopping.view.swing/) | Frontend desktop (Java Swing + FlatLaf Material) |
| [`view.gluon`](br.com.wdc.shopping.view.gluon/) | Frontend multiplataforma (JavaFX + Gluon — Desktop, iOS, Android) |
| [`view.teavm`](br.com.wdc.shopping.view.teavm/) | Frontend multiplataforma (TeaVM + Tauri — Web, Desktop, Android, iOS) |
| [`persistence.rest`](br.com.wdc.shopping.persistence.rest/) | Controllers REST (Javalin) para expor repositórios via HTTP |
| [`persistence.client`](br.com.wdc.shopping.persistence.client/) | Client REST (OkHttp + Gson) que implementa repositórios via HTTP |
| [`tests`](br.com.wdc.shopping.tests/) | Testes de workflow e repositórios |

---

## Por que explorar este projeto?

- **Sem Spring, sem CDI, sem magia** — injeção via `AtomicReference<T> BEAN`, 100% explícito
- **Separação real de concerns** — troque a UI inteira sem tocar em lógica (múltiplos frontends provam isso)
- **Padrões sólidos** — Command, Repository, Presenter, ViewState
- **Tecnologia moderna** — Java 21, Virtual Threads, React 19, Vaadin 24, TypeScript
- **Código limpo** — ~3.5s de build completo, zero warnings
- **Segurança real** — RSA + PBKDF2 + AES-GCM (não apenas demonstrativo)

---

## Licença

[MIT License](https://github.com/mrcdom/wdc-cube-java-v2/blob/main/LICENSE) — use, modifique, distribua.

Copyright (c) 2026 Marcelo Domingos / WeDoCode Consultoria LTDA
