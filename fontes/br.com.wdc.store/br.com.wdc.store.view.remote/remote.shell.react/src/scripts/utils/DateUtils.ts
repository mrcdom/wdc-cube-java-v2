function pad2(value: number) {
  return value < 10 ? "0" + value : String(value)
}

function toDate(dateOrMillis: number | Date) {
  return typeof dateOrMillis === "number" ? new Date(dateOrMillis) : dateOrMillis
}

/** Formats a date as `MM/DD/YYYY`. */
export function formatDate(dateOrMillis: number | Date | null | undefined) {
  if (dateOrMillis === undefined || dateOrMillis == null) {
    console.error("Date missing or invalid!")
    return "Date unavailable"
  }

  const date = toDate(dateOrMillis)
  // getMonth() is zero-based
  return pad2(date.getMonth() + 1) + "/" + pad2(date.getDate()) + "/" + date.getFullYear()
}

/** Formats a date-time as `MM/DD/YYYY h:mm AM|PM` (12-hour clock). */
export function formatDateTime(dateOrMillis: number | Date | null | undefined) {
  if (dateOrMillis === undefined || dateOrMillis == null) {
    return ""
  }

  const date = toDate(dateOrMillis)
  const hours = date.getHours()
  const hour12 = hours % 12 === 0 ? 12 : hours % 12
  return formatDate(date) + " " + hour12 + ":" + pad2(date.getMinutes()) + (hours < 12 ? " AM" : " PM")
}
