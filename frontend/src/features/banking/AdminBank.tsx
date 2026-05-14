/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useState } from 'react'
import { Panel } from '../../components/ui/Panel'
import { TablePanel } from '../../components/ui/Table'
import { settled } from '../../utils/async'

export function AdminBank({ api }) {
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [deposit, setDeposit] = useState({ id: '', amount: '', description: '' })
  const [invoiceId, setInvoiceId] = useState('')
  const [emails, setEmails] = useState([])

  const refresh = useCallback(async () => {
    const [accountList, transactionList, emailPage] = await Promise.allSettled([
      api.get('/banking/admin'),
      api.get('/banking/admin/accounts/transactions'),
      api.get('/admin/notifications/emails'),
    ])
    setAccounts(settled(accountList, []))
    setTransactions(settled(transactionList, []))
    setEmails(settled(emailPage)?.content || [])
  }, [api])

  useEffect(() => { refresh() }, [refresh])

  async function adminDeposit(event) {
    event.preventDefault()
    await api.post(`/banking/admin/accounts/${deposit.id}/deposit`, { amount: Number(deposit.amount), description: deposit.description })
    setDeposit({ id: '', amount: '', description: '' })
    refresh()
  }

  return (
    <div className="dashboard-grid admin">
      <Panel title="Operacoes bancarias">
        <form onSubmit={adminDeposit} className="inline-form">
          <input placeholder="ID conta" value={deposit.id} onChange={(event) => setDeposit({ ...deposit, id: event.target.value })} required />
          <input placeholder="Valor" value={deposit.amount} onChange={(event) => setDeposit({ ...deposit, amount: event.target.value })} type="number" step="0.01" required />
          <input placeholder="Descricao" value={deposit.description} onChange={(event) => setDeposit({ ...deposit, description: event.target.value })} />
          <button>Depositar</button>
        </form>
        <form className="inline-form" onSubmit={(event) => { event.preventDefault(); api.post(`/banking/admin/billing/invoices/${invoiceId}/close`).then(() => setInvoiceId('')) }}>
          <input placeholder="ID fatura" value={invoiceId} onChange={(event) => setInvoiceId(event.target.value)} required />
          <button>Fechar fatura</button>
        </form>
        <div className="button-row">
          <button onClick={() => api.post('/banking/admin/billing/charge-overdue')}>Cobrar vencidas</button>
          <button onClick={() => api.post('/banking/admin/billing/run-day')}>Rodar ciclo diario</button>
        </div>
      </Panel>
      <TablePanel title="Contas" rows={accounts} columns={['id', 'userId', 'accountNumber', 'balance', 'accountType', 'status', 'createdAt']} action={(account) => (
        <div className="button-row tight">
          <button onClick={() => api.post(`/banking/admin/accounts/${account.id}/block`).then(refresh)}>Bloquear</button>
          <button onClick={() => api.post(`/banking/admin/accounts/${account.id}/unblock`).then(refresh)}>Desbloquear</button>
        </div>
      )} />
      <TablePanel title="Todas transacoes" rows={transactions} columns={['id', 'type', 'amount', 'balanceAfter', 'reference', 'description', 'createdAt']} />
      <TablePanel title="Emails do banco" rows={emails} columns={['id', 'toEmail', 'subject', 'type', 'status', 'createdAt']} />
    </div>
  )
}
