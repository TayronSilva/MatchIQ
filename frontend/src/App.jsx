import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AppProvider, useApp } from './context/AppContext'
import ErrorBoundary from './components/ErrorBoundary'
import Navbar from './components/Navbar'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Vagas from './pages/Vagas'
import VacancyDetail from './pages/VacancyDetail'
import Cvs from './pages/Cvs'
import Applications from './pages/Applications'

function Protected({ children }) {
  const { token } = useApp()
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <ErrorBoundary>
      <AppProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<Protected><Navbar /><Dashboard /></Protected>} />
            <Route path="/vagas" element={<Protected><Navbar /><Vagas /></Protected>} />
            <Route path="/vaga/:id" element={<Protected><Navbar /><VacancyDetail /></Protected>} />
            <Route path="/cvs" element={<Protected><Navbar /><Cvs /></Protected>} />
            <Route path="/candidaturas" element={<Protected><Navbar /><Applications /></Protected>} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AppProvider>
    </ErrorBoundary>
  )
}
