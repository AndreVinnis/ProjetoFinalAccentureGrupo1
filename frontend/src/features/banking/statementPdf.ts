import { date, formatCell } from '../../utils/format'

const pageWidth = 595
const pageHeight = 842
const margin = 48
const lineHeight = 16

type StatementRecord = Record<string, unknown>
type StatementData = Record<string, unknown>

function cleanText(value: unknown) {
  return String(value ?? '--')
    .replace(/R\$\s*-/g, 'R$ ')
    .replace(/-\s*R\$/g, 'R$ ')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^\x20-\x7E]/g, '-')
}

function escapePdfText(value: unknown) {
  return cleanText(value)
    .replace(/\\/g, '\\\\')
    .replace(/\(/g, '\\(')
    .replace(/\)/g, '\\)')
}

function textLine(text: unknown, x: number, y: number, size = 10, bold = false) {
  return `BT /${bold ? 'F2' : 'F1'} ${size} Tf ${x} ${y} Td (${escapePdfText(text)}) Tj ET`
}

function splitLine(text: unknown, maxLength = 90) {
  const words = cleanText(text).split(' ')
  const lines: string[] = []
  let current = ''

  words.forEach((word) => {
    const next = current ? `${current} ${word}` : word
    if (next.length > maxLength) {
      if (current) lines.push(current)
      current = word
    } else {
      current = next
    }
  })

  if (current) lines.push(current)
  return lines.length ? lines : ['--']
}

function statementMoney(value: unknown) {
  const numeric = parseMoneyValue(value)
  const formatted = Math.abs(numeric).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })

  return `R$ ${formatted}`
}

function parseMoneyValue(value: unknown) {
  if (typeof value === 'number') return value

  const normalized = String(value ?? 0)
    .trim()
    .replace(/[^\d,.-]/g, '')

  if (normalized.includes(',')) {
    return Number(normalized.replace(/\./g, '').replace(',', '.')) || 0
  }

  return Number(normalized) || 0
}

function asRecord(value: unknown): StatementRecord {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as StatementRecord
    : {}
}

function asRecordArray(value: unknown): StatementRecord[] {
  return Array.isArray(value) ? value as StatementRecord[] : []
}

function buildStatementLines(data: StatementData) {
  const generatedAt = new Date()
  const account = asRecord(data.account)
  const balanceInfo = asRecord(data.balance)
  const balance = balanceInfo.balance ?? account.balance
  const transactions = asRecordArray(data.transactions)
  const purchases = asRecordArray(data.purchases)
  const invoices = asRecordArray(data.invoices)

  const lines: Array<{ text: string; size?: number; bold?: boolean; spacer?: boolean }> = [
    { text: 'ACC Bank - Extrato geral da conta', size: 18, bold: true },
    { text: `Gerado em ${generatedAt.toLocaleString('pt-BR')}` },
    { text: `Conta: ${account.accountNumber || '--'} | Status: ${account.status || '--'} | Tipo: ${account.accountType || '--'}` },
    { text: `Saldo atual: ${statementMoney(balance)}`, bold: true },
    { text: '', spacer: true },
    { text: 'Transacoes da conta', size: 13, bold: true },
  ]

  if (!transactions.length) {
    lines.push({ text: 'Nenhuma transacao encontrada.' })
  } else {
    transactions.forEach((item) => {
      const description = item.description || item.reference || '--'
      lines.push({
        text: `${date(item.createdAt)} | ${item.type || '--'} | Valor: ${statementMoney(item.amount)} | Saldo: ${statementMoney(item.balanceAfter)} | ${description}`,
      })
    })
  }

  lines.push({ text: '', spacer: true }, { text: 'Compras no cartao', size: 13, bold: true })

  if (!purchases.length) {
    lines.push({ text: 'Nenhuma compra encontrada.' })
  } else {
    purchases.forEach((item) => {
      lines.push({
        text: `${date(item.purchaseDate)} | Fatura ${item.invoiceId || '--'} | ${statementMoney(item.amount)} | ${item.description || item.reference || '--'}`,
      })
    })
  }

  lines.push({ text: '', spacer: true }, { text: 'Faturas', size: 13, bold: true })

  if (!invoices.length) {
    lines.push({ text: 'Nenhuma fatura encontrada.' })
  } else {
    invoices.forEach((item) => {
      lines.push({
        text: `Fatura ${item.id || '--'} | Ref. ${formatCell(item.referenceMonth)} | Total ${statementMoney(item.totalAmount)} | Pago ${statementMoney(item.paidAmount)} | ${item.status || '--'} | Venc. ${date(item.dueDate)}`,
      })
    })
  }

  return lines.flatMap((line) => {
    if (line.spacer) return [line]
    return splitLine(line.text).map((text, index) => ({
      ...line,
      text,
      size: index ? 10 : line.size,
      bold: index ? false : line.bold,
    }))
  })
}

function paginate(lines: ReturnType<typeof buildStatementLines>) {
  const pages: typeof lines[] = []
  let current: typeof lines = []
  let y = pageHeight - margin

  lines.forEach((line) => {
    const usedHeight = line.spacer ? lineHeight : lineHeight + (line.size && line.size > 12 ? 4 : 0)
    if (y - usedHeight < margin) {
      pages.push(current)
      current = []
      y = pageHeight - margin
    }
    current.push(line)
    y -= usedHeight
  })

  if (current.length) pages.push(current)
  return pages
}

function pageContent(lines: ReturnType<typeof buildStatementLines>, pageNumber: number, totalPages: number) {
  let y = pageHeight - margin
  const content = [
    textLine(`Pagina ${pageNumber} de ${totalPages}`, pageWidth - 120, 28, 9),
  ]

  lines.forEach((line) => {
    if (line.spacer) {
      y -= lineHeight
      return
    }
    content.push(textLine(line.text, margin, y, line.size || 10, line.bold))
    y -= lineHeight + (line.size && line.size > 12 ? 4 : 0)
  })

  return content.join('\n')
}

function createPdf(pageStreams: string[]) {
  const fontRegular = 3 + pageStreams.length * 2
  const fontBold = fontRegular + 1
  const objects: string[] = []
  const pageRefs: string[] = []

  objects[1] = '<< /Type /Catalog /Pages 2 0 R >>'

  pageStreams.forEach((stream, index) => {
    const pageObject = 3 + index * 2
    const contentObject = pageObject + 1
    pageRefs.push(`${pageObject} 0 R`)
    objects[pageObject] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageWidth} ${pageHeight}] /Resources << /Font << /F1 ${fontRegular} 0 R /F2 ${fontBold} 0 R >> >> /Contents ${contentObject} 0 R >>`
    objects[contentObject] = `<< /Length ${stream.length} >>\nstream\n${stream}\nendstream`
  })

  objects[2] = `<< /Type /Pages /Kids [${pageRefs.join(' ')}] /Count ${pageRefs.length} >>`
  objects[fontRegular] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'
  objects[fontBold] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>'

  let pdf = '%PDF-1.4\n'
  const offsets = [0]

  for (let index = 1; index < objects.length; index += 1) {
    offsets[index] = pdf.length
    pdf += `${index} 0 obj\n${objects[index]}\nendobj\n`
  }

  const xrefOffset = pdf.length
  pdf += `xref\n0 ${objects.length}\n0000000000 65535 f \n`
  for (let index = 1; index < objects.length; index += 1) {
    pdf += `${String(offsets[index]).padStart(10, '0')} 00000 n \n`
  }
  pdf += `trailer\n<< /Size ${objects.length} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF`
  return pdf
}

export function downloadStatementPdf(data: StatementData) {
  const pages = paginate(buildStatementLines(data))
  const streams = pages.map((page, index) => pageContent(page, index + 1, pages.length))
  const pdf = createPdf(streams)
  const blob = new Blob([pdf], { type: 'application/pdf' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const account = asRecord(data.account)
  const accountNumber = cleanText(account.accountNumber || 'conta').replace(/\W+/g, '-')

  link.href = url
  link.download = `extrato-acc-bank-${accountNumber}.pdf`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
