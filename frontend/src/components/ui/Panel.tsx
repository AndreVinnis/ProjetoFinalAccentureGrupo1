/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function Panel({ title, children }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {children}
    </section>
  )
}
