import React, { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'

export default function Navbar() {
  const { handleLogout, resumeName } = useApp()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  function handleNav() {
    setMenuOpen(false)
  }

  return (
    <header className="nav">
      <span className="brand" onClick={() => { navigate('/'); setMenuOpen(false) }} style={{ cursor: 'pointer' }}>
        <span className="dot">🎯</span> MatchIQ
      </span>

      <button className="nav-hamburger" onClick={() => setMenuOpen(v => !v)} aria-label="Menu">
        <span className={menuOpen ? 'open' : ''} />
      </button>

      <nav className={'nav-links' + (menuOpen ? ' open' : '')}>
        <NavLink to="/" end onClick={handleNav}>Dashboard</NavLink>
        <NavLink to="/vagas" onClick={handleNav}>Vagas</NavLink>
        <NavLink to="/cvs" onClick={handleNav}>Meus CVs</NavLink>
        <NavLink to="/candidaturas" onClick={handleNav}>Candidaturas</NavLink>
      </nav>

      <div className={'nav-right' + (menuOpen ? ' open' : '')}>
        {resumeName && <span className="nav-resume">📄 {resumeName}</span>}
        <button className="btn-ghost" onClick={() => { handleLogout(); setMenuOpen(false) }}>Sair</button>
      </div>
    </header>
  )
}
