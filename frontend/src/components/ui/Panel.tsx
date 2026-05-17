/* eslint-disable @typescript-eslint/ban-ts-comment */
// @ts-nocheck
import type { ReactNode, Ref } from 'react'

interface PanelProps {
  title: ReactNode
  children: ReactNode
  panelRef?: Ref<HTMLElement>
}

export function Panel({ title, children, panelRef }: PanelProps) {
  return (
    <section className="panel" ref={panelRef}>
      <h2>{title}</h2>
      {children}
    </section>
  )
}
