/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function digitsOnly(value) {
  return String(value || '').replace(/\D/g, '')
}

export function formatCpf(value) {
  return digitsOnly(value)
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

export function formatCep(value) {
  return digitsOnly(value)
    .slice(0, 8)
    .replace(/(\d{5})(\d)/, '$1-$2')
}

export function maskCard(value) {
  const cardDigits = digitsOnly(value)
  if (!cardDigits) return '**** **** **** ----'
  return cardDigits.replace(/\d(?=\d{4})/g, '*').replace(/(.{4})/g, '$1 ').trim()
}

export function formatCardNumber(value) {
  const cardDigits = digitsOnly(value)
  if (!cardDigits) return '---- ---- ---- ----'
  return cardDigits.replace(/(.{4})/g, '$1 ').trim()
}

export function money(value) {
  const numeric = Number(value || 0)
  return numeric.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function date(value) {
  if (!value) return '--'
  return new Date(value).toLocaleDateString('pt-BR')
}

export function formatCell(value) {
  if (value === null || value === undefined) return '--'
  if (Array.isArray(value)) return `${value.length} itens`
  if (typeof value === 'object') return JSON.stringify(value)
  if (typeof value === 'number') return value.toLocaleString('pt-BR')
  if (typeof value === 'string' && value.match(/^\d{4}-\d{2}-\d{2}T/)) return new Date(value).toLocaleString('pt-BR')
  return value
}
