/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import { digitsOnly } from './format'

export const initialRegister = {
  name: '',
  email: '',
  password: '',
  cpf: '',
  birthDate: '',
  phone: '',
  zipCode: '',
  state: '',
  city: '',
  neighborhood: '',
  street: '',
  number: '',
  complement: '',
}

export function titleForPath(pathname) {
  const titles = {
    '/banco': 'Banco',
    '/loja': 'Loja',
    '/carrinho': 'Carrinho',
    '/perfil': 'Minhas informações',
    '/pedidos': 'Meus pedidos',
    '/admin': 'Painel admin',
    '/admin/ecommerce': 'Gestao ecommerce',
    '/admin/banco': 'Gestao banco',
  }
  return titles[pathname] || 'Plataforma ACC'
}

export function defaultPathForRoles(roles) {
  if (isAdmin(roles)) return '/admin'
  return '/banco'
}

export function isAdmin(roles) {
  return roles.includes('BANKING_ADMIN') || roles.includes('ECOMMERCE_ADMIN')
}

export function isValidCpf(value) {
  const cpf = digitsOnly(value)
  if (cpf.length !== 11 || /^(\d)\1+$/.test(cpf)) return false

  const calculateDigit = (base) => {
    const sum = base
      .split('')
      .reduce((total, digit, index) => total + Number(digit) * (base.length + 1 - index), 0)
    const rest = (sum * 10) % 11
    return rest === 10 ? 0 : rest
  }

  const firstDigit = calculateDigit(cpf.slice(0, 9))
  const secondDigit = calculateDigit(cpf.slice(0, 10))
  return firstDigit === Number(cpf[9]) && secondDigit === Number(cpf[10])
}

export function hasRequiredRegisterFields(values) {
  const requiredFields = [
    'name',
    'email',
    'password',
    'cpf',
    'birthDate',
    'phone',
    'zipCode',
    'state',
    'city',
    'neighborhood',
    'street',
    'number',
  ]

  return requiredFields.every((field) => String(values[field] || '').trim())
}

export function authErrorMessage(error, context) {
  const rawMessage = `${error?.message || ''}`.toLowerCase()
  const fields = error?.payload?.fields || {}

  if (fields.cpf) return 'CPF invalido. Confira os dados e tente novamente.'
  if (fields.zipCode) return 'CEP invalido. Use o formato 00000-000.'
  if (fields.email) return fields.email

  if (rawMessage.includes('senha') || rawMessage.includes('credential') || error?.status === 401) {
    return 'E-mail ou senha incorretos. Confira os dados e tente novamente.'
  }
  if (rawMessage.includes('e-mail') || rawMessage.includes('email')) {
    return 'Este e-mail ja esta cadastrado. Use outro e-mail ou faca login.'
  }
  if (rawMessage.includes('cpf')) {
    return rawMessage.includes('cadastrado')
      ? 'Este CPF ja esta cadastrado. Use outro CPF ou faca login.'
      : 'CPF invalido. Confira os dados e tente novamente.'
  }
  if (rawMessage.includes('cep') || rawMessage.includes('zipcode')) {
    return 'CEP invalido. Use o formato 00000-000.'
  }

  return context === 'login'
    ? 'Nao foi possivel entrar. Confira e-mail e senha.'
    : 'Nao foi possivel criar sua conta. Confira os dados informados.'
}

export function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
