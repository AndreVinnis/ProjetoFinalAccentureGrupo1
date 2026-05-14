/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function BankMetric({ label, value, helper, progress }) {
  return (
    <article className="bank-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{helper}</small>
      <div className="metric-track">
        <i style={{ width: `${Math.max(0, Math.min(progress || 0, 100))}%` }} />
      </div>
    </article>
  )
}
