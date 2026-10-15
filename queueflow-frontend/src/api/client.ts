const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const ACCESS_TOKEN_KEY = 'queueflow_access_token'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

async function parseResponse<T>(response: Response): Promise<T> {
  if (response.ok) {
    if (response.status === 204) {
      return undefined as T
    }

    return response.json() as Promise<T>
  }

  let message = `Request failed with status ${response.status}`

  try {
    const body = (await response.json()) as {
      message?: string
      error?: string
    }

    message = body.message ?? body.error ?? message
  } catch {
    // Keep the default HTTP error message.
  }

  throw new ApiError(response.status, message)
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)

  const headers = new Headers(options.headers)

  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (accessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    })

    return parseResponse<T>(response)
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }

    throw new ApiError(
      0,
      'Unable to connect to QueueFlow. Please check your connection and try again.',
    )
  }
}

export { API_BASE_URL, ACCESS_TOKEN_KEY }
