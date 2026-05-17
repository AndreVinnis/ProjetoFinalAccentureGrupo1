/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Panel } from '../../components/ui/Panel'
import { TablePanel } from '../../components/ui/Table'
import { settled } from '../../utils/async'
import { money } from '../../utils/format'

export function AdminBank({ api }) {
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [deposit, setDeposit] = useState({ id: '', amount: '', description: '' })
  const [invoiceId, setInvoiceId] = useState('')
  const [openInvoices, setOpenInvoices] = useState([])
  const [emails, setEmails] = useState([])
  const [feedback, setFeedback] = useState('')
  const [busyAction, setBusyAction] = useState('')

  const totalBalance = useMemo(
    () => accounts.reduce((total, account) => total + Number(account.balance || 0), 0),
    [accounts]
  )

  const blockedAccounts = useMemo(
    () => accounts.filter((account) => String(account.status).toUpperCase() !== 'ACTIVE'),
    [accounts]
  )

  const movedValue = useMemo(
    () => transactions.reduce((total, transaction) => total + Math.abs(Number(transaction.amount || 0)), 0),
    [transactions]
  )

  const refresh = useCallback(async () => {
    const [accountList, transactionList, openInvoiceList, emailPage] = await Promise.allSettled([
      api.get('/banking/admin'),
      api.get('/banking/admin/accounts/transactions'),
      api.get('/banking/admin/billing/invoices/open'),
      api.get('/admin/notifications/emails'),
    ])

    setAccounts(settled(accountList, []))
    setTransactions(settled(transactionList, []))
    setOpenInvoices(settled(openInvoiceList, []))
    setEmails(settled(emailPage)?.content || [])
  }, [api])

  useEffect(() => { refresh() }, [refresh])

  async function runAction(action, callback, successMessage) {
    setBusyAction(action)

    try {
      await callback()
      setFeedback(successMessage)
      await refresh()
    } finally {
      setBusyAction('')
    }
  }

  async function adminDeposit(event) {
    event.preventDefault()

    await runAction(
      'deposit',
      async () => {
        await api.post(`/banking/admin/accounts/${deposit.id}/deposit`, {
          amount: Number(deposit.amount),
          description: deposit.description
        })
        setDeposit({ id: '', amount: '', description: '' })
      },
      'Deposito administrativo realizado.'
    )
  }

  async function closeInvoice(event) {
    event.preventDefault()

    await runAction(
      'close-invoice',
      async () => {
        await api.post(`/banking/admin/billing/invoices/${invoiceId}/close`)
        setInvoiceId('')
      },
      'Fatura fechada com sucesso.'
    )
  }

  async function runBillingAction(action, endpoint, message) {
    await runAction(
      action,
      async () => api.post(endpoint),
      message
    )
  }

  async function toggleAccountStatus(account) {
    const isBlocked = String(account.status).toUpperCase() === 'BLOCKED'
    const action = isBlocked ? 'unblock' : 'block'

    await runAction(
      `${action}-${account.id}`,
      async () => api.post(`/banking/admin/accounts/${account.id}/${action}`),
      isBlocked ? 'Conta desbloqueada.' : 'Conta bloqueada.'
    )
  }

  function fillDeposit(account) {
    setDeposit({
      id: String(account.id),
      amount: '',
      description: `Deposito administrativo conta ${account.accountNumber || account.id}`
    })
  }

  function invoiceLabel(invoice) {
    const amount = money(Number(invoice.totalAmount || 0) - Number(invoice.paidAmount || 0))
    const month = invoice.referenceMonth || 'sem mes'
    const closing = invoice.closingDate ? `fecha em ${invoice.closingDate}` : 'sem fechamento'

    return `#${invoice.id} - ${month} - ${amount} - ${closing}`
  }

  return (
    <div className="dashboard-grid admin bank-admin-workspace">
      <section className="bank-admin-hero">
        <div>
          <span>Central Banking Admin</span>
          <h2>Operacao bancaria, contas, faturas e notificacoes.</h2>
          <p>Monitore saldos, bloqueios, transacoes e ciclos de cobranca em uma visao operacional para administradores.</p>
        </div>

        <div className="bank-admin-metrics" aria-label="Resumo bancario">
          <article>
            <i className="bank-admin-icon bank-admin-icon-account" aria-hidden="true" />
            <strong>{accounts.length}</strong>
            <small>contas</small>
          </article>
          <article>
            <i className="bank-admin-icon bank-admin-icon-balance" aria-hidden="true" />
            <strong>{money(totalBalance)}</strong>
            <small>saldo total</small>
          </article>
          <article>
            <i className="bank-admin-icon bank-admin-icon-alert" aria-hidden="true" />
            <strong>{blockedAccounts.length}</strong>
            <small>restritas/bloqueadas</small>
          </article>
          <article>
            <i className="bank-admin-icon bank-admin-icon-transfer" aria-hidden="true" />
            <strong>{money(movedValue)}</strong>
            <small>movimentado</small>
          </article>
        </div>
      </section>

      {feedback ? (
        <p className="toast-banner bank-admin-feedback">{feedback}</p>
      ) : null}

      <Panel title="Operacoes bancarias">
        <div className="bank-admin-actions">
          <form onSubmit={adminDeposit} className="bank-admin-card">
            <span>Operacao manual</span>
            <strong>Deposito em conta</strong>
            <input placeholder="ID conta" value={deposit.id} onChange={(event) => setDeposit({ ...deposit, id: event.target.value })} required />
            <input placeholder="Valor" value={deposit.amount} onChange={(event) => setDeposit({ ...deposit, amount: event.target.value })} type="number" step="0.01" min="0.01" required />
            <input placeholder="Descricao" value={deposit.description} onChange={(event) => setDeposit({ ...deposit, description: event.target.value })} />
            <button disabled={busyAction === 'deposit'}>{busyAction === 'deposit' ? 'Depositando...' : 'Depositar'}</button>
          </form>

          <form className="bank-admin-card" onSubmit={closeInvoice}>
            <span>Faturas</span>
            <strong>Fechar fatura</strong>
            <select value={invoiceId} onChange={(event) => setInvoiceId(event.target.value)} required>
              <option value="">Selecione uma fatura aberta</option>
              {openInvoices.map((invoice) => (
                <option key={invoice.id} value={invoice.id}>
                  {invoiceLabel(invoice)}
                </option>
              ))}
            </select>
            <button disabled={busyAction === 'close-invoice'}>{busyAction === 'close-invoice' ? 'Fechando...' : 'Fechar fatura'}</button>
          </form>

          <div className="bank-admin-card bank-admin-cycle-card">
            <span>Ciclo de cobranca</span>
            <strong>Rotinas financeiras</strong>
            <button
              onClick={() => runBillingAction('charge-overdue', '/banking/admin/billing/charge-overdue', 'Cobranca de faturas vencidas executada.')}
              disabled={busyAction === 'charge-overdue'}
            >
              Cobrar vencidas
            </button>
            <button
              onClick={() => runBillingAction('run-day', '/banking/admin/billing/run-day', 'Ciclo diario executado.')}
              disabled={busyAction === 'run-day'}
            >
              Rodar ciclo diario
            </button>
          </div>
        </div>
      </Panel>

      <TablePanel title="Contas" rows={accounts} columns={['id', 'userId', 'accountNumber', 'balance', 'accountType', 'status', 'createdAt']} action={(account) => (
        <div className="button-row tight bank-account-actions">
          <button type="button" onClick={() => fillDeposit(account)}>Depositar</button>
          <button
            type="button"
            className={String(account.status).toUpperCase() === 'BLOCKED' ? '' : 'danger-button'}
            onClick={() => toggleAccountStatus(account)}
            disabled={busyAction === `block-${account.id}` || busyAction === `unblock-${account.id}`}
          >
            {String(account.status).toUpperCase() === 'BLOCKED' ? 'Desbloquear' : 'Bloquear'}
          </button>
        </div>
      )} />

      <TablePanel title="Todas transacoes" rows={transactions} columns={['id', 'type', 'amount', 'balanceAfter', 'reference', 'description', 'createdAt']} />
      <TablePanel title="Emails do banco" rows={emails} columns={['id', 'toEmail', 'subject', 'type', 'status', 'createdAt']} />
    </div>
  )
}
