# br.com.wdc.store.design — the design tokens

The WDC Store's palette, spacing, radii and type, in one file: [`src/main/tokens/tokens.json`](src/main/tokens/tokens.json), in the W3C Design Tokens format ([ADR 0027](../../../docs/adr/0027-design-tokens.md)). The palette of the web shells (React, TeaVM) is the brand for every view technology.

`GenerateDesignTokens` (`br.com.wdc.store.scripts`) writes, from it:

| File | For |
|------|-----|
| `src/main/java/br/com/wdc/store/design/StoreTokens.java` | Java (SWT, the backend's landing page): colors as `0xRRGGBB` and `_HEX`, sizes in pixels, weights, the family, `CSS_VARIABLES` |
| `remote.shell.cn1/common/.../cn1/theme/StoreTokens.java` | Codename One (the same class, Java 8) |
| `remote.shell.react/src/scss/generated/_tokens.scss`, `teavm.commons/src/main/scss/generated/_tokens.scss`, `remote.shell.cn1/common/src/main/scss/generated/_tokens.scss` | Sass: `$color-primary`, `$space-md`, ... and `@mixin store-token-variables` (`--store-*` custom properties) |
| `remote.shell.flutter/flutter.commons/lib/src/generated/store_tokens.dart` | Flutter: `StoreTokens.primary`, `StoreTokens.spaceMd`, ... |
| `view.vaadin/src/main/resources/META-INF/resources/styles/tokens.css` | Vaadin: `:root { --store-* }`, to which the Lumo variables point |

To change a color, a spacing step or a type size: edit `tokens.json`, run the generator (see the scripts README), rebuild the clients. `DesignTokensTest` fails while the generated files are stale. Each technology keeps its theme file and names, filled from the tokens; values off the scale, sizes of things, breakpoints and platform units stay in the views.
