const currencyFormat = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" })

export function format(value: number | undefined, decimalPoint = 2) {
  if (value == null) {
    return ""
  }

  if (isNaN(value)) {
    console.error("The given value is not a number!")
    return ""
  }

  return value.toLocaleString("en-US", { minimumFractionDigits: decimalPoint, maximumFractionDigits: decimalPoint })
}

/** Formats a value as US dollars: `$1,234.56`. */
export function formatCurrency(value: number | undefined) {
  if (value == null) {
    return ""
  }

  if (isNaN(value)) {
    console.error("The given value is not a number!")
    return ""
  }

  return currencyFormat.format(value)
}
