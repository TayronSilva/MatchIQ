import React, { createContext, useContext, useState, useEffect } from 'react'

const API = import.meta.env.VITE_API_URL || '/api'
const AppCtx = createContext(null)

export function useApp() {
  return useContext(AppCtx)
}

export function AppProvider({ children }) {
  const [token, setToken] = useState(localStorage.getItem('token') || '')
  const [resumeId, setResumeId] = useState(localStorage.getItem('mqi_resumeId') || '')
  const [resumeName, setResumeName] = useState(localStorage.getItem('mqi_resumeName') || '')
  const [resumes, setResumes] = useState([])
  const [showUpload, setShowUpload] = useState(false)

  const [vacancyId, setVacancyId] = useState(localStorage.getItem('mqi_vacancyId') || '')
  const [vacancyMode, setVacancyMode] = useState('link')
  const [vacancyNeedsInfo, setVacancyNeedsInfo] = useState(false)
  const [vacancyHint, setVacancyHint] = useState('')

  const [matches, setMatches] = useState(() => {
    try { return JSON.parse(localStorage.getItem('mqi_matches')) || [] } catch { return [] }
  })
  const [vacancies, setVacancies] = useState(() => {
    try { return JSON.parse(localStorage.getItem('mqi_vacancies')) || {} } catch { return {} }
  })
  const [applications, setApplications] = useState([])
  const [appsByVacancy, setAppsByVacancy] = useState({})
  const [tailorList, setTailorList] = useState([])

  const [loading, setLoading] = useState(false)
  const [dashboardLoading, setDashboardLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function api(path, options = {}) {
    const res = await fetch(API + path, {
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
      } catch (e) { /* ignore */ }
      throw new Error(msg)
    }
    if (res.status === 204) return null
    return res.json()
  }

  function showError(e) {
    setError(e.message || 'Algo deu errado')
    setTimeout(() => setError(''), 5000)
  }
  function showSuccess(msg) {
    setSuccess(msg)
    setTimeout(() => setSuccess(''), 5000)
  }

  async function loadResumes() {
    let list = null
    try {
      list = await api('/v1/resumes')
    } catch (e) {
      list = null
    }
    if (list === null) return
    setResumes(list || [])
    const savedId = localStorage.getItem('mqi_resumeId')
    const exists = (list || []).some(r => String(r.id) === String(savedId))
    if (list && list.length) {
      const pick = exists ? list.find(r => String(r.id) === String(savedId)) : list[0]
      const name = pick.fileName || pick.originalName || pick.title || 'Currículo'
      setResumeId(String(pick.id))
      setResumeName(name)
      localStorage.setItem('mqi_resumeId', String(pick.id))
      localStorage.setItem('mqi_resumeName', name)
    } else {
      setResumeId('')
      setResumeName('')
      localStorage.removeItem('mqi_resumeId')
      localStorage.removeItem('mqi_resumeName')
    }
  }

  async function deleteResume(id) {
    if (!window.confirm('Excluir este currículo? Isso não apaga os matches já feitos com ele.')) return
    try {
      await api(`/v1/resumes/${id}`, { method: 'DELETE' })
      await loadResumes()
      showSuccess('Currículo excluído.')
    } catch (err) { showError(err) }
  }

  async function handleLogin(e) {
    e.preventDefault()
    const email = e.target.email.value
    const password = e.target.password.value
    setError('')
    try {
      const data = await api('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password })
      })
      localStorage.setItem('token', data.token)
      setToken(data.token)
      await Promise.all([loadResumes(), loadDashboard()])
    } catch (err) { showError(err) }
  }

  async function handleRegister(e) {
    e.preventDefault()
    const name = e.target.name.value
    const email = e.target.email.value
    const password = e.target.password.value
    setError('')
    try {
      await api('/v1/users', { method: 'POST', body: JSON.stringify({ name, email, password }) })
      showSuccess('Conta criada! Agora é só entrar.')
      e.target.reset()
    } catch (err) { showError(err) }
  }

  function handleLogout() {
    localStorage.removeItem('token')
    setToken('')
    setResumeId('')
    setResumeName('')
    setVacancyId('')
    setResumes([])
  }

  async function loadDashboard() {
    setDashboardLoading(true)
    try {
      const [ms, vs] = await Promise.all([
        api('/v1/matches').catch(() => null),
        api('/v1/vacancies').catch(() => null)
      ])
      if (ms) {
        setMatches(ms)
        localStorage.setItem('mqi_matches', JSON.stringify(ms))
      }
      if (vs) {
        const map = {}
        vs.forEach(v => { map[v.id] = v })
        setVacancies(map)
        localStorage.setItem('mqi_vacancies', JSON.stringify(map))
      }
    } catch (e) { /* mantém o cache local */ }
    finally { setDashboardLoading(false) }
  }

  useEffect(() => {
    if (token) {
      loadResumes()
      loadDashboard()
      loadApplications()
      loadTailorList()
    }
    // eslint-disable-next-line
  }, [token])

  async function handleUploadResume(e) {
    e.preventDefault()
    const file = e.target.file.files[0]
    const language = e.target.language.value || 'pt-BR'
    if (!file) return
    setLoading(true)
    setError('')
    try {
      const form = new FormData()
      form.append('file', file)
      const res = await fetch(API + '/v1/resumes?language=' + encodeURIComponent(language), {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: form
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data.message || 'Erro no upload')
      const name = data.fileName || data.originalName || file.name
      setResumeId(String(data.id))
      setResumeName(name)
      localStorage.setItem('mqi_resumeId', String(data.id))
      localStorage.setItem('mqi_resumeName', name)
      setShowUpload(false)
      await loadResumes()
      showSuccess('Currículo salvo! Vou usá-lo nos próximos matches.')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  function selectResume(r) {
    setResumeId(String(r.id))
    setResumeName(r.fileName || r.originalName || r.title || 'Currículo')
    localStorage.setItem('mqi_resumeId', String(r.id))
    localStorage.setItem('mqi_resumeName', r.fileName || r.originalName || r.title || 'Currículo')
    showSuccess('Currículo selecionado.')
  }

  async function handleVacancyLink(e) {
    e.preventDefault()
    const url = e.target.url.value.trim()
    if (!url) return
    setLoading(true)
    setError('')
    setVacancyHint('')
    try {
      const data = await api('/v1/vacancies/from-url?url=' + encodeURIComponent(url), { method: 'POST' })
      if (data.needsMoreInfo) {
        setVacancyNeedsInfo(true)
        setVacancyId(data.id)
        setVacancyHint('A descrição veio incompleta. Cole abaixo o texto completo da vaga para extrair as skills certas.')
      } else {
        setVacancyNeedsInfo(false)
        setVacancyId(data.id)
        localStorage.setItem('mqi_vacancyId', String(data.id))
        setVacancyHint('✅ Vaga adicionada! Agora clique em "Analisar".')
      }
    } catch (err) {
      setVacancyNeedsInfo(false)
      setVacancyHint('Alguns sites (como o Indeed) não permitem ler a vaga pelo link. Sem problema: abra a vaga no navegador, copie o título e a descrição e use a aba "Colar texto".')
      showError(err)
    } finally { setLoading(false) }
  }

  async function handleVacancyManual(e) {
    e.preventDefault()
    const title = e.target.title.value.trim()
    const description = e.target.description.value.trim()
    const url = e.target.url ? e.target.url.value.trim() : ''
    setLoading(true)
    setError('')
    setVacancyHint('')
    try {
      const data = await api('/v1/vacancies', {
        method: 'POST',
        body: JSON.stringify({ title: title || 'Vaga', description, url })
      })
      setVacancyNeedsInfo(false)
      setVacancyId(data.id)
      localStorage.setItem('mqi_vacancyId', String(data.id))
      setVacancyHint('✅ Vaga salva! Agora clique em "Analisar".')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  async function handleUpdateVacancy(e) {
    e.preventDefault()
    const description = e.target.fullDescription.value
    if (!description || description.length < 50) {
      showError('Cole a descrição completa da vaga (mínimo 50 caracteres).')
      return
    }
    setLoading(true)
    setError('')
    try {
      const data = await api(`/v1/vacancies/${vacancyId}`, {
        method: 'PUT',
        body: JSON.stringify({ title: 'Vaga', description })
      })
      setVacancyNeedsInfo(false)
      setVacancyId(data.id)
      showSuccess('Vaga atualizada! Skills extraídas. Agora pode analisar.')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  async function createMatch(vid) {
    if (!resumeId || !vid) {
      showError('Selecione um currículo e uma vaga antes de analisar.')
      return null
    }
    setLoading(true)
    setError('')
    try {
      const match = await api(`/v1/matches/calculate?resumeId=${resumeId}&vacancyId=${vid}`, { method: 'POST' })
      return match
    } catch (err) { showError(err); return null }
    finally { setLoading(false) }
  }

  async function deleteMatch(id) {
    try {
      await api(`/v1/matches/${id}`, { method: 'DELETE' })
    } catch (err) { showError(err) }
    const next = matches.filter(m => m.id !== id)
    setMatches(next)
    localStorage.setItem('mqi_matches', JSON.stringify(next))
    showSuccess('Match removido do ranking.')
  }

  async function clearMatches() {
    if (!window.confirm('Limpar todo o ranking? Isso remove todos os matches testados.')) return
    try {
      await api('/v1/matches', { method: 'DELETE' })
    } catch (err) { showError(err) }
    setMatches([])
    localStorage.setItem('mqi_matches', JSON.stringify([]))
    showSuccess('Ranking limpo.')
  }

  async function loadTailorList() {
    try {
      const list = await api('/v1/tailor')
      setTailorList(list || [])
    } catch (e) { /* ignora */ }
  }

  async function downloadTailoredPdf(id) {
    try {
      const res = await fetch(API + '/v1/tailor/' + id + '/pdf', {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      })
      if (!res.ok) { showError('Falha ao baixar o PDF.'); return }
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'curriculo-tailor.pdf'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) { showError(e) }
  }

  async function loadApplications() {
    try {
      const list = await api('/v1/applications')
      const arr = list || []
      setApplications(arr)
      setAppsByVacancy(Object.fromEntries(arr.map(a => [a.vacancyId, a])))
    } catch (e) { /* ignora */ }
  }

  async function createApplication(vacancyId, resumeId, matchId) {
    try {
      const created = await api('/v1/applications', {
        method: 'POST',
        body: JSON.stringify({ vacancyId, resumeId, matchId, status: 'APPLIED' })
      })
      setApplications(prev => [created, ...prev])
      setAppsByVacancy(prev => ({ ...prev, [vacancyId]: created }))
      showSuccess('Candidatura registrada!')
      return created
    } catch (e) { showError(e) }
  }

  async function updateApplication(id, patch) {
    try {
      const u = await api('/v1/applications/' + id, {
        method: 'PUT',
        body: JSON.stringify(patch)
      })
      setApplications(prev => prev.map(a => a.id === id ? u : a))
      if (u.vacancyId) setAppsByVacancy(prev => ({ ...prev, [u.vacancyId]: u }))
    } catch (e) { showError(e) }
  }

  async function deleteApplication(id) {
    try {
      await api('/v1/applications/' + id, { method: 'DELETE' })
      setApplications(prev => prev.filter(a => a.id !== id))
      setAppsByVacancy(prev => {
        const next = { ...prev }
        for (const k in next) if (next[k].id === id) delete next[k]
        return next
      })
    } catch (e) { showError(e) }
  }

  const value = {
    token, setToken,
    resumeId, setResumeId, resumeName, setResumeName, resumes, showUpload, setShowUpload,
    vacancyId, setVacancyId, vacancyMode, setVacancyMode, vacancyNeedsInfo, setVacancyNeedsInfo, vacancyHint, setVacancyHint,
    matches, setMatches, vacancies, setVacancies,
    applications, appsByVacancy, tailorList,
    loading, dashboardLoading, error, success,
    api, showError, showSuccess,
    loadResumes, deleteResume, handleLogin, handleRegister, handleLogout,
    loadDashboard, handleUploadResume, selectResume,
    handleVacancyLink, handleVacancyManual, handleUpdateVacancy,
    createMatch, deleteMatch, clearMatches,
    loadTailorList, downloadTailoredPdf,
    loadApplications, createApplication, updateApplication, deleteApplication
  }

  return <AppCtx.Provider value={value}>{children}</AppCtx.Provider>
}
