export function formatDate(dateOrMillis: number | Date | null | undefined) {
  if (dateOrMillis === undefined || dateOrMillis == null) {
    console.error('Data não informada, ou inválida!')
    return 'Data indisponível'
  }

  const purchaseDate = typeof dateOrMillis === 'number' ? new Date(dateOrMillis) : dateOrMillis

  let purchaseDay = purchaseDate.getDay() + 1 //Dia primeiro retorna zero
  let purchaseMonth = purchaseDate.getMonth() + 1 //Janeiro retorna zero
  let purchaseFullYear = purchaseDate.getFullYear()

  let dayOutput = purchaseDay < 10 ? '0' + purchaseDay : purchaseDay
  let monthOutput = purchaseMonth < 10 ? '0' + purchaseMonth : purchaseMonth

  return dayOutput + '/' + monthOutput + '/' + purchaseFullYear
}
