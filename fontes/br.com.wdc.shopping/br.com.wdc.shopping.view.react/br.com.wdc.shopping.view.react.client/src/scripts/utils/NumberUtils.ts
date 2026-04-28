export function format(value: number | undefined, decimalPoint = 2) {
  if (value == null) {
    return ''
  }

  if (isNaN(value)) {
    console.error('Valor informado não é de tipo numérico!')
    return ''
  }

  return value.toLocaleString(navigator.language, { minimumFractionDigits: decimalPoint })
}
