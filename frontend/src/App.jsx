import React, { useState } from 'react'

const API = '/api'

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('token') || '')
  const [view, setView] = useState('dashboard')
  const [resumeId, setResumeId] = useState(null)
  const [vacancyId, setVacancyId] = useState(null)
  const [matchResult, setMatchResult] = useState(null)
  const [loading, setLoading] = useState(false)
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

  // ---------- LOGIN / CADASTRO ----------
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
      setView('dashboard')
    } catch (err) { showError(err) }
  }

  async function handleRegister(e) {
    e.preventDefault()
    const name = e.target.name.value
    const email = e.target.email.value
    const password = e.target.password.value
    setError('')
    try {
      await api('/v1/users', {
        method: 'POST',
        body: JSON.stringify({ name, email, password })
      })
      showSuccess('Conta criada! Faça login.')
    } catch (err) { showError(err) }
  }

  function handleLogout() {
    localStorage.removeItem('token')
    setToken('')
    setView('dashboard')
    setMatchResult(null)
    setResumeId(null)
    setVacancyId(null)
  }

  // ---------- CURRÍCULO ----------
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
      setResumeId(data.id)
      showSuccess('Currículo enviado! O sistema leu as skills automaticamente.')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  // ---------- VAGA ----------
  const [vacancyNeedsInfo, setVacancyNeedsInfo] = useState(false)

  async function handleVacancy(e) {
    e.preventDefault()
    const url = e.target.url.value
    const title = e.target.title.value
    const description = e.target.description.value
    setLoading(true)
    setError('')
    try {
      let data
      if (url) {
        data = await api('/v1/vacancies/from-url?url=' + encodeURIComponent(url), {
          method: 'POST'
        })
        if (data.needsMoreInfo) {
          setVacancyNeedsInfo(true)
          showSuccess('Vaga lida do link! A descrição veio curta — cole o texto completo abaixo pra ter um match real.')
        } else {
          setVacancyNeedsInfo(false)
        }
      } else {
        data = await api('/v1/vacancies', {
          method: 'POST',
          body: JSON.stringify({ title, description })
        })
        setVacancyNeedsInfo(false)
      }
      setVacancyId(data.id)
      showSuccess('Vaga salva! Clique em "Ver meu match".')
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
      showSuccess('Vaga atualizada! Skills extraídas. Agora pode ver o match.')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  // ---------- MATCH ----------
  async function handleMatch() {
    if (!resumeId || !vacancyId) {
      showError('Envie o currículo e a vaga primeiro.')
      return
    }
    setLoading(true)
    setError('')
    try {
      const match = await api(`/v1/matches/calculate?resumeId=${resumeId}&vacancyId=${vacancyId}`, {
        method: 'POST'
      })
      const analysis = await api(`/v1/analyses/generate?matchId=${match.id}`, { method: 'POST' })
      const recommendation = await api(`/v1/recommendations/generate?matchId=${match.id}`, { method: 'POST' })
      setMatchResult({ match, analysis, recommendation })
      setView('result')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  // ---------- TELAS ----------
  if (!token) {
    return (
      <div className="auth-page">
        <h1>🎯 MatchIQ</h1>
        <p className="subtitle">Veja se o seu currículo combina com a vaga</p>

        <div className="card">
          <h2>Entrar</h2>
          <form onSubmit={handleLogin}>
            <input name="email" type="email" placeholder="E-mail" required />
            <input name="password" type="password" placeholder="Senha" required />
            <button type="submit">Entrar</button>
          </form>
        </div>

        <div className="card">
          <h2>Criar conta</h2>
          <form onSubmit={handleRegister}>
            <input name="name" placeholder="Nome" required />
            <input name="email" type="email" placeholder="E-mail" required />
            <input name="password" type="password" placeholder="Senha (mín. 8)" minLength={8} required />
            <button type="submit">Criar conta</button>
          </form>
        </div>
        {error && <div className="banner error">{error}</div>}
        {success && <div className="banner success">{success}</div>}
      </div>
    )
  }

  if (view === 'result' && matchResult) {
    const { match, analysis, recommendation } = matchResult
    return (
      <div className="page">
        <header>
          <h1>🎯 MatchIQ</h1>
          <button className="link" onClick={handleLogout}>Sair</button>
        </header>

        <div className="score-big">{match.score}%</div>
        <p className="score-label">de compatibilidade com a vaga</p>

        <div className="card">
          <h2>✅ O que você tem</h2>
          <ul>{analysis.strengths?.length ? analysis.strengths.map((s, i) => <li key={i}>{s}</li>) : <li>Nenhuma skill detectada</li>}</ul>
        </div>

        <div className="card">
          <h2>❌ O que falta</h2>
          <ul>{analysis.gaps?.length ? analysis.gaps.map((g, i) => <li key={i}>{g}</li>) : <li>Nada! Você atende tudo.</li>}</ul>
        </div>

        <div className="card">
          <h2>📋 Plano de estudos</h2>
          <p className="preserve">{recommendation.studyPlan}</p>
          {recommendation.source === 'AI' && <p className="tag">✨ Gerado com IA</p>}
        </div>

        <button onClick={() => { setView('dashboard'); setMatchResult(null) }}>← Fazer outro match</button>
        {error && <div className="banner error">{error}</div>}
      </div>
    )
  }

  return (
    <div className="page">
      <header>
        <h1>🎯 MatchIQ</h1>
        <button className="link" onClick={handleLogout}>Sair</button>
      </header>

      <div className="card">
        <h2>1️⃣ Envie seu currículo</h2>
        <p className="hint">PDF ou Word. O sistema lê as skills sozinho.</p>
        <form onSubmit={handleUploadResume}>
          <input name="file" type="file" accept=".pdf,.docx" required />
          <input name="language" placeholder="Idioma (pt-BR)" defaultValue="pt-BR" />
          <button type="submit" disabled={loading}>{loading ? 'Enviando...' : 'Enviar currículo'}</button>
        </form>
        {resumeId && <div className="banner success">✅ Currículo enviado</div>}
      </div>

      <div className="card">
        <h2>2️⃣ Cole a vaga</h2>
        <p className="hint">Cola o link da vaga (o sistema tenta ler) ou escreve o texto.</p>
        <form onSubmit={handleVacancy}>
          <input name="url" placeholder="Link da vaga (opcional)" />
          <input name="title" placeholder="Título (se for manual)" />
          <textarea name="description" placeholder="Descrição da vaga (se for manual)" rows={4} />
          <button type="submit" disabled={loading}>{loading ? 'Salvando...' : 'Salvar vaga'}</button>
        </form>
        {vacancyId && !vacancyNeedsInfo && <div className="banner success">✅ Vaga salva</div>}

        {vacancyNeedsInfo && (
          <div className="card warn">
            <h2>⚠️ A descrição veio curta do link</h2>
            <p className="hint">O site da vaga não expõe o texto completo. Cole a descrição inteira pra extrair as skills certas:</p>
            <form onSubmit={handleUpdateVacancy}>
              <textarea name="fullDescription" placeholder="Cole aqui a descrição completa da vaga..." rows={6} required />
              <button type="submit" disabled={loading}>{loading ? 'Atualizando...' : 'Atualizar vaga'}</button>
            </form>
          </div>
        )}
      </div>

      <button className="big" onClick={handleMatch} disabled={loading || !resumeId || !vacancyId || vacancyNeedsInfo}>
        {loading ? 'Calculando...' : '💘 Ver meu match'}
      </button>

      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>}
    </div>
  )
}
