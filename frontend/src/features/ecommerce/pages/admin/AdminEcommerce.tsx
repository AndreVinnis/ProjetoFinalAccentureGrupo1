import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../../../components/ui/Panel'
import { TablePanel } from '../../../../components/ui/Table'
import { settled } from '../../../../utils/async'
import type { ApiClient } from '../../../../services/api'

export function AdminEcommerce({ api }: { api: ApiClient }) {
  const [emails, setEmails] = useState([])
  // Você pode adicionar estados para products e orders aqui depois

  const refresh = useCallback(async () => {
    const [emailPage] = await Promise.allSettled([
      api.get('/admin/notifications/emails'),
    ])
    
    const emailData = settled(emailPage) as any
    setEmails(emailData?.content || [])
  }, [api])

  useEffect(() => { refresh() }, [refresh])

  return (
    <div className="dashboard-grid admin ecommerce">
      <Panel title="Administração da Loja">
        <p>Ações de gerencialmento de produtos, reposição de estoque, etc.</p>
        {/* Adicione os forms de produtos/categorias aqui depois */}
      </Panel>
      
      {/* Tabela de Log de E-mails compartilhada com o Banco */}
      <TablePanel 
        title="Log de E-mails Enviados" 
        rows={emails} 
        columns={['id', 'toEmail', 'subject', 'type', 'status', 'createdAt']} 
      />
    </div>
  )
}
