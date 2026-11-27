const TOKENS = /yyyy|MM|dd|HH|h|mm|a/g

function pad2(value: number) {
  return value < 10 ? "0" + value : String(value)
}

/**
 * Formats a date with a catalog pattern: `yyyy` year, `MM` month, `dd` day, `HH` 24-hour, `h` 12-hour, `mm` minutes,
 * `a` AM/PM; any other character is kept as it is.
 */
export function formatPattern(date: Date, pattern: string): string {
  return pattern.replace(TOKENS, (token) => {
    switch (token) {
      case "yyyy":
        return String(date.getFullYear())
      case "MM":
        return pad2(date.getMonth() + 1)
      case "dd":
        return pad2(date.getDate())
      case "HH":
        return pad2(date.getHours())
      case "h":
        return String(date.getHours() % 12 === 0 ? 12 : date.getHours() % 12)
      case "mm":
        return pad2(date.getMinutes())
      default:
        return date.getHours() < 12 ? "AM" : "PM"
    }
  })
}
