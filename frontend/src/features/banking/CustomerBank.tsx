/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useState } from 'react'
import { BankMetric } from '../../components/ui/BankMetric'
import { Table } from '../../components/ui/Table'
import { settled } from '../../utils/async'
import { date, formatCardNumber, maskCard, money } from '../../utils/format'

function limitProgress(limit) {
  const total = Number(limit?.creditLimit || 0)
  if (!total) return 0
  return (Number(limit?.usedLimit || 0) / total) * 100
}

function invoiceProgress(invoice) {
  const total = Number(invoice?.totalAmount || 0)
  if (!total) return 0
  return (Number(invoice?.paidAmount || 0) / total) * 100
}

export function CustomerBank({ api }) {
  const [data, setData] = useState({})
  const [deposit, setDeposit] = useState({ amount: '', description: '' })
  const [pix, setPix] = useState('')
  const [invoicePay, setInvoicePay] = useState({ id: '', amount: '' })
  const [activeBankTab, setActiveBankTab] = useState('transactions')
  const [cardRevealed, setCardRevealed] = useState(false)

  const refresh = useCallback(async () => {
    const [account, balance, card, limit, purchases, invoices, currentInvoice, transactions] = await Promise.allSettled([
      api.get('/banking/accounts/me'),
      api.get('/banking/accounts/me/balance'),
      api.get('/banking/cards/me'),
      api.get('/banking/cards/me/limit'),
      api.get('/banking/cards/me/purchases'),
      api.get('/banking/invoices'),
      api.get('/banking/invoices/current'),
      api.get('/banking/accounts/me/transactions'),
    ])
    setData({
      account: settled(account),
      balance: settled(balance),
      card: settled(card),
      limit: settled(limit),
      purchases: settled(purchases, []),
      invoices: settled(invoices, []),
      currentInvoice: settled(currentInvoice),
      transactions: settled(transactions, []),
    })
  }, [api])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function makeDeposit(event) {
    event.preventDefault()
    await api.post('/banking/accounts/me/deposit', { amount: Number(deposit.amount), description: deposit.description })
    setDeposit({ amount: '', description: '' })
    refresh()
  }

  async function payPix(event) {
    event.preventDefault()
    await api.get(`/banking/pix/${pix}`)
    await api.post('/banking/pix/pay', { code: pix })
    setPix('')
    refresh()
  }

  async function payInvoice(event) {
    event.preventDefault()
    await api.post(`/banking/invoices/${invoicePay.id}/pay`, { amount: Number(invoicePay.amount) })
    setInvoicePay({ id: '', amount: '' })
    refresh()
  }

  return (
    <div className="bank-dashboard">
      <section className="bank-hero nubank-style">
        <div className="bank-greeting">
          <p className="eyebrow">ACC Bank</p>
          <h2>Olá, sua vida financeira está aqui.</h2>
          <small>Conta {data.account?.accountNumber || '--'} - {data.account?.status || 'carregando'}</small>
        </div>
        <div className="balance-panel">
          <span>Saldo em conta</span>
          <strong>{money(data.balance?.balance ?? data.account?.balance)}</strong>
          <small>Disponivel para Pix, deposito e compras no ecommerce.</small>
        </div>
      </section>
      <section className="bank-shortcuts" aria-label="Acoes rapidas do banco">
        <button onClick={() => document.getElementById('deposit-action')?.focus()}>Depositar</button>
        <button onClick={() => document.getElementById('pix-action')?.focus()}>Pagar Pix</button>
        <button onClick={() => document.getElementById('invoice-action')?.focus()}>Pagar fatura</button>
        <button onClick={() => setActiveBankTab('transactions')}>Extrato</button>
      </section>
      <section className="bank-metrics">
        <BankMetric label="Limite disponivel" value={money(data.limit?.availableLimit)} helper={`Usado ${money(data.limit?.usedLimit)}`} progress={limitProgress(data.limit)} />
        <BankMetric label="Fatura atual" value={money(data.currentInvoice?.totalAmount)} helper={`${data.currentInvoice?.status || 'indisponivel'} - vence ${date(data.currentInvoice?.dueDate)}`} progress={invoiceProgress(data.currentInvoice)} />
        <BankMetric label="Lancamentos" value={data.transactions?.length || 0} helper="Movimentacoes recentes" progress={Math.min((data.transactions?.length || 0) * 12, 100)} />
      </section>
      <section className="bank-card-section">
        <button className={`credit-card-visual ${cardRevealed ? 'revealed' : ''}`} type="button" onClick={() => setCardRevealed((current) => !current)} aria-label={cardRevealed ? 'Ocultar dados do cartao' : 'Mostrar dados do cartao'}>
          <div className="card-face card-front">
            <div className="card-chip" />
            <span>ACC Platinum</span>
            <strong>{maskCard(data.card?.cardNumbers)}</strong>
            <div>
              <small>{data.card?.holderName || 'Cliente ACC'}</small>
              <small>{data.card?.expirationMonth ? `${String(data.card.expirationMonth).padStart(2, '0')}/${data.card.expirationYear}` : '--/--'}</small>
            </div>
          </div>
          <div className="card-face card-back">
            <span>Dados completos do cartao</span>
            <strong>{formatCardNumber(data.card?.cardNumbers)}</strong>
            <div className="card-secret-grid">
              <small>Titular <b>{data.card?.holderName || 'Cliente ACC'}</b></small>
              <small>CVV <b>{data.card?.cvv || '---'}</b></small>
              <small>Validade <b>{data.card?.expirationMonth ? `${String(data.card.expirationMonth).padStart(2, '0')}/${data.card.expirationYear}` : '--/--'}</b></small>
              <small>Status <b>{data.card?.status || '--'}</b></small>
              <small>Limite <b>{money(data.card?.creditLimit)}</b></small>
              <small>Disponivel <b>{money(data.card?.availableLimit)}</b></small>
            </div>
            <em>Clique novamente para ocultar</em>
          </div>
        </button>
        <div className="card-summary-panel">
          <span>Cartao de credito</span>
          <strong>{money(data.currentInvoice?.totalAmount)}</strong>
          <small>Fatura atual - limite disponivel {money(data.limit?.availableLimit)}</small>
          <button onClick={() => setActiveBankTab('purchases')}>Ver compras</button>
        </div>
      </section>
      <section className="bank-actions">
        <form onSubmit={makeDeposit} className="action-card">
          <div><span>Deposito</span><strong>Adicionar saldo</strong></div>
          <input id="deposit-action" placeholder="Valor" value={deposit.amount} onChange={(event) => setDeposit({ ...deposit, amount: event.target.value })} type="number" step="0.01" required />
          <input placeholder="Descricao opcional" value={deposit.description} onChange={(event) => setDeposit({ ...deposit, description: event.target.value })} />
          <button>Depositar agora</button>
        </form>
        <form onSubmit={payPix} className="action-card">
          <div><span>Pix ecommerce</span><strong>Pagar pedido</strong></div>
          <input id="pix-action" placeholder="Codigo Pix gerado na loja" value={pix} onChange={(event) => setPix(event.target.value)} required />
          <button>Pagar com saldo</button>
        </form>
        <form onSubmit={payInvoice} className="action-card">
          <div><span>Cartao ACC</span><strong>Pagar fatura</strong></div>
          <input id="invoice-action" placeholder="ID da fatura" value={invoicePay.id} onChange={(event) => setInvoicePay({ ...invoicePay, id: event.target.value })} required />
          <input placeholder="Valor" value={invoicePay.amount} onChange={(event) => setInvoicePay({ ...invoicePay, amount: event.target.value })} type="number" step="0.01" required />
          <button>Quitar fatura</button>
        </form>
      </section>
      <section className="bank-activity panel">
        <div className="activity-header">
          <div><p className="eyebrow">Historico financeiro</p><h2>Movimentacoes da conta</h2></div>
          <div className="pill-tabs">
            <button className={activeBankTab === 'transactions' ? 'active' : ''} onClick={() => setActiveBankTab('transactions')}>Transacoes</button>
            <button className={activeBankTab === 'purchases' ? 'active' : ''} onClick={() => setActiveBankTab('purchases')}>Compras</button>
            <button className={activeBankTab === 'invoices' ? 'active' : ''} onClick={() => setActiveBankTab('invoices')}>Faturas</button>
          </div>
        </div>
        {activeBankTab === 'transactions' && <Table rows={data.transactions} columns={['id', 'type', 'amount', 'balanceAfter', 'description', 'createdAt']} />}
        {activeBankTab === 'purchases' && <Table rows={data.purchases} columns={['id', 'invoiceId', 'amount', 'description', 'reference', 'purchaseDate']} />}
        {activeBankTab === 'invoices' && <Table rows={data.invoices} columns={['id', 'referenceMonth', 'totalAmount', 'paidAmount', 'status', 'dueDate']} />}
      </section>
    </div>
  )
}
