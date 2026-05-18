const API_BASE = import.meta.env.VITE_API_URL || '/api'

interface ApiRequestOptions {
  method?: string
  headers?: Record<string, string>
  body?: unknown
  silent?: boolean
}

interface ApiErrorPayload {
  message?: string
  error?: string
}

export interface Api {
  get<T = unknown>(path: string, options?: ApiRequestOptions): Promise<T>
  post<T = unknown>(path: string, body?: unknown): Promise<T>
  put<T = unknown>(path: string, body?: unknown): Promise<T>
  patch<T = unknown>(path: string, body?: unknown): Promise<T>
  delete<T = unknown>(path: string): Promise<T>
}

export class ApiError extends Error {
  status: number
  payload?: unknown

  constructor(
    message: string,
    status: number,
    payload?: unknown
  ) {
    super(message)

    this.name = 'ApiError'
    this.status = status
    this.payload = payload
  }
}

export function createApi(
  token: string | null,
  setToast: (message: string) => void,
  onUnauthorized?: () => void
): Api {

  async function request<T>(
    path: string,
    options: ApiRequestOptions = {}
  ): Promise<T> {

    const headers: Record<string, string> = {
      ...(options.headers ?? {})
    }

    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json'
    }

    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(
      `${API_BASE}${path}`,
      {
        ...options,
        headers,
        body:
          options.body === undefined
            ? undefined
            : JSON.stringify(options.body)
      }
    )

    const text = await response.text()

    const payload = text
      ? safeJson<unknown>(text)
      : null

    if (!response.ok) {

      const errorPayload =
        payload as ApiErrorPayload | null

      const message =
        errorPayload?.message ||
        errorPayload?.error ||
        text ||
        fallbackErrorMessage(response.status, path)

      if (response.status === 401 && token) {
        onUnauthorized?.()
      }

      if (!options.silent) {
        setToast(message)
      }

      throw new ApiError(
        message,
        response.status,
        payload
      )
    }

    return payload as T
  }

  return {
    get: (path, options) => request(path, options),

    post: (path, body) =>
      request(path, {
        method: 'POST',
        body
      }),

    put: (path, body) =>
      request(path, {
        method: 'PUT',
        body
      }),

    patch: (path, body) =>
      request(path, {
        method: 'PATCH',
        body
      }),

    delete: (path) =>
      request(path, {
        method: 'DELETE'
      }),
  }
}

function safeJson<T>(text: string): T {
  try {
    return JSON.parse(text) as T
  } catch {
    return text as T
  }
}

function fallbackErrorMessage(status: number, path: string) {
  if (status === 403 && path.includes('/ecommerce/cart/me/items')) {
    return 'Sua sacola esta fechada. Reabra o carrinho para adicionar produtos.'
  }

  return `Erro ${status}`
}

export type ApiClient =
  ReturnType<typeof createApi>
