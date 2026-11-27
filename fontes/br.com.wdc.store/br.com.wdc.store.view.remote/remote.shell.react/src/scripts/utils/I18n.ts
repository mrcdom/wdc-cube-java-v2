import { type Language, type Message, type MessageKey, languageOf, text, translate } from "@root/generated/messages"

/** The language of the shell, taken once from the browser at startup. */
export const LANGUAGE: Language = languageOf(typeof navigator !== "undefined" ? navigator.language : undefined)

/** The text of a catalog key in the shell's language, each {name} replaced by its argument. */
export function t(key: MessageKey, args?: Record<string, string>): string {
  return text(LANGUAGE, key, args)
}

/** The text of a message from the presentation (`{key, args}` on the wire) in the shell's language. */
export function tm(message: Message | null | undefined): string {
  return translate(LANGUAGE, message)
}

export type { Message }
