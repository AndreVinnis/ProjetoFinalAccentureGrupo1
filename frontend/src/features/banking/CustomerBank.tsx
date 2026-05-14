/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useState } from 'react'
import { BankMetric } from '../../components/ui/BankMetric'
import { Table } from '../../components/ui/Table'
import { settled } from '../../utils/async'
import { date, formatCardNumber, maskCard, money } from '../../utils/format'
import { downloadStatementPdf } from './statementPdf'

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

function normalizeCardPayload(card) {
  return card?.card || card?.data || card?.content || card
}

export function CustomerBank({ api }) {
  const [data, setData] = useState({})
  const [deposit, setDeposit] = useState({ amount: '', description: '' })
  const [pix, setPix] = useState('')
  const [invoicePay, setInvoicePay] = useState({ id: '', amount: '' })
  const [activeBankTab, setActiveBankTab] = useState('transactions')
  const [cardRevealed, setCardRevealed] = useState(false)
  const [cardDetailsError, setCardDetailsError] = useState('')
  const [activeActionModal, setActiveActionModal] = useState(null)
  const [pixFeedback, setPixFeedback] = useState({ status: 'idle', message: '' })

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
      card: normalizeCardPayload(settled(card)),
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
    closeActionModal()
    refresh()
  }

  async function payPix(event) {
    event.preventDefault()
    setPixFeedback({ status: 'loading', message: '' })

    try {
      await api.get(`/banking/pix/${pix}`)
      await api.post('/banking/pix/pay', { code: pix })
      setPix('')
      setPixFeedback({ status: 'success', message: '' })
      refresh()
    } catch {
      setPixFeedback({ status: 'error', message: 'Chave Pix errada.' })
    }
  }

  async function payInvoice(event) {
    event.preventDefault()
    await api.post(`/banking/invoices/${invoicePay.id}/pay`, { amount: Number(invoicePay.amount) })
    setInvoicePay({ id: '', amount: '' })
    closeActionModal()
    refresh()
  }

  function handleStatementDownload() {
    downloadStatementPdf(data)
    setActiveBankTab('transactions')
  }

  async function toggleCardReveal() {
    if (!cardRevealed) {
      setCardDetailsError('')

      try {
        const card = await api.get('/banking/cards/me')
        setData((current) => ({
          ...current,
          card: normalizeCardPayload(card),
        }))
      } catch {
        setCardDetailsError('Nao foi possivel carregar os dados do cartao.')
      }
    }

    setCardRevealed((current) => !current)
  }

  function openActionModal(action) {
    setActiveActionModal(action)
    setPixFeedback({ status: 'idle', message: '' })
  }

  function closeActionModal() {
    setActiveActionModal(null)
    setPixFeedback({ status: 'idle', message: '' })
  }

  const card = normalizeCardPayload(data.card)
  const cardNumber = card?.cardNumbers || card?.cardNumber || card?.number
  const cardHolder = card?.holderName || card?.name || 'Cliente ACC'
  const cardCvv = card?.cvv || card?.securityCode || '---'
  const cardExpirationMonth = card?.expirationMonth || card?.expiryMonth
  const cardExpirationYear = card?.expirationYear || card?.expiryYear
  const cardExpiration = cardExpirationMonth ? `${String(cardExpirationMonth).padStart(2, '0')}/${cardExpirationYear}` : '--/--'
  const hasCardDetails = Boolean(cardNumber && cardCvv !== '---' && cardExpirationMonth)

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
        <button className="shortcut-deposit" onClick={() => openActionModal('deposit')}>Depositar</button>
        <button className="shortcut-pix" onClick={() => openActionModal('pix')}>Pagar Pix</button>
        <button className="shortcut-invoice" onClick={() => openActionModal('invoice')}>Pagar fatura</button>
        <button className="shortcut-statement" onClick={handleStatementDownload}>Extrato</button>
      </section>
      <section className="bank-metrics">
        <BankMetric label="Limite disponivel" value={money(data.limit?.availableLimit)} helper={`Usado ${money(data.limit?.usedLimit)}`} progress={limitProgress(data.limit)} />
        <BankMetric label="Fatura atual" value={money(data.currentInvoice?.totalAmount)} helper={`${data.currentInvoice?.status || 'indisponivel'} - vence ${date(data.currentInvoice?.dueDate)}`} progress={invoiceProgress(data.currentInvoice)} />
        <BankMetric label="Lancamentos" value={data.transactions?.length || 0} helper="Movimentacoes recentes" progress={Math.min((data.transactions?.length || 0) * 12, 100)} />
      </section>
      <section className="bank-card-section">
        <button className={`credit-card-visual ${cardRevealed ? 'revealed' : ''}`} type="button" onClick={toggleCardReveal} aria-label={cardRevealed ? 'Ocultar dados do cartao' : 'Mostrar dados do cartao'}>
          <div className="card-face card-front">
            <div className="card-chip" />
            <span>ACC Platinum</span>
            <strong>{maskCard(cardNumber)}</strong>
            <div>
              <small>{cardHolder}</small>
              <small>{cardExpiration}</small>
            </div>
          </div>
          <div className="card-face card-back">
            <span>Dados completos do cartao</span>
            {hasCardDetails ? (
              <>
                <div className="card-full-number">
                  <small>Numero do cartao</small>
                  <strong>{formatCardNumber(cardNumber)}</strong>
                </div>
                <div className="card-secret-grid card-secret-grid-complete">
                  <small>Nome impresso <b>{cardHolder}</b></small>
                  <small>CVV <b>{cardCvv}</b></small>
                  <small>Validade <b>{cardExpiration}</b></small>
                </div>
              </>
            ) : (
              <div className="card-details-empty">
                <strong>{cardDetailsError || 'Dados do cartao indisponiveis'}</strong>
                <small>O endpoint /banking/cards/me nao retornou numero, CVV e validade para este usuario.</small>
              </div>
            )}
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
      {activeActionModal ? (
        <div className="bank-modal-backdrop" role="presentation" onMouseDown={closeActionModal}>
          <section className="bank-action-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
            <button className="modal-close-button" onClick={closeActionModal} aria-label="Fechar modal">x</button>

            {activeActionModal === 'deposit' ? (
              <form onSubmit={makeDeposit} className="action-card modal-form">
                <div><span>Deposito</span><strong>Adicionar saldo</strong></div>
                <input autoFocus placeholder="Valor" value={deposit.amount} onChange={(event) => setDeposit({ ...deposit, amount: event.target.value })} type="number" step="0.01" required />
                <input placeholder="Descricao opcional" value={deposit.description} onChange={(event) => setDeposit({ ...deposit, description: event.target.value })} />
                <button>Depositar agora</button>
              </form>
            ) : null}

            {activeActionModal === 'pix' ? (
              <form onSubmit={payPix} className="action-card modal-form">
                {pixFeedback.status === 'success' ? (
                  <div className="pix-success-state" role="status">
                    <span className="pix-success-check" aria-hidden="true" />
                    <strong>Pix pago</strong>
                    <small>Pagamento efetuado com sucesso pelo ACC Bank.</small>
                    <button type="button" onClick={closeActionModal}>Fechar</button>
                  </div>
                ) : (
                  <>
                    <div><span>Pix ecommerce</span><strong>Pagar pedido</strong></div>
                    <input
                      autoFocus
                      className={pixFeedback.status === 'error' ? 'input-error' : ''}
                      placeholder="Codigo Pix gerado na loja"
                      value={pix}
                      onChange={(event) => {
                        setPix(event.target.value)
                        if (pixFeedback.status === 'error') setPixFeedback({ status: 'idle', message: '' })
                      }}
                      required
                    />
                    {pixFeedback.status === 'error' ? <small className="field-error-message">{pixFeedback.message}</small> : null}
                    <button disabled={pixFeedback.status === 'loading'}>{pixFeedback.status === 'loading' ? 'Processando Pix...' : 'Pagar com saldo'}</button>
                  </>
                )}
              </form>
            ) : null}

            {activeActionModal === 'invoice' ? (
              <form onSubmit={payInvoice} className="action-card modal-form">
                <div><span>Cartao ACC</span><strong>Pagar fatura</strong></div>
                <input autoFocus placeholder="ID da fatura" value={invoicePay.id} onChange={(event) => setInvoicePay({ ...invoicePay, id: event.target.value })} required />
                <input placeholder="Valor" value={invoicePay.amount} onChange={(event) => setInvoicePay({ ...invoicePay, amount: event.target.value })} type="number" step="0.01" required />
                <button>Quitar fatura</button>
              </form>
            ) : null}
          </section>
        </div>
      ) : null}
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
