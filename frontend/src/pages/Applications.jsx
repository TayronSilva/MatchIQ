import React, { useEffect } from 'react'
import { useApp } from '../context/AppContext'
import { statusLabel, STATUS_LABELS } from '../lib/helpers'

export default function Applications() {
  const { applications, vacancies, updateApplication, deleteApplication, loadApplications, error, success } = useApp()

  useEffect(() => { loadApplications() }, [])

  return (
    <div className="page">
      <div className="hero rise">
        <h1>Minhas candidaturas 📮</h1>
        <p>Acompanhe o status de cada aplicação e o que levar na entrevista.</p>
      </div>

      {applications.length === 0 ? (
        <div className="empty">Nenhuma candidatura registrada ainda. Marque "que apliquei" no detalhe de uma vaga.</div>
      ) : (
        <div className="rank-list">
          {applications.map(a => {
            const v = vacancies[a.vacancyId]
            return (
              <div key={a.id} className="rank-item" style={{ flexWrap: 'wrap' }}>
                <div className="rank-main" style={{ flex: '1 1 200px' }}>
                  <div className="rank-title">{v?.title || 'Vaga'}</div>
                  <div className="rank-sub">{new Date(a.appliedAt || a.createdAt).toLocaleDateString('pt-BR')} · {statusLabel(a.status)}</div>
                  {a.interviewAt && <div className="rank-sub">Entrevista: {new Date(a.interviewAt).toLocaleString('pt-BR')}</div>}
                  {v?.url && <div className="rank-sub"><a href={v.url} target="_blank" rel="noopener noreferrer">🔗 Ver vaga original</a></div>}
                </div>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                  <select className="btn mini" value={a.status}
                    onChange={(e) => updateApplication(a.id, { status: e.target.value })}>
                    {Object.keys(STATUS_LABELS).map(s => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
                  </select>
                  <button className="btn mini" onClick={() => deleteApplication(a.id)}>🗑</button>
                </div>
                {a.documents && a.documents.length > 0 && (
                  <div style={{ flexBasis: '100%' }}>
                    <div className="hint">Levar:</div>
                    <div className="chips">
                      {a.documents.map((d, i) => <span key={i} className="chip">{d}</span>)}
                    </div>
                  </div>
                )}
                <div style={{ flexBasis: '100%' }}>
                  <textarea className="ta" placeholder="Anotações (empresa, o que falar na entrevista...)" defaultValue={a.notes || ''}
                    onBlur={(e) => updateApplication(a.id, { notes: e.target.value })} />
                </div>
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
