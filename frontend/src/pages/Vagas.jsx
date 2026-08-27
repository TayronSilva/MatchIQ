import React from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useApp } from '../context/AppContext'

export default function Vagas() {
  const {
    resumeId, vacancies,
    vacancyMode, setVacancyMode, vacancyHint, vacancyNeedsInfo, vacancyId,
    handleVacancyLink, handleVacancyManual, handleUpdateVacancy,
    createMatch, loading, error, success,
    api, loadDashboard, showSuccess, showError
  } = useApp()
  const navigate = useNavigate()

  async function analyze(vid) {
    const m = await createMatch(vid)
    if (m) navigate('/vaga/' + m.id)
  }

  async function removeVacancy(id) {
    if (!window.confirm('Excluir esta vaga? Os matches dela também serão removidos.')) return
    try {
      await api('/v1/vacancies/' + id, { method: 'DELETE' })
      showSuccess('Vaga excluída.')
      loadDashboard()
    } catch (e) { showError(e) }
  }

  const list = Object.values(vacancies)

  return (
    <div className="page">
      <div className="hero rise">
        <h1>Vagas & Análise 🔍</h1>
        <p>Adicione a vaga e veja o quanto seu currículo combina. Depois gere um CV tailor-made.</p>
      </div>

      {!resumeId && (
        <div className="banner info">
          Envie seu currículo primeiro — vá ao <Link to="/">Dashboard</Link> e adicione um PDF/Word.
        </div>
      )}

      <div className="card rise">
        <div className="card-title">➕ Adicionar vaga</div>
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
              <label htmlFor="url">Link da vaga (opcional)</label>
              <input id="url" name="url" placeholder="https://..." />
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

      <div className="section-head">
        <h2>📋 Vagas cadastradas ({list.length})</h2>
      </div>
      {list.length === 0 ? (
        <div className="empty">Nenhuma vaga ainda. Adicione a primeira acima. 🚀</div>
      ) : (
        <div className="rank-list">
          {list.map(v => (
            <div key={v.id} className="rank-item" style={{ flexWrap: 'wrap' }}>
              <div className="rank-main" style={{ flex: '1 1 200px' }}>
                <div className="rank-title">{v.title || 'Vaga'}</div>
                <div className="rank-sub">{v.company || ''}</div>
                {v.url && <div className="rank-sub"><a href={v.url} target="_blank" rel="noopener noreferrer">🔗 link da vaga</a></div>}
              </div>
              <button className="btn mini" disabled={!resumeId || loading}
                onClick={() => analyze(v.id)}>
                💘 Analisar
              </button>
              <button className="btn mini btn-danger" onClick={() => removeVacancy(v.id)}>🗑</button>
            </div>
          ))}
        </div>
      )}

      {error && <div className="banner error">{error}</div>}
      {success && <div className="banner success">{success}</div>}
    </div>
  )
}
