import React from 'react'
import { scoreColor } from '../lib/helpers'

export default function ScoreRing({ score }) {
  const r = 54
  const c = 2 * Math.PI * r
  const offset = c * (1 - (score || 0) / 100)
  const color = scoreColor(score)
  return (
    <svg width="160" height="160" viewBox="0 0 160 160">
      <circle cx="80" cy="80" r={r} fill="none" stroke="var(--surface-2)" strokeWidth="14" />
      <circle cx="80" cy="80" r={r} fill="none" stroke={color} strokeWidth="14"
        strokeDasharray={c} strokeDashoffset={offset} strokeLinecap="round" transform="rotate(-90 80 80)" />
      <text x="80" y="90" textAnchor="middle" fontSize="34" fontWeight="800" fill="var(--ink)">{score}%</text>
    </svg>
  )
}
