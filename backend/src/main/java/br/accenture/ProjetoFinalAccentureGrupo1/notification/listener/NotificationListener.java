package br.accenture.ProjetoFinalAccentureGrupo1.notification.listener;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoiceOverdueEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoicePaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderCancelledEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderDeliveredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderPaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderShippedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.config.RabbitConfig;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.template.EmailTemplates;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/*
 * Escuta eventos publicados por banking e ecommerce e enfileira as
 * mensagens de e-mail no RabbitMQ.
 *
 * Usa AFTER_COMMIT pra garantir que só enfileira e-mail se a transação
 * de origem foi efetivamente salva no banco.
 *
 * Autor: André Vinícius Barros Macambira
 */

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, EmailTemplates.orderPaid(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderShipped(OrderShippedEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, EmailTemplates.orderShipped(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderDelivered(OrderDeliveredEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, EmailTemplates.orderDelivered(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, EmailTemplates.orderCancelled(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceOverdue(InvoiceOverdueEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, EmailTemplates.invoiceOverdue(event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoicePaid(InvoicePaidEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EMAIL_QUEUE, EmailTemplates.invoicePaid(event));
    }
}
