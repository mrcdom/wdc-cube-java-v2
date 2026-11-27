# 0027. One set of design tokens, generated for every view technology

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

The WDC Store draws the same screens with seven view technologies — React, Flutter, Codename One, TeaVM (teavm.web and its remote shell), SWT (local and remote) and Vaadin — and the backend's landing page is one more look. Each kept its own palette, in its own form: CSS variables, Sass variables, Dart constants, Java ints, CN1 Sass in millimeters, SWT colors with point-sized fonts, Lumo overrides. The values had drifted: the brand blue was `#0d66d0` in five of them, `#1976d2` in Vaadin and `#0077b6` on the landing page; error, success and secondary text had three or four values each. A change to the brand meant eight edits that nothing checked.

## Decision

**`tokens.json`, in the W3C Design Tokens format, is the single source**, in the module `br.com.wdc.store.design`: the palette (roles such as `primary`, `surface`, `textSecondary`, `error`, `successBackground`), spacing, radii, font sizes, weights and the family.

**The code is generated and committed.** `GenerateDesignTokens` (`br.com.wdc.store.scripts`), like the jOOQ schema, writes:

- `StoreTokens` in Java (Java 8 syntax), for SWT and the landing page, and its copy for Codename One;
- `_tokens.scss` — Sass variables and a mixin of `--store-*` custom properties — for the React shell, the TeaVM views and Codename One's theme;
- `store_tokens.dart` for Flutter;
- `tokens.css` for Vaadin, whose Lumo variables point at it.

`DesignTokensTest` fails while they don't match the JSON.

**The palette of the web shells (React, TeaVM) is the brand for all.** Where another technology differed for the same role, it adopted the token. Each technology keeps its theme file and names, now filled from the tokens. Values off the scale, the sizes of things (images, icons, control heights, borders), breakpoints and each platform's units stay in the views.

## Consequences

- A color, spacing step or type size is changed once, in `tokens.json`; running the generator and rebuilding the clients changes it everywhere.
- Vaadin and the landing page changed their blue to the brand's, and the other technologies' error, success and secondary-text colors moved to the tokens: a small, deliberate visual change.
- Design tools that speak the W3C format can read and write the file.

## Alternatives considered

- **The tokens as a Java class, with the other formats generated from it:** the source would have types, but a non-Java consumer would read a translation, and design tools could not.
- **Keep the palettes, only document the brand:** no machinery, but the drift that motivated this would continue.
- **A third-party generator (Style Dictionary):** the usual tool, but a Node toolchain inside the Maven build for a handful of outputs that fit in one class.
