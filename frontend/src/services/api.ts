/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
const API_BASE = import.meta.env.VITE_API_URL || '/api'

export function createApi(token, setToast) {
  async function request(path, options = {}) {
    const headers = { ...(options.headers || {}) }
    if (options.body !== undefined) headers['Content-Type'] = 'application/json'
    if (token) headers.Authorization = `Bearer ${token}`

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })

    const text = await response.text()
    const payload = text ? safeJson(text) : null
    if (!response.ok) {
      const message = payload?.message || payload?.error || text || `Erro ${response.status}`
      setToast(message)
      const error = new Error(message)
      error.status = response.status
      error.payload = payload
      throw error
    }
    return payload ?? text
  }

  return {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: 'POST', body }),
    put: (path, body) => request(path, { method: 'PUT', body }),
    patch: (path, body) => request(path, { method: 'PATCH', body }),
    delete: (path) => request(path, { method: 'DELETE' }),
  }
}

function safeJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}
