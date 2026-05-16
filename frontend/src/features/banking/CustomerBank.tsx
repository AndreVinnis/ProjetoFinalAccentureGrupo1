/* eslint-disable @typescript-eslint/ban-ts-comment */
/* eslint-disable react-hooks/set-state-in-effect */
// @ts-nocheck
import { useCallback, useEffect, useState } from 'react'
import { BankMetric } from '../../components/ui/BankMetric'
import { Table } from '../../components/ui/Table'
import { settled } from '../../utils/async'
import { date, digitsOnly, formatCardNumber, maskCard, money } from '../../utils/format'
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
  const [pix, setPix] = useState({ code: '', password: '' })
  const [pixTransfer, setPixTransfer] = useState({ recipientEmail: '', amount: '', description: '', password: '' })
  const [pixMode, setPixMode] = useState('payment')
  const [cardPassword, setCardPassword] = useState('')
  const [invoicePay, setInvoicePay] = useState({ id: '', amount: '' })
  const [activeBankTab, setActiveBankTab] = useState('transactions')
  const [cardRevealed, setCardRevealed] = useState(false)
  const [cardDetailsError, setCardDetailsError] = useState('')
  const [activeActionModal, setActiveActionModal] = useState(null)
  const [pixFeedback, setPixFeedback] = useState({ status: 'idle', message: '' })

  const refresh = useCallback(async () => {
    const silent = { silent: true }
    const [account, balance, limit, purchases, invoices, currentInvoice, transactions] = await Promise.allSettled([
      api.get('/banking/accounts/me', silent),
      api.get('/banking/accounts/me/balance', silent),
      api.get('/banking/cards/me/limit', silent),
      api.get('/banking/cards/me/purchases', silent),
      api.get('/banking/invoices', silent),
      api.get('/banking/invoices/current', silent),
      api.get('/banking/accounts/me/transactions', silent),
    ])
    setData((current) => ({
      ...current,
      account: settled(account),
      balance: settled(balance),
      limit: settled(limit),
      purchases: settled(purchases, []),
      invoices: settled(invoices, []),
      currentInvoice: settled(currentInvoice),
      transactions: settled(transactions, []),
    }))
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
      await api.get(`/banking/pix/${pix.code}`)
      await api.post('/banking/pix/pay', { code: pix.code, password: pix.password })
      setPix({ code: '', password: '' })
      setPixFeedback({ status: 'success', message: 'Pix pago com sucesso pelo ACC Bank.' })
      refresh()
    } catch {
      setPixFeedback({ status: 'error', message: 'Codigo Pix ou senha da conta incorretos.' })
    }
  }

  async function transferPix(event) {
    event.preventDefault()
    setPixFeedback({ status: 'loading', message: '' })

    try {
      await api.post('/banking/pix/transfer', {
        recipientEmail: pixTransfer.recipientEmail,
        amount: Number(pixTransfer.amount),
        description: pixTransfer.description,
        password: pixTransfer.password,
      })
      setPixTransfer({ recipientEmail: '', amount: '', description: '', password: '' })
      setPixFeedback({ status: 'success', message: 'Transferencia Pix enviada com sucesso.' })
      refresh()
    } catch {
      setPixFeedback({ status: 'error', message: 'Nao foi possivel concluir o Pix. Confira os dados e a senha ACC.' })
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

      if (!hasCardDetails) {
        if (!/^\d{4}$/.test(cardPassword)) {
          setCardDetailsError('Digite a senha da conta ACC com 4 digitos.')
          setCardRevealed(true)
          return
        }

        try {
          const card = await api.post('/banking/cards/me', { password: cardPassword })
          setData((current) => ({
            ...current,
            card: normalizeCardPayload(card),
          }))
          setCardPassword('')
        } catch {
          setCardDetailsError('Senha invalida ou dados do cartao indisponiveis.')
        }
      }
    }

    setCardRevealed((current) => !current)
  }

  function openActionModal(action) {
    setActiveActionModal(action)
    setPixFeedback({ status: 'idle', message: '' })
    if (action === 'pix') setPixMode('payment')
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
        <button className="shortcut-pix" onClick={() => openActionModal('pix')}>PIX</button>
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
              </div>
            )}
            <em>Clique novamente para ocultar</em>
          </div>
        </button>
        <div className="card-summary-panel">
          <span>Cartao de credito</span>
          <strong>{money(data.currentInvoice?.totalAmount)}</strong>
          <small>Fatura atual - limite disponivel {money(data.limit?.availableLimit)}</small>
          {!hasCardDetails ? (
            <input
              className="card-password-input"
              placeholder="Senha ACC de 4 digitos"
              value={cardPassword}
              onChange={(event) => setCardPassword(digitsOnly(event.target.value).slice(0, 4))}
              type="password"
              inputMode="numeric"
              maxLength="4"
            />
          ) : null}
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
              <form onSubmit={pixMode === 'payment' ? payPix : transferPix} className="action-card modal-form">
                {pixFeedback.status === 'success' ? (
                  <div className="pix-success-state" role="status">
                    <span className="pix-success-check" aria-hidden="true" />
                    <strong>Pix concluido</strong>
                    <small>{pixFeedback.message}</small>
                    <button type="button" onClick={closeActionModal}>Fechar</button>
                  </div>
                ) : (
                  <>
                    <div><span>PIX</span><strong>{pixMode === 'payment' ? 'Pagar codigo Pix' : 'Transferir saldo'}</strong></div>
                    <div className="pix-mode-tabs" aria-label="Escolher tipo de Pix">
                      <button type="button" className={pixMode === 'payment' ? 'active' : ''} onClick={() => { setPixMode('payment'); setPixFeedback({ status: 'idle', message: '' }) }}>Pagar codigo</button>
                      <button type="button" className={pixMode === 'transfer' ? 'active' : ''} onClick={() => { setPixMode('transfer'); setPixFeedback({ status: 'idle', message: '' }) }}>Transferencia</button>
                    </div>
                    {pixMode === 'payment' ? (
                      <>
                        <input
                          autoFocus
                          className={pixFeedback.status === 'error' ? 'input-error' : ''}
                          placeholder="Codigo Pix gerado na loja"
                          value={pix.code}
                          onChange={(event) => {
                            setPix({ ...pix, code: event.target.value })
                            if (pixFeedback.status === 'error') setPixFeedback({ status: 'idle', message: '' })
                          }}
                          required
                        />
                        <input
                          className={pixFeedback.status === 'error' ? 'input-error' : ''}
                          placeholder="Senha ACC de 4 digitos"
                          value={pix.password}
                          onChange={(event) => {
                            setPix({ ...pix, password: digitsOnly(event.target.value).slice(0, 4) })
                            if (pixFeedback.status === 'error') setPixFeedback({ status: 'idle', message: '' })
                          }}
                          type="password"
                          inputMode="numeric"
                          maxLength="4"
                          required
                        />
                      </>
                    ) : (
                      <>
                        <input
                          autoFocus
                          className={pixFeedback.status === 'error' ? 'input-error' : ''}
                          placeholder="E-mail do destinatario"
                          value={pixTransfer.recipientEmail}
                          onChange={(event) => {
                            setPixTransfer({ ...pixTransfer, recipientEmail: event.target.value })
                            if (pixFeedback.status === 'error') setPixFeedback({ status: 'idle', message: '' })
                          }}
                          type="email"
                          required
                        />
                        <input
                          className={pixFeedback.status === 'error' ? 'input-error' : ''}
                          placeholder="Valor da transferencia"
                          value={pixTransfer.amount}
                          onChange={(event) => {
                            setPixTransfer({ ...pixTransfer, amount: event.target.value })
                            if (pixFeedback.status === 'error') setPixFeedback({ status: 'idle', message: '' })
                          }}
                          type="number"
                          step="0.01"
                          required
                        />
                        <input
                          placeholder="Descricao opcional"
                          value={pixTransfer.description}
                          onChange={(event) => setPixTransfer({ ...pixTransfer, description: event.target.value })}
                        />
                        <input
                          className={pixFeedback.status === 'error' ? 'input-error' : ''}
                          placeholder="Senha ACC de 4 digitos"
                          value={pixTransfer.password}
                          onChange={(event) => {
                            setPixTransfer({ ...pixTransfer, password: digitsOnly(event.target.value).slice(0, 4) })
                            if (pixFeedback.status === 'error') setPixFeedback({ status: 'idle', message: '' })
                          }}
                          type="password"
                          inputMode="numeric"
                          maxLength="4"
                          required
                        />
                      </>
                    )}
                    {pixFeedback.status === 'error' ? <small className="field-error-message">{pixFeedback.message}</small> : null}
                    <button disabled={pixFeedback.status === 'loading'}>{pixFeedback.status === 'loading' ? 'Processando Pix...' : pixMode === 'payment' ? 'Pagar codigo' : 'Transferir agora'}</button>
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
