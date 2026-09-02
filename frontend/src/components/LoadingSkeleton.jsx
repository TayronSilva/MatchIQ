import React from 'react'

export function CardSkeleton({ lines = 3 }) {
  return (
    <div className="skeleton-card">
      <div className="skeleton-line skeleton-title" />
      {Array.from({ length: lines }).map((_, i) => (
        <div key={i} className="skeleton-line" style={{ width: `${60 + Math.random() * 35}%` }} />
      ))}
    </div>
  )
}

export function RankSkeleton({ count = 3 }) {
  return (
    <div className="rank-list">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="skeleton-rank-item">
          <div className="skeleton-circle" />
          <div className="skeleton-rank-body">
            <div className="skeleton-line skeleton-title" style={{ width: '70%' }} />
            <div className="skeleton-line" style={{ width: '40%' }} />
          </div>
        </div>
      ))}
    </div>
  )
}

export function StatsSkeleton() {
  return (
    <div className="stat-grid">
      {[1, 2, 3].map(i => (
        <div key={i} className="skeleton-stat">
          <div className="skeleton-line" style={{ width: '50%', height: 28, margin: '0 auto 6px' }} />
          <div className="skeleton-line" style={{ width: '60%', height: 12, margin: '0 auto' }} />
        </div>
      ))}
    </div>
  )
}
