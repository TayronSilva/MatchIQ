export function scoreColor(s) {
  if (s >= 70) return 'var(--ok)'
  if (s >= 40) return 'var(--warn)'
  return 'var(--miss)'
}

export function scoreLabel(s) {
  if (s >= 70) return 'Ótimo encaixe'
  if (s >= 50) return 'Bom potencial'
  return 'Distante da vaga'
}

export function buildDiagnosis(score, matched, missing) {
  let head
  if (score >= 80) head = 'Excelente compatibilidade! Seu currículo atende à maior parte dos requisitos da vaga.'
  else if (score >= 50) head = 'Boa compatibilidade. Você já tem boa parte do que a vaga pede.'
  else if (score > 0) head = 'Compatibilidade moderada. Há skills importantes a desenvolver.'
  else head = 'Compatibilidade baixa. A vaga exige skills que ainda não identificamos no currículo.'
  const have = matched.length
    ? 'Skills que você já domina: ' + matched.join(', ') + '.'
    : 'Nenhuma skill do cruzamento foi detectada no currículo.'
  const need = missing.length
    ? 'Reforce: ' + missing.join(', ') + '.'
    : 'Você atende a todas as skills exigidas!'
  return head + '\n\n' + have + '\n' + need
}

export function buildPlan(missing) {
  if (!missing.length) return 'Nenhum plano necessário: você já domina as skills exigidas pela vaga.'
  return 'Plano sugerido (ordem recomendada):\n' +
    missing.map((m, i) => (i + 1) + '. Estudar ' + m + ' — foque em fundamentos e pratique com projetos reais.').join('\n') +
    '\nDica: monte um projeto de portfólio combinando essas skills para comprovar na prática.'
}

export const STATUS_LABELS = {
  APPLIED: 'Candidatado',
  INTERVIEW: 'Entrevista',
  OFFER: 'Oferta',
  REJECTED: 'Recusado',
  WITHDRAWN: 'Desistiu'
}
export function statusLabel(s) { return STATUS_LABELS[s] || s }
