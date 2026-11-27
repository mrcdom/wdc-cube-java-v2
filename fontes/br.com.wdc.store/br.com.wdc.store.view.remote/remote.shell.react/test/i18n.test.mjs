import { test } from "node:test"
import assert from "node:assert/strict"

import { languageOf, text, translate } from "../src/scripts/generated/messages.ts"
import { formatPattern } from "../src/scripts/utils/DatePattern.ts"

test("a browser tag maps to the catalog's language", () => {
  assert.equal(languageOf("pt-BR"), "pt-BR")
  assert.equal(languageOf("pt-PT"), "pt-BR")
  assert.equal(languageOf("pt"), "pt-BR")
  assert.equal(languageOf("en-GB"), "en-US")
  assert.equal(languageOf("fr-FR"), "en-US")
  assert.equal(languageOf(undefined), "en-US")
})

test("dates follow the pattern of each language", () => {
  const date = new Date(2026, 10, 5, 14, 7)
  assert.equal(formatPattern(date, text("en-US", "format.date")), "11/05/2026")
  assert.equal(formatPattern(date, text("pt-BR", "format.date")), "05/11/2026")
  assert.equal(formatPattern(date, text("en-US", "format.dateTime")), "11/05/2026 2:07 PM")
  assert.equal(formatPattern(date, text("pt-BR", "format.dateTime")), "05/11/2026 14:07")
  assert.equal(formatPattern(new Date(2026, 0, 1, 0, 30), "h:mm a"), "12:30 AM")
})

test("a wire message with args is translated", () => {
  const message = { key: "error.signin.tooManySeconds", args: { seconds: "30" } }
  assert.match(translate("en-US", message), /30 s/)
  assert.match(translate("pt-BR", message), /30/)
  assert.notEqual(translate("pt-BR", message), translate("en-US", message))
  assert.equal(translate("pt-BR", { key: "error.unexpected" }), text("pt-BR", "error.unexpected"))
  assert.equal(translate("en-US", undefined), "")
})
