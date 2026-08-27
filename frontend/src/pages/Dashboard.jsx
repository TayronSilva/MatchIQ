import React from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useApp } from '../context/AppContext'
import { scoreColor, scoreLabel, statusLabel } from '../lib/helpers'

export default function Dashboard() {
  const {
    resumeId, resumeName, resumes, showUpload, setShowUpload,
    handleUploadResume, selectResume, deleteResume,
    matches, vacancies, dashboardLoading,
    tailorList, applications, appsByVacancy,
    downloadTailoredPdf, createApplication, deleteMatch, clearMatches, updateApplication,
    error, success
  } = useApp()
  const navigate = useNavigate()

  const ranked = [...matches].sort((a, b) => (b.score || 0) - (a.score || 0))
  const best = ranked.length ? ranked[0].score : 0
  const avg = ranked.length ? Math.round(ranked.reduce((s, m) => s + (m.score || 0), 0) / ranked.length) : 0
  const maxBar = Math.max(1, ...ranked.map(m => m.score || 0))

  return (
    <div className="page">
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
          <p className="hint">Nenhum currículo enviado ainda. Envie um PDF ou Word para começar.</p>
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
            <button className="btn" type="submit">Enviar currículo</button>
          </form>
        )}
      </div>

      {/* Atalho para vagas */}
      <Link to="/vagas" className="card-link">
        <div className="card rise link-card">
          <div className="card-title"><span className="step-no">2</span> Vagas & Análise</div>
          <p className="hint">Adicione vagas (link ou texto), analise seu encaixe e gere um CV tailor-made.</p>
          <span className="btn-soft">Ir para Vagas →</span>
        </div>
      </Link>

      <div className="section-head">
        <h2>🏆 Ranking de vagas</h2>
        {ranked.length > 0 && (
          <button className="btn-danger" onClick={clearMatches} title="Limpar tudo">🗑 Limpar</button>
        )}
      </div>

      {dashboardLoading ? (
        <p className="hint">Carregando…</p>
      ) : ranked.length === 0 ? (
        <div className="empty">Nenhum match ainda. Vá em <Link to="/vagas">Vagas</Link> e faça seu primeiro! 🚀</div>
      ) : (
        <div className="rank-list">
          {ranked.map((m, i) => (
            <div key={m.id} className="rank-item" onClick={() => navigate('/vaga/' + m.id)}>
              <div className={'rank-medal' + (i === 0 ? ' g1' : i === 1 ? ' g2' : i === 2 ? ' g3' : '')}>
                {i + 1}
              </div>
              <div className="rank-main">
                <div className="rank-title">{vacancies[m.vacancyId]?.title || 'Vaga'}</div>
                <div className="rank-sub">{new Date(m.createdAt).toLocaleDateString('pt-BR')} · {scoreLabel(m.score)}</div>
                <div className="rank-bar"><i style={{ width: (m.score || 0) + '%', background: scoreColor(m.score) }} /></div>
              </div>
              <div className="rank-score" style={{ color: scoreColor(m.score) }}>{m.score}%</div>
              {appsByVacancy[m.vacancyId] ? (
                <span className="tag" title="Candidatura registrada">✅ Aplicado</span>
              ) : (
                <button className="rank-del" title="Marcar candidatura"
                  onClick={(e) => { e.stopPropagation(); createApplication(m.vacancyId, resumeId, m.id) }}>➕</button>
              )}
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

      <div className="section-head">
        <h2>📂 Meus CVs gerados</h2>
        <Link to="/cvs" className="btn-soft">Ver todos</Link>
      </div>
      {tailorList.length === 0 ? (
        <div className="empty">Nenhum currículo gerado ainda.</div>
      ) : (
        <div className="rank-list">
          {tailorList.slice(0, 3).map(t => (
            <div key={t.id} className="rank-item" onClick={() => navigate('/cvs')}>
              <div className="rank-main">
                <div className="rank-title">{vacancies[t.vacancyId]?.title || 'Vaga'}</div>
                <div className="rank-sub">
                  {new Date(t.createdAt).toLocaleDateString('pt-BR')} · {t.status === 'COMPLETED' ? 'Pronto' : t.status === 'PENDING' ? 'Gerando…' : 'Falhou'}
                </div>
              </div>
              {t.status === 'COMPLETED' && (
                <button className="btn mini" onClick={(e) => { e.stopPropagation(); downloadTailoredPdf(t.id) }}>⬇ PDF</button>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="section-head">
        <h2>📮 Minhas candidaturas</h2>
        <Link to="/candidaturas" className="btn-soft">Ver todas</Link>
      </div>
      {applications.length === 0 ? (
        <div className="empty">Nenhuma candidatura registrada ainda.</div>
      ) : (
        <div className="rank-list">
          {applications.slice(0, 3).map(a => {
            const v = vacancies[a.vacancyId]
            return (
              <div key={a.id} className="rank-item" onClick={() => navigate('/candidaturas')}>
                <div className="rank-main">
                  <div className="rank-title">{v?.title || 'Vaga'}</div>
                  <div className="rank-sub">{new Date(a.appliedAt || a.createdAt).toLocaleDateString('pt-BR')} · {statusLabel(a.status)}</div>
                </div>
                <span className="tag">{statusLabel(a.status)}</span>
              </div>
            )
          })}
        </div>
      )}

      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>}
    </div>
  )
}
