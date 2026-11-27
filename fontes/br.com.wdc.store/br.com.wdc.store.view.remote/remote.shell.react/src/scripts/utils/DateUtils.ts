import { t } from "@root/utils/I18n"
import { formatPattern } from "@root/utils/DatePattern"

function toDate(dateOrMillis: number | Date) {
  return typeof dateOrMillis === "number" ? new Date(dateOrMillis) : dateOrMillis
}

/** Formats a date with the catalog's `format.date` pattern of the shell's language. */
export function formatDate(dateOrMillis: number | Date | null | undefined) {
  if (dateOrMillis === undefined || dateOrMillis == null) {
    console.error("Date missing or invalid!")
    return t("format.dateUnavailable")
  }
  return formatPattern(toDate(dateOrMillis), t("format.date"))
}

/** Formats a date-time with the catalog's `format.dateTime` pattern of the shell's language. */
export function formatDateTime(dateOrMillis: number | Date | null | undefined) {
  if (dateOrMillis === undefined || dateOrMillis == null) {
    return ""
  }
  return formatPattern(toDate(dateOrMillis), t("format.dateTime"))
}
