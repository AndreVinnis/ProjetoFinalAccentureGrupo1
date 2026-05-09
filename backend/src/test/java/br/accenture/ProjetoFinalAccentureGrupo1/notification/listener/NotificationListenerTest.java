package br.accenture.ProjetoFinalAccentureGrupo1.notification.listener;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.*;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.*;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.config.RabbitConfig;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
// André Vinícius Barros Macambira
class NotificationListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationListener listener;

    @Test
    void onOrderPaid_DeveEnfileirarMensagem() {
        OrderPaidEvent event = new OrderPaidEvent(
                42L, 1L, "Ana", "ana@email.com",
                new BigDecimal("100.00"), "PIX", Instant.now()
        );

        listener.onOrderPaid(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EMAIL_QUEUE),
                any(EmailMessage.class)
        );
    }

    @Test
    void onOrderShipped_DeveEnfileirarMensagem() {
        OrderShippedEvent event = new OrderShippedEvent(
                42L, 1L, "Ana", "ana@email.com",
                Instant.now()
        );

        listener.onOrderShipped(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EMAIL_QUEUE),
                any(EmailMessage.class)
        );
    }

    @Test
    void onOrderDelivered_DeveEnfileirarMensagem() {
        OrderDeliveredEvent event = new OrderDeliveredEvent(
                42L, 1L, "Ana", "ana@email.com",
                Instant.now()
        );

        listener.onOrderDelivered(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EMAIL_QUEUE),
                any(EmailMessage.class)
        );
    }

    @Test
    void onOrderCancelled_DeveEnfileirarMensagem() {
        OrderCancelledEvent event = new OrderCancelledEvent(
                42L, 1L, "Ana", "ana@email.com",
                new BigDecimal("100.00"), true, Instant.now()
        );

        listener.onOrderCancelled(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EMAIL_QUEUE),
                any(EmailMessage.class)
        );
    }

    @Test
    void onInvoiceOverdue_DeveEnfileirarMensagem() {
        InvoiceOverdueEvent event = new InvoiceOverdueEvent(
                10L, 1L, "Ana", "ana@email.com",
                new BigDecimal("500.00"),
                LocalDate.of(2026, 5, 5),
                YearMonth.of(2026, 4)
        );

        listener.onInvoiceOverdue(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EMAIL_QUEUE),
                any(EmailMessage.class)
        );
    }

    @Test
    void onInvoicePaid_DeveEnfileirarMensagem() {
        InvoicePaidEvent event = new InvoicePaidEvent(
                10L, 1L, "Ana", "ana@email.com",
                new BigDecimal("500.00"),
                YearMonth.of(2026, 4),
                Instant.now()
        );

        listener.onInvoicePaid(event);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EMAIL_QUEUE),
                any(EmailMessage.class)
        );
    }
}