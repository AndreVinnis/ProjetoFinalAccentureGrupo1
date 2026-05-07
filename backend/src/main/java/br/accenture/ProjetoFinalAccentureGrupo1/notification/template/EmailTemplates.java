package br.accenture.ProjetoFinalAccentureGrupo1.notification.template;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoiceOverdueEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.InvoicePaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderCancelledEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderDeliveredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderPaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderShippedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.notification.domain.EmailMessage;
import java.time.format.DateTimeFormatter;

// Constrói EmailMessage a partir dos eventos publicados pelos outros módulos.
// André Vinícius Barros Macambira
public class EmailTemplates {

    private EmailTemplates() {}

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static EmailMessage orderPaid(OrderPaidEvent e) {
        String subject = "Pedido #" + e.orderId() + " confirmado";
        String body = """
            Olá %s!
            
            Seu pedido #%d foi pago e está sendo preparado.
            Total: R$ %s
            Forma de pagamento: %s
            
            Em breve enviaremos atualizações sobre o envio.
            
            Obrigado pela compra!
            """.formatted(
                e.customerName(),
                e.orderId(),
                e.totalAmount(),
                formatPaymentMethod(e.paymentMethod())
        );
        return new EmailMessage(e.customerEmail(), subject, body, "ORDER_PAID");
    }

    public static EmailMessage orderShipped(OrderShippedEvent e) {
        String subject = "Seu pedido #" + e.orderId() + " saiu para entrega";
        String body = """
            Olá %s,
            
            Seu pedido #%d acabou de sair para entrega.
            Em alguns dias ele estará na sua casa.
            """.formatted(e.customerName(), e.orderId());
        return new EmailMessage(e.customerEmail(), subject, body, "ORDER_SHIPPED");
    }

    public static EmailMessage orderDelivered(OrderDeliveredEvent e) {
        String subject = "Pedido #" + e.orderId() + " entregue";
        String body = """
            Olá %s!
            
            Seu pedido #%d foi entregue. Esperamos que tenha gostado!
            
            Conte com a gente em uma próxima compra.
            """.formatted(e.customerName(), e.orderId());
        return new EmailMessage(e.customerEmail(), subject, body, "ORDER_DELIVERED");
    }

    public static EmailMessage orderCancelled(OrderCancelledEvent e) {
        String subject = "Pedido #" + e.orderId() + " cancelado";
        String body;
        if (e.refundIssued()) {
            body = """
                Olá %s,
                
                Seu pedido #%d foi cancelado e o estorno de R$ %s
                já foi creditado na sua conta.
                """.formatted(e.customerName(), e.orderId(), e.totalAmount());
        } else {
            body = """
                Olá %s,
                
                Seu pedido #%d foi cancelado antes do pagamento.
                Nenhuma cobrança foi efetuada.
                """.formatted(e.customerName(), e.orderId());
        }
        return new EmailMessage(e.customerEmail(), subject, body, "ORDER_CANCELLED");
    }

    public static EmailMessage invoiceOverdue(InvoiceOverdueEvent e) {
        String subject = "Sua fatura está em atraso";
        String body = """
            Olá %s,
            
            Sua fatura referente a %s no valor de R$ %s
            venceu em %s e ainda não foi paga.
            
            Sua conta está temporariamente restrita.
            Acesse o sistema e quite a fatura para liberar a conta.
            """.formatted(
                e.customerName(),
                e.referenceMonth(),
                e.amount(),
                e.dueDate().format(BR_DATE)
        );
        return new EmailMessage(e.customerEmail(), subject, body, "INVOICE_OVERDUE");
    }

    public static EmailMessage invoicePaid(InvoicePaidEvent e) {
        String subject = "Sua fatura foi paga";
        String body = """
            Olá %s!
            
            Recebemos o pagamento de R$ %s referente à fatura de %s.
            Sua conta está normalizada.
            
            Obrigado!
            """.formatted(
                e.customerName(),
                e.amountPaid(),
                e.referenceMonth()
        );
        return new EmailMessage(e.customerEmail(), subject, body, "INVOICE_PAID");
    }

    private static String formatPaymentMethod(String method) {
        return switch (method) {
            case "PIX" -> "PIX";
            case "CREDIT_CARD" -> "Cartão de Crédito";
            default -> method;
        };
    }
}