import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'

export default function Login() {
  const { token, handleLogin, handleRegister, error, success } = useApp()
  const [authTab, setAuthTab] = useState('login')
  const navigate = useNavigate()

  useEffect(() => {
    if (token) navigate('/', { replace: true })
  }, [token, navigate])

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
