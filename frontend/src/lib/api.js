const BASE = import.meta.env.VITE_API_URL || '/api'

export function getToken() {
  return localStorage.getItem('token') || ''
}

export async function api(path, options = {}) {
  const token = getToken()
  const res = await fetch(BASE + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  })
  if (!res.ok) {
    let msg = 'Erro ' + res.status
    try {
      const body = await res.json()
      if (body.message) msg = body.message
    } catch {}
    throw new Error(msg)
  }
  if (res.status === 204) return null
  return res.json()
}

export async function apiUpload(path, formData) {
  const token = getToken()
  const res = await fetch(BASE + path, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formData
  })
  const data = await res.json()
  if (!res.ok) throw new Error(data.message || 'Erro no upload')
  return data
}

export async function apiDownload(path) {
  const token = getToken()
  const res = await fetch(BASE + path, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!res.ok) throw new Error('Falha ao baixar arquivo.')
  return res.blob()
}
