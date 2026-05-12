package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.listeners;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.events.PaymentReceivedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
// Autor: André Vinícius Barros Macambira
public class PaymentReceivedListener {

    private final OrderService orderService;

    @EventListener
    public void onPaymentReceived(PaymentReceivedEvent event){
        orderService.confirmPaidByPix(event.reference(), event.payerUserId());
    }
}
