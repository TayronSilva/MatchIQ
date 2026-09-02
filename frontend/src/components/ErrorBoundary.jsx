import React from 'react'

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary caught:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary">
          <div className="error-boundary-icon">⚠️</div>
          <h2>Algo deu errado</h2>
          <p>Ocorreu um erro inesperado. Tente recarregar a página.</p>
          <button className="btn" onClick={() => window.location.reload()}>
            Recarregar
          </button>
          {import.meta.env.DEV && this.state.error && (
            <pre className="error-boundary-details">{this.state.error.message}</pre>
          )}
        </div>
      )
    }
    return this.props.children
  }
}
