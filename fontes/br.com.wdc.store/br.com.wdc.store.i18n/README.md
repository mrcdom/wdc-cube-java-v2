# br.com.wdc.store.i18n — the UI texts

Every text the WDC Store shows, in each language: [`src/main/catalog/<language>/*.json`](src/main/catalog), key → text with `{placeholders}`, the files of a language merged ([ADR 0028](../../../docs/adr/0028-ui-texts-in-one-catalog.md)). Languages: **en-US** (the default, the repository's) and **pt-BR**. `common.json` holds the texts every technology shares; a technology's own texts are in a file of its own (`landing.json`, ...). Keys are dotted camelCase (`login.signIn`, `error.signin.tooManySeconds`).

**The presentation gives keys; the views translate.** A scope's `errorMessage` is a [`Message`](src/main/java/br/com/wdc/store/i18n/Message.java) — a key and its arguments — and each view shows it in the user's language (`message.in(language)`); remotely it travels as `{"key": ..., "args": {...}}` and the client translates it. Dates follow the language (`format.date`, `format.dateTime`); prices stay in US dollars.

`GenerateMessages` (`br.com.wdc.store.scripts`) writes, from the catalog:

| File | For |
|------|-----|
| `src/main/java/br/com/wdc/store/i18n/StoreMessages.java` | Java: a constant per key, `language(tag)`, `text(tag, key)`, `format(tag, key, args)` (Java 8 syntax) |
| `src/main/java/br/com/wdc/store/i18n/Messages.java` | Java: one factory per key with a parameter per placeholder (`Messages.errorSigninTooManySeconds(30)`) |
| `remote.shell.cn1/common/.../cn1/i18n/StoreMessages.java` | Codename One (the same class) |
| `remote.shell.react/src/scripts/generated/messages.ts` | React: `languageOf`, `text`, `translate`, `MessageKey` |
| `remote.shell.flutter/flutter.commons/lib/src/generated/messages.dart` | Flutter: `languageOf`, `text`, `translate`, `MessageKeys` |

To add or change a text: edit the JSON of **every** language, run the generator (see the scripts README), rebuild the clients. `MessagesCatalogTest` fails while a language misses a key, a key's placeholders differ between languages, or the generated files are stale.
