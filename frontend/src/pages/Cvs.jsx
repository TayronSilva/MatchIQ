import React, { useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useApp } from '../context/AppContext'

export default function Cvs() {
  const { tailorList, vacancies, downloadTailoredPdf, loadTailorList, api, showSuccess, showError, error, success } = useApp()
  const navigate = useNavigate()

  useEffect(() => { loadTailorList() }, [])

  async function removeTailor(id) {
    if (!window.confirm('Excluir este currículo gerado?')) return
    try {
      await api('/v1/tailor/' + id, { method: 'DELETE' })
      showSuccess('Currículo excluído.')
      loadTailorList()
    } catch (e) { showError(e) }
  }

  return (
    <div className="page">
      <div className="hero rise">
        <h1>Meus CVs 📄</h1>
        <p>Currículos gerados pela IA, alinhados a cada vaga.</p>
      </div>

      {tailorList.length === 0 ? (
        <div className="empty">Nenhum currículo gerado ainda. Vá em <Link to="/vagas">Vagas</Link>, analise uma vaga e clique em "Gerar CV tailor-made".</div>
      ) : (
        <div className="rank-list">
          {tailorList.map(t => (
            <div key={t.id} className="rank-item" style={{ flexWrap: 'wrap' }}>
              <div className="rank-main" style={{ flex: '1 1 200px' }}>
                <div className="rank-title">{vacancies[t.vacancyId]?.title || 'Vaga'}</div>
                <div className="rank-sub">
                  {new Date(t.createdAt).toLocaleDateString('pt-BR')} · {t.status === 'COMPLETED' ? 'Pronto' : t.status === 'PENDING' ? 'Gerando…' : 'Falhou'}
                </div>
              </div>
              {t.status === 'COMPLETED' && (
                <button className="btn mini" onClick={() => downloadTailoredPdf(t.id)}>⬇ Baixar PDF</button>
              )}
              <button className="btn mini btn-danger" onClick={() => removeTailor(t.id)}>🗑</button>
            </div>
          ))}
        </div>
      )}

      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>}
    </div>
  )
}
