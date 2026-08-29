import type { ErrorResponse } from '../types';

const API_BASE_URL = '/api/v1';

class ApiError extends Error {
  status: number;
  code: string;
  traceId?: string;

  constructor(errorResponse: ErrorResponse) {
    super(errorResponse.message || 'An error occurred');
    this.name = 'ApiError';
    this.status = errorResponse.status;
    this.code = errorResponse.code;
    this.traceId = errorResponse.traceId;
  }
}

export async function apiFetch<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('respondr_access_token');
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401 && !endpoint.startsWith('/auth/')) {
    const refreshToken = localStorage.getItem('respondr_refresh_token');
    if (refreshToken) {
      try {
        const refreshResponse = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken }),
        });

        if (refreshResponse.ok) {
          const data = await refreshResponse.json();
          localStorage.setItem('respondr_access_token', data.accessToken);
          localStorage.setItem('respondr_refresh_token', data.refreshToken);

          headers.set('Authorization', `Bearer ${data.accessToken}`);
          response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers,
          });
        } else {
          localStorage.removeItem('respondr_access_token');
          localStorage.removeItem('respondr_refresh_token');
          window.dispatchEvent(new Event('respondr_auth_expired'));
        }
      } catch {
        localStorage.removeItem('respondr_access_token');
        localStorage.removeItem('respondr_refresh_token');
        window.dispatchEvent(new Event('respondr_auth_expired'));
      }
    }
  }

  if (response.status === 204) {
    return {} as T;
  }

  if (!response.ok) {
    let errData: ErrorResponse;
    try {
      errData = await response.json();
    } catch {
      errData = {
        timestamp: new Date().toISOString(),
        status: response.status,
        code: 'HTTP_ERROR',
        message: response.statusText || 'HTTP Request Failed',
        path: endpoint,
        traceId: 'unknown',
      };
    }
    throw new ApiError(errData);
  }

  return response.json();
}
