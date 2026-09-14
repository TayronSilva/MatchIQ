import React, { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useApp } from '../context/AppContext'
import ScoreRing from '../components/ScoreRing'
import { scoreColor, scoreLabel, buildDiagnosis, buildPlan, statusLabel, STATUS_LABELS } from '../lib/helpers'

const BREAKDOWN_LABELS = {
  competencias: 'Competências',
  senioridade: 'Senioridade',
  regiao: 'Região/Modalidade',
  recencia: 'Recência',
  preferencias: 'Preferências'
}
const BREAKDOWN_MAX = { competencias: 40, senioridade: 20, regiao: 20, recencia: 10, preferencias: 10 }

export default function VacancyDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { api, resumeId, vacancies, appsByVacancy, createApplication, updateApplication, shareLinkedIn,
    downloadTailoredPdf, showError, showSuccess, loadTailorList, deleteMatch, error, success } = useApp()

  const [matchResult, setMatchResult] = useState(null)
  const [genStuck, setGenStuck] = useState(false)
  const [tailor, setTailor] = useState(null)
  const [tailorBusy, setTailorBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [vacancy, setVacancy] = useState(null)
  const pollRef = useRef(null)

  useEffect(() => {
    if (pollRef.current) clearInterval(pollRef.current)
    load()
    return () => { if (pollRef.current) clearInterval(pollRef.current) }
    // eslint-disable-next-line
  }, [id])

  async function load() {
    setLoading(true)
    setNotFound(false)
    if (pollRef.current) clearInterval(pollRef.current)
    try {
      const match = await api(`/v1/matches/${id}`)
      const v = await api(`/v1/vacancies/${match.vacancyId}`).catch(() => null)
      setVacancy(v)
      const [analysis, recommendation] = await Promise.all([
        api(`/v1/analyses/match/${id}`).catch(() => null),
        api(`/v1/recommendations/match/${id}`).catch(() => null)
      ])
      setMatchResult({ match, analysis, recommendation })
      const done = (s) => !s || s === 'COMPLETED' || s === 'FAILED'
      if (!done(analysis && analysis.status) || !done(recommendation && recommendation.status)) {
        poll(id)
      }
    } catch (e) { setNotFound(true); showError(e) }
    finally { setLoading(false) }
  }

  function poll(matchId) {
    const start = Date.now()
    let nullCount = 0
    pollRef.current = setInterval(async () => {
      try {
        const [analysis, recommendation] = await Promise.all([
          api(`/v1/analyses/match/${matchId}`).catch(() => null),
          api(`/v1/recommendations/match/${matchId}`).catch(() => null)
        ])
        if (!analysis && !recommendation) {
          nullCount++
          if (nullCount >= 5) {
            clearInterval(pollRef.current)
            setGenStuck(true)
            return
          }
        }
        const safeA = analysis || { status: 'PENDING' }
        const safeR = recommendation || { status: 'PENDING' }
        setMatchResult(prev => prev ? ({ ...prev, analysis: safeA, recommendation: safeR }) : prev)
        const done = (s) => !s || s === 'COMPLETED' || s === 'FAILED'
        const timedOut = Date.now() - start > 30000
        if ((done(safeA.status) && done(safeR.status)) || timedOut) {
          clearInterval(pollRef.current)
          if (timedOut) setGenStuck(true)
        }
      } catch (e) { /* mantém pollando */ }
    }, 2000)
  }

  async function generateTailoredCv(vid, rid) {
    if (!rid || !vid) { showError('Selecione um currículo e uma vaga antes de gerar o CV.'); return }
    setTailorBusy(true); setTailor(null)
    try {
      const created = await api('/v1/tailor', {
        method: 'POST',
        body: JSON.stringify({ resumeId: rid, vacancyId: vid })
      })
      setTailor(created)
      for (let i = 0; i < 30; i++) {
        await new Promise(r => setTimeout(r, 2500))
        const s = await api('/v1/tailor/' + created.id)
        setTailor(s)
        if (s.status === 'COMPLETED' || s.status === 'FAILED') break
      }
    } catch (e) { showError(e) }
    finally { setTailorBusy(false); loadTailorList() }
  }

  if (loading) {
    return <div className="page"><div className="hero"><p className="hint">Carregando…</p></div></div>
  }
  if (notFound) {
    return (
      <div className="page">
        <div className="hero"><h1>Match não encontrado</h1>
          <button className="btn" onClick={() => navigate('/')}>← Dashboard</button></div>
      </div>
    )
  }

  const { match, analysis, recommendation } = matchResult
  const tailorVid = (match && match.vacancyId) || ''
  const tailorRid = (match && match.resumeId) || resumeId
  const analysisLoading = !analysis || analysis.status === 'PENDING'
  const analysisFailed = analysis && analysis.status === 'FAILED'
  const recommendationLoading = !recommendation || recommendation.status === 'PENDING'
  const recommendationFailed = recommendation && recommendation.status === 'FAILED'
  const matched = match.matchedSkills || []
  const missing = match.missingSkills || []
  const breakdown = match.scoreBreakdown || null

  return (
    <div className="page">
      <button className="btn-ghost" onClick={() => navigate('/')}>← Dashboard</button>

      {vacancy && (
        <div className="card">
          <h2>📋 Vaga analisada</h2>
          <div className="vac-title">{vacancy.title || 'Vaga'}</div>
          {vacancy.company && <div className="hint">{vacancy.company}</div>}
          {vacancy.url ? (
            <p className="hint" style={{ marginTop: 8 }}>
              <a href={vacancy.url} target="_blank" rel="noopener noreferrer">🔗 Ver vaga original</a>
            </p>
          ) : (
            <p className="hint" style={{ marginTop: 8 }}>Sem link da vaga salvo.</p>
          )}
        </div>
      )}

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

      {breakdown && (
        <div className="card">
          <h2>📊 Detalhamento do score</h2>
          <div className="breakdown">
            {Object.entries(BREAKDOWN_LABELS).map(([key, label]) => {
              const val = breakdown[key] || 0
              const max = BREAKDOWN_MAX[key]
              const pct = Math.round((val / max) * 100)
              return (
                <div key={key} className="breakdown-row">
                  <span className="breakdown-label">{label}</span>
                  <div className="breakdown-bar">
                    <i style={{ width: pct + '%', background: scoreColor(Math.round(pct * 0.7)) }} />
                  </div>
                  <span className="breakdown-val">{val}/{max}</span>
                </div>
              )
            })}
          </div>
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
        {recommendationLoading && !genStuck ? (
          <p className="hint">Gerando plano com IA…</p>
        ) : recommendation && recommendation.studyPlan ? (
          <div className="md"><ReactMarkdown remarkPlugins={[remarkGfm]}>{recommendation.studyPlan}</ReactMarkdown></div>
        ) : genStuck ? (
          <div className="md"><ReactMarkdown remarkPlugins={[remarkGfm]}>{buildPlan(missing)}</ReactMarkdown></div>
        ) : (
          <p className="hint">Sem plano de estudos.</p>
        )}
        {recommendation && recommendation.source === 'AI' && !recommendationLoading && <p className="tag">✨ Gerado com IA</p>}
      </div>

      <div className="card">
        <h2>📄 Currículo para esta vaga</h2>
        <button className="btn" onClick={() => generateTailoredCv(tailorVid, tailorRid)}
          disabled={tailorBusy || !tailorVid || !tailorRid}>
          {tailorBusy ? 'Gerando currículo…' : 'Gerar CV tailor-made'}
        </button>
        {!tailorRid && <p className="hint">Selecione um currículo primeiro.</p>}
        {tailor && tailor.status === 'PENDING' && <p className="hint">Gerando currículo com IA…</p>}
        {tailor && tailor.status === 'FAILED' && <p className="hint">Não foi possível gerar o currículo agora. Tente novamente.</p>}
        {tailor && tailor.status === 'COMPLETED' && tailor.contentMarkdown && (
          <>
            <div className="md"><ReactMarkdown remarkPlugins={[remarkGfm]}>{tailor.contentMarkdown}</ReactMarkdown></div>
            <button className="btn" onClick={() => downloadTailoredPdf(tailor.id)}>⬇ Baixar PDF</button>
          </>
        )}
      </div>

      <div className="card">
        <h2>📮 Candidatura</h2>
        {appsByVacancy[tailorVid] ? (() => {
          const app = appsByVacancy[tailorVid]
          return (
            <>
              <p className="hint">Candidatura registrada ({statusLabel(app.status)}).</p>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                <select className="btn mini" value={app.status}
                  onChange={(e) => updateApplication(app.id, { status: e.target.value })}>
                  {Object.keys(STATUS_LABELS).map(s => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
                </select>
                <button className="btn-soft" onClick={() => navigate('/candidaturas')}>Ver no tracker</button>
              </div>
            </>
          )
        })() : (
          <button className="btn" onClick={() => createApplication(tailorVid, tailorRid, match && match.id)}>
            Marcar que apliquei
          </button>
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button className="btn" onClick={() => navigate('/')}>← Voltar ao dashboard</button>
        <button className="btn-soft" style={{ color: '#dc3545', borderColor: '#dc3545' }} onClick={() => {
          if (window.confirm('Excluir este match e todas as análises?')) {
            deleteMatch(match.id).then(() => navigate('/'))
          }
        }}>🗑 Excluir match</button>
      </div>
      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>}
    </div>
  )
}
