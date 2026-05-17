/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
export function Panel({ title, children, panelRef }) {
  return (
    <section className="panel" ref={panelRef}>
      <h2>{title}</h2>
      {children}
    </section>
  )
}
