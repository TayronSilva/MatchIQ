import React, { useState, useEffect } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

const API = '/api'

function scoreColor(s) {
  if (s >= 70) return 'var(--ok)'
  if (s >= 40) return 'var(--warn)'
  return 'var(--miss)'
}

function scoreLabel(s) {
  if (s >= 70) return 'Ótimo encaixe'
  if (s >= 40) return 'Bom potencial'
  return 'Distante da vaga'
}

function buildDiagnosis(score, matched, missing) {
  let head
  if (score >= 80) head = 'Excelente compatibilidade! Seu currículo atende à maior parte dos requisitos da vaga.'
  else if (score >= 50) head = 'Boa compatibilidade. Você já tem boa parte do que a vaga pede.'
  else if (score > 0) head = 'Compatibilidade moderada. Há skills importantes a desenvolver.'
  else head = 'Compatibilidade baixa. A vaga exige skills que ainda não identificamos no currículo.'
  const have = matched.length
    ? 'Skills que você já domina: ' + matched.join(', ') + '.'
    : 'Nenhuma skill do cruzamento foi detectada no currículo.'
  const need = missing.length
    ? 'Reforce: ' + missing.join(', ') + '.'
    : 'Você atende a todas as skills exigidas!'
  return head + '\n\n' + have + '\n' + need
}

function buildPlan(missing) {
  if (!missing.length) return 'Nenhum plano necessário: você já domina as skills exigidas pela vaga.'
  return 'Plano sugerido (ordem recomendada):\n' +
    missing.map((m, i) => (i + 1) + '. Estudar ' + m + ' — foque em fundamentos e pratique com projetos reais.').join('\n') +
    '\nDica: monte um projeto de portfólio combinando essas skills para comprovar na prática.'
}

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('token') || '')
  const [view, setView] = useState('dashboard')
  const [authTab, setAuthTab] = useState('login')

  const [resumeId, setResumeId] = useState(localStorage.getItem('mqi_resumeId') || '')
  const [resumeName, setResumeName] = useState(localStorage.getItem('mqi_resumeName') || '')
  const [resumes, setResumes] = useState([])
  const [showUpload, setShowUpload] = useState(false)

  const [vacancyId, setVacancyId] = useState(localStorage.getItem('mqi_vacancyId') || '')
  const [vacancyMode, setVacancyMode] = useState('link')
  const [vacancyNeedsInfo, setVacancyNeedsInfo] = useState(false)
  const [vacancyHint, setVacancyHint] = useState('')

  const [matchResult, setMatchResult] = useState(null)
  const [genStuck, setGenStuck] = useState(false)
  const [matches, setMatches] = useState([])
  const [vacancies, setVacancies] = useState({})
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
    try {
      const list = await api('/v1/resumes').catch(() => [])
      setResumes(list || [])
      const savedId = localStorage.getItem('mqi_resumeId')
      const exists = (list || []).some(r => String(r.id) === String(savedId))
      if (list && list.length) {
        const pick = exists ? list.find(r => String(r.id) === String(savedId)) : list[0]
        setResumeId(String(pick.id))
        setResumeName(pick.fileName || pick.originalName || pick.title || 'Currículo')
        localStorage.setItem('mqi_resumeId', String(pick.id))
        localStorage.setItem('mqi_resumeName', pick.fileName || pick.originalName || pick.title || 'Currículo')
      } else {
        setResumeId('')
        setResumeName('')
        localStorage.removeItem('mqi_resumeId')
        localStorage.removeItem('mqi_resumeName')
      }
    } catch (e) { /* ignora */ }
  }

  async function deleteResume(id) {
    if (!window.confirm('Excluir este currículo? Isso não apaga os matches já feitos com ele.')) return
    try {
      await api(`/v1/resumes/${id}`, { method: 'DELETE' })
      await loadResumes()
      showSuccess('Currículo excluído.')
    } catch (err) { showError(err) }
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
      await api('/v1/users', {
        method: 'POST',
        body: JSON.stringify({ name, email, password })
      })
      showSuccess('Conta criada! Agora é só entrar.')
      setAuthTab('login')
      e.target.reset()
    } catch (err) { showError(err) }
  }

  function handleLogout() {
    localStorage.removeItem('token')
    setToken('')
    setView('dashboard')
    setMatchResult(null)
    setResumeId('')
    setResumeName('')
    setVacancyId('')
    setResumes([])
  }

  // ---------- DASHBOARD ----------
  async function loadDashboard() {
    setDashboardLoading(true)
    try {
      const [ms, vs] = await Promise.all([
        api('/v1/matches').catch(() => []),
        api('/v1/vacancies').catch(() => [])
      ])
      setMatches(ms || [])
      const map = {}
      ;(vs || []).forEach(v => { map[v.id] = v })
      setVacancies(map)
    } catch (e) { showError(e) }
    finally { setDashboardLoading(false) }
  }

  useEffect(() => {
    if (token && view === 'dashboard') {
      loadDashboard()
      loadResumes()
    }
    // eslint-disable-next-line
  }, [token, view])

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

  // ---------- VAGA ----------
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
        setVacancyHint('✅ Vaga adicionada! Agora clique em "Ver meu match".')
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
    setLoading(true)
    setError('')
    setVacancyHint('')
    try {
      const data = await api('/v1/vacancies', {
        method: 'POST',
        body: JSON.stringify({ title: title || 'Vaga', description })
      })
      setVacancyNeedsInfo(false)
      setVacancyId(data.id)
      localStorage.setItem('mqi_vacancyId', String(data.id))
      setVacancyHint('✅ Vaga salva! Agora clique em "Ver meu match".')
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
      showError('Selecione um currículo e uma vaga primeiro.')
      return
    }
    setLoading(true)
    setError('')
    try {
      const match = await api(`/v1/matches/calculate?resumeId=${resumeId}&vacancyId=${vacancyId}`, { method: 'POST' })
      setMatchResult({ match, analysis: null, recommendation: null })
      setGenStuck(false)
      setView('result')
      poll(match.id)
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  function poll(matchId) {
    const start = Date.now()
    const interval = setInterval(async () => {
      try {
        const [analysis, recommendation] = await Promise.all([
          api(`/v1/analyses/match/${matchId}`).catch(() => ({ status: 'PENDING' })),
          api(`/v1/recommendations/match/${matchId}`).catch(() => ({ status: 'PENDING' }))
        ])
        setMatchResult(prev => ({ ...prev, analysis, recommendation }))
        const done = (s) => !s || s === 'COMPLETED' || s === 'FAILED'
        const timedOut = Date.now() - start > 30000
        if ((done(analysis.status) && done(recommendation.status)) || timedOut) {
          clearInterval(interval)
          if (timedOut) setGenStuck(true)
        }
      } catch (e) { /* mantém pollando */ }
    }, 2000)
  }

  async function viewMatch(matchId) {
    setLoading(true)
    setError('')
    try {
      const [match, analysis, recommendation] = await Promise.all([
        api(`/v1/matches/${matchId}`),
        api(`/v1/analyses/match/${matchId}`).catch(() => null),
        api(`/v1/recommendations/match/${matchId}`).catch(() => null)
      ])
      setMatchResult({ match, analysis, recommendation })
      setGenStuck(false)
      setView('result')
    } catch (err) { showError(err) } finally { setLoading(false) }
  }

  async function deleteMatch(id) {
    try {
      await api(`/v1/matches/${id}`, { method: 'DELETE' })
      await loadDashboard()
      showSuccess('Match removido do ranking.')
    } catch (err) { showError(err) }
  }

  async function clearMatches() {
    if (!window.confirm('Limpar todo o ranking? Isso remove todos os matches testados.')) return
    try {
      await api('/v1/matches', { method: 'DELETE' })
      await loadDashboard()
      showSuccess('Ranking limpo.')
    } catch (err) { showError(err) }
  }

  function backToDashboard() {
    setView('dashboard')
    setMatchResult(null)
    setGenStuck(false)
    loadDashboard()
  }

  // ---------- TELAS ----------
  if (!token) {
    return (
      <div className="auth">
        <div className="auth-brand">
          <div className="logo">🎯</div>
          <h1>MatchIQ</h1>
          <p className="subtitle">Descubra o quanto seu currículo combina com a vaga</p>
        </div>

        <div className="auth-card pop">
          <div className="auth-tabs">
            <button className={'auth-tab' + (authTab === 'login' ? ' active' : '')} onClick={() => setAuthTab('login')}>Entrar</button>
            <button className={'auth-tab' + (authTab === 'register' ? ' active' : '')} onClick={() => setAuthTab('register')}>Criar conta</button>
          </div>

          {authTab === 'login' ? (
            <form onSubmit={handleLogin}>
              <div className="field">
                <label htmlFor="email">E-mail</label>
                <input id="email" name="email" type="email" placeholder="voce@email.com" required autoComplete="email" />
              </div>
              <div className="field">
                <label htmlFor="password">Senha</label>
                <input id="password" name="password" type="password" placeholder="Sua senha" required autoComplete="current-password" />
              </div>
              <button className="btn" type="submit">Entrar</button>
            </form>
          ) : (
            <form onSubmit={handleRegister}>
              <div className="field">
                <label htmlFor="name">Nome</label>
                <input id="name" name="name" placeholder="Seu nome" required autoComplete="name" />
              </div>
              <div className="field">
                <label htmlFor="emailR">E-mail</label>
                <input id="emailR" name="email" type="email" placeholder="voce@email.com" required autoComplete="email" />
              </div>
              <div className="field">
                <label htmlFor="passwordR">Senha</label>
                <input id="passwordR" name="password" type="password" placeholder="Mínimo 8 caracteres" minLength={8} required autoComplete="new-password" />
              </div>
              <button className="btn" type="submit">Criar conta</button>
            </form>
          )}
        </div>

        {error && <div className="banner error">{error}</div>}
        {success && <div className="banner success">{success}</div>}
      </div>
    )
  }

  if (view === 'result' && matchResult) {
    const { match, analysis, recommendation } = matchResult
    const analysisLoading = !analysis || analysis.status === 'PENDING'
    const analysisFailed = analysis && analysis.status === 'FAILED'
    const recommendationLoading = !recommendation || recommendation.status === 'PENDING'
    const recommendationFailed = recommendation && recommendation.status === 'FAILED'
    const matched = match.matchedSkills || []
    const missing = match.missingSkills || []

    return (
      <div className="page">
        <div className="header">
          <span className="brand"><span className="dot">🎯</span> MatchIQ</span>
          <button className="btn-ghost" onClick={backToDashboard}>← Dashboard</button>
        </div>

        <div className="ring-wrap">
          <ScoreRing score={match.score} />
        </div>
        <p className="score-sub">{scoreLabel(match.score)} com a vaga</p>

        {match.rationale && (
          <div className="card ok">
            <h2>🧭 Por que esse score?</h2>
            <p className="preserve">{match.rationale}</p>
          </div>
        )}

        {analysisFailed && (
          <div className="card warn">
            <h2>⚠️ Análise indisponível</h2>
            <p>Não consegui gerar a análise agora. Tente novamente.</p>
          </div>
        )}

        <div className="card">
          <h2>🧠 Diagnóstico</h2>
          {(analysis && analysis.observations) ? (
            <p className="preserve">{analysis.observations}</p>
          ) : match.rationale ? (
            <>
              <p className="preserve">{match.rationale}</p>
              <p className="tag">Resumo do score</p>
            </>
          ) : (
            <p className="preserve">{buildDiagnosis(match.score, matched, missing)}</p>
          )}
        </div>

        <div className="card">
          <h2>✅ O que você tem ({matched.length})</h2>
          <div className="chips">
            {matched.length ? matched.map((s, i) => <span key={i} className="chip ok">{s}</span>)
              : <span className="hint">Nenhuma skill detectada</span>}
          </div>
        </div>

        <div className="card">
          <h2>❌ O que falta ({missing.length})</h2>
          <div className="chips">
            {missing.length ? missing.map((s, i) => <span key={i} className="chip miss">{s}</span>)
              : <span className="hint">Nada! Você atende tudo.</span>}
          </div>
        </div>

        {recommendationFailed && (
          <div className="card warn">
            <h2>⚠️ Plano indisponível</h2>
            <p>Não consegui gerar o plano de estudos agora. Tente novamente.</p>
          </div>
        )}

        <div className="card">
          <h2>📋 Plano de estudos</h2>
          {recommendationLoading ? (
            <p className="hint">Gerando plano com IA…</p>
          ) : recommendation.studyPlan ? (
            <div className="md"><ReactMarkdown remarkPlugins={[remarkGfm]}>{recommendation.studyPlan}</ReactMarkdown></div>
          ) : genStuck ? (
            <p className="preserve">{buildPlan(missing)}</p>
          ) : (
            <p className="hint">Sem plano de estudos.</p>
          )}
          {recommendation && recommendation.source === 'AI' && !recommendationLoading && <p className="tag">✨ Gerado com IA</p>}
        </div>

        <button className="btn" onClick={backToDashboard}>← Voltar ao dashboard</button>
        {error && <div className="banner error">{error}</div>}
      </div>
    )
  }

  // ---------- DASHBOARD ----------
  const ranked = [...matches].sort((a, b) => (b.score || 0) - (a.score || 0))
  const best = ranked.length ? ranked[0].score : 0
  const avg = ranked.length ? Math.round(ranked.reduce((s, m) => s + (m.score || 0), 0) / ranked.length) : 0
  const maxBar = Math.max(1, ...ranked.map(m => m.score || 0))

  return (
    <div className="page">
      <div className="header">
        <span className="brand"><span className="dot">🎯</span> MatchIQ</span>
        <button className="btn-ghost" onClick={handleLogout}>Sair</button>
      </div>

      <div className="hero rise">
        <h1>Bem-vindo ao MatchIQ 💡</h1>
        <p>Veja o quanto seu currículo combina com a vaga dos sonhos — e o que estudar para chegar lá.</p>
      </div>

      <div className="stat-grid">
        <div className="stat"><div className="stat-num">{matches.length}</div><div className="stat-lbl">matches</div></div>
        <div className="stat"><div className="stat-num">{best}%</div><div className="stat-lbl">melhor score</div></div>
        <div className="stat"><div className="stat-num">{avg}%</div><div className="stat-lbl">score médio</div></div>
      </div>

      {/* Currículo */}
      <div className="card rise">
        <div className="card-title"><span className="step-no">1</span> Seu currículo</div>
        {resumeId ? (
          <>
            <div className="resume-active">
              <div className="ic">📄</div>
              <div className="meta">
                <div className="name">{resumeName}</div>
                <div className="sub">Currículo ativo — será usado nos matches</div>
              </div>
              <button className="btn-soft" onClick={() => setShowUpload(v => !v)}>
                {showUpload ? 'Cancelar' : 'Trocar'}
              </button>
              <button className="btn-danger" title="Excluir currículo" onClick={() => deleteResume(resumeId)}>🗑</button>
            </div>
            {resumes.length > 1 && (
              <div className="resume-list">
                {resumes.map(r => (
                  <div key={r.id}
                    className={'resume-chip' + (String(r.id) === String(resumeId) ? ' active' : '')}
                    onClick={() => selectResume(r)}>
                    <span>{r.fileName || r.originalName || r.title || 'Currículo'}</span>
                    <button className="x" title="Excluir"
                      onClick={(e) => { e.stopPropagation(); deleteResume(r.id) }}>✕</button>
                  </div>
                ))}
              </div>
            )}
          </>
        ) : (
          <p className="hint">PDF ou Word. O sistema lê as skills sozinho e salva pra você.</p>
        )}

        {(!resumeId || showUpload) && (
          <form onSubmit={handleUploadResume}>
            <div className="field">
              <label htmlFor="file">Arquivo do currículo</label>
              <input id="file" name="file" type="file" accept=".pdf,.docx" required />
            </div>
            <div className="field">
              <label htmlFor="language">Idioma</label>
              <input id="language" name="language" placeholder="pt-BR" defaultValue="pt-BR" />
            </div>
            <button className="btn" type="submit" disabled={loading}>{loading ? 'Enviando...' : 'Enviar currículo'}</button>
          </form>
        )}
      </div>

      {/* Vaga */}
      <div className="card rise">
        <div className="card-title"><span className="step-no">2</span> A vaga</div>
        <p className="hint">Cole o link da vaga ou cole o texto dela. Funciona do jeito que der.</p>
        <div className="seg">
          <button className={vacancyMode === 'link' ? 'active' : ''} onClick={() => setVacancyMode('link')}>🔗 Link</button>
          <button className={vacancyMode === 'manual' ? 'active' : ''} onClick={() => setVacancyMode('manual')}>📝 Colar texto</button>
        </div>

        {vacancyMode === 'link' ? (
          <form onSubmit={handleVacancyLink}>
            <div className="field">
              <label htmlFor="url">Link da vaga</label>
              <input id="url" name="url" placeholder="https://..." />
            </div>
            <button className="btn" type="submit" disabled={loading}>{loading ? 'Salvando...' : 'Adicionar vaga'}</button>
          </form>
        ) : (
          <form onSubmit={handleVacancyManual}>
            <div className="field">
              <label htmlFor="title">Título (opcional)</label>
              <input id="title" name="title" placeholder="Ex.: Desenvolvedor React" />
            </div>
            <div className="field">
              <label htmlFor="description">Descrição da vaga</label>
              <textarea id="description" name="description" placeholder="Cole aqui o texto completo da vaga..." rows={4} required />
            </div>
            <button className="btn" type="submit" disabled={loading}>{loading ? 'Salvando...' : 'Salvar vaga'}</button>
          </form>
        )}

        {vacancyHint && <div className="banner info">{vacancyHint}</div>}

        {vacancyNeedsInfo && (
          <div className="card warn" style={{ marginTop: 14 }}>
            <h2>⚠️ A descrição veio curta</h2>
            <p className="hint">O site da vaga não expõe o texto completo. Cole a descrição inteira pra extrair as skills certas:</p>
            <form onSubmit={handleUpdateVacancy}>
              <textarea name="fullDescription" placeholder="Cole aqui a descrição completa da vaga..." rows={6} required />
              <button className="btn" type="submit" disabled={loading}>{loading ? 'Atualizando...' : 'Atualizar vaga'}</button>
            </form>
          </div>
        )}
      </div>

      <button className="btn btn-big" onClick={handleMatch} disabled={loading || !resumeId || !vacancyId || vacancyNeedsInfo}>
        {loading ? 'Calculando...' : '💘 Ver meu match'}
      </button>

      <div className="section-head">
        <h2>🏆 Ranking de vagas</h2>
        {ranked.length > 0 && (
          <button className="btn-danger" onClick={clearMatches} title="Limpar tudo">🗑 Limpar</button>
        )}
      </div>

      {dashboardLoading ? (
        <p className="hint">Carregando…</p>
      ) : ranked.length === 0 ? (
        <div className="empty">Nenhum match ainda. Faça seu primeiro! 🚀</div>
      ) : (
        <div className="rank-list">
          {ranked.map((m, i) => (
            <div key={m.id} className="rank-item" onClick={() => viewMatch(m.id)}>
              <div className={'rank-medal' + (i === 0 ? ' g1' : i === 1 ? ' g2' : i === 2 ? ' g3' : '')}>
                {i + 1}
              </div>
              <div className="rank-main">
                <div className="rank-title">{vacancies[m.vacancyId]?.title || 'Vaga'}</div>
                <div className="rank-sub">{new Date(m.createdAt).toLocaleDateString('pt-BR')} · {scoreLabel(m.score)}</div>
                <div className="rank-bar"><i style={{ width: (m.score || 0) + '%', background: scoreColor(m.score) }} /></div>
              </div>
              <div className="rank-score" style={{ color: scoreColor(m.score) }}>{m.score}%</div>
              <button className="rank-del" title="Remover do ranking"
                onClick={(e) => { e.stopPropagation(); deleteMatch(m.id) }}>🗑</button>
            </div>
          ))}
        </div>
      )}

      {ranked.length > 1 && (
        <>
          <div className="section-title">📈 Evolução dos scores</div>
          <div className="card">
            <div className="timeline">
              {ranked.slice().reverse().map(m => (
                <div key={m.id} className={'bar' + ((m.score || 0) >= 70 ? ' hi' : '')}
                  style={{ height: ((m.score || 0) / maxBar * 100) + '%' }}
                  title={(vacancies[m.vacancyId]?.title || 'Vaga') + ': ' + m.score + '%'} />
              ))}
            </div>
          </div>
        </>
      )}

      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>}
    </div>
  )
}

function ScoreRing({ score }) {
  const r = 54
  const c = 2 * Math.PI * r
  const offset = c * (1 - (score || 0) / 100)
  const color = scoreColor(score)
  return (
    <svg width="160" height="160" viewBox="0 0 160 160">
      <circle cx="80" cy="80" r={r} fill="none" stroke="var(--surface-2)" strokeWidth="14" />
      <circle cx="80" cy="80" r={r} fill="none" stroke={color} strokeWidth="14"
        strokeDasharray={c} strokeDashoffset={offset} strokeLinecap="round" transform="rotate(-90 80 80)" />
      <text x="80" y="90" textAnchor="middle" fontSize="34" fontWeight="800" fill="var(--ink)">{score}%</text>
    </svg>
  )
}
