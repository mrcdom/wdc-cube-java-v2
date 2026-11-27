# 0028. The UI texts in one catalog per language, translated by the views from keys

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

The WDC Store's texts were written in each view technology, in English, and in the presenters: about twenty error messages that the presenters put, ready to show, in each scope's `errorMessage`. Wording drifted between technologies ("Add" or "Add to Cart", "Invalid username or password." or "Username or password not recognized!"), and two login views ignored the presenter's message and showed their own. There was no way to show the store in another language, and nothing to check that a text existed everywhere it was needed.

## Decision

**One catalog per language, with the code generated from it**, like the design tokens ([0027](0027-design-tokens.md)). The module `br.com.wdc.store.i18n` holds `src/main/catalog/<language>/*.json` — key → text with `{placeholders}`, the files of a language merged — and `GenerateMessages` writes:

- `StoreMessages` (Java 8 syntax: the texts, the language chosen from a tag, the formatting) and `Messages` (one factory per key, a parameter per placeholder, so a message that lacks an argument does not compile), plus the copy of `StoreMessages` for Codename One;
- `messages.ts` for the React shell and `messages.dart` for the Flutter shell.

`MessagesCatalogTest` fails while a language misses a key, a key's placeholders differ between languages, or the generated files are stale.

**The presentation gives keys; the views translate.** `errorMessage` is a `Message` (a key and its arguments), not a text. Each view shows it in the user's language — the device's, the browser's, the JVM's — and the remote views do so on the client: the message travels as `{"key", "args"}`, so the server does not need to know the user's language. An unexpected error shows its cause through `error.unexpectedWithCause`; the code's own description of what failed goes to the log only. The remote host's failures to handle a client message go through the application too (`RemoteApplication.alertUnexpectedError`), so they reach the user the same way.

**Two languages:** en-US, the default and the repository's language, and pt-BR, which proves the catalog complete. Dates follow the language's pattern (`format.date`, `format.dateTime`). Prices stay in US dollars in every language: the currency is a fact of the data, not of the language. Where the technologies worded the same thing differently, the React shell's wording won.

## Consequences

- A text is changed in one place, in every language, for every view.
- A device or browser in Portuguese shows the store in Portuguese, with dates as DD/MM/YYYY; the backend's landing page follows `Accept-Language`.
- The presentation stays free of the language.
- The framework's own default alert for an application that does not override it stays in English: translating it would mean a catalog in the published framework. The causes of unexpected errors (exception messages) are shown as they come.

## Alternatives considered

- **Each platform's own resources** (`strings.xml`, ARB files, resource bundles, JSON for the web): idiomatic on each platform, but the same text repeated in seven places, with nothing checking that they agree.
- **The presenter translates**, knowing the user's locale: simpler views, but the language of the remote views would be decided on the server, and the presentation would carry every language.
- **English only, with the catalog ready:** less work, but nothing would show the catalog is complete.
