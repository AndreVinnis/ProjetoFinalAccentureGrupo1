/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function LoadingScreen({ title, detail, compact = false }) {
  return (
    <div className={`loading-screen ${compact ? 'compact' : ''}`}>
      <div className="loading-card">
        <div className="loading-mark">
          <span />
          <span />
          <span />
        </div>
        <strong>{title}</strong>
        <small>{detail}</small>
      </div>
    </div>
  )
}
