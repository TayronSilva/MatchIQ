import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useApp } from '../context/AppContext'

export default function Navbar() {
  const { handleLogout, resumeName } = useApp()
  const navigate = useNavigate()
  return (
    <header className="nav">
      <span className="brand" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
        <span className="dot">🎯</span> MatchIQ
      </span>
      <nav className="nav-links">
        <NavLink to="/" end>Dashboard</NavLink>
        <NavLink to="/vagas">Vagas</NavLink>
        <NavLink to="/cvs">Meus CVs</NavLink>
        <NavLink to="/candidaturas">Candidaturas</NavLink>
      </nav>
      <div className="nav-right">
        {resumeName && <span className="nav-resume">📄 {resumeName}</span>}
        <button className="btn-ghost" onClick={handleLogout}>Sair</button>
      </div>
    </header>
  )
}
