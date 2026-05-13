package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * Agenda a execução diária do ciclo de vida dos pedidos e carrinhos do ecommerce.
 *
 * Três gatilhos disparam o mesmo método idempotente #runDailyLifecycle():
 * 1. Scheduled @Scheduled(cron = "0 0 2 * * *") — todo dia às 2h da manhã.
 * 2. EventListener(ApplicationReadyEvent.class) — uma vez no startup do app.
 * 3. Disparo manual via endpoint administrativo (POST /admin/orders/run-lifecycle).
 *
 * A idempotência garante que a lógica de transição processe apenas o que atingiu
 * a maturidade de tempo (ex: 1 dia para envio, 5 dias para entrega), sendo seguro
 * rodar múltiplas vezes.
 */
@Component
@RequiredArgsConstructor
public class OrderLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderLifecycleScheduler.class);

    private final OrderService orderService;
    private final CartService cartService;

    @Setter
    @Autowired
    private Clock clock;

    private final AtomicBoolean running = new AtomicBoolean(false);

    // Gatilho do cron diário (2h da manhã, fuso do servidor).
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledDailyRun() {
        log.info("[ecommerce-lifecycle] disparo agendado (cron) — iniciando ciclo diário");
        runDailyLifecycle();
    }

    /**
     * Gatilho de startup para recuperar dias de inatividade.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[ecommerce-lifecycle] disparo de startup — recuperando dias eventualmente perdidos");
        runDailyLifecycle();
    }

    public void runDailyLifecycle() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[ecommerce-lifecycle] ciclo diário já em execução — ignorando este disparo");
            return;
        }
        LocalDate today = LocalDate.now(clock);
        long t0 = System.currentTimeMillis();
        try {
            log.info("[ecommerce-lifecycle] iniciando ciclo diário para today={}", today);

            // 1. Transição de PAID para SHIPPED (ex: após 1 dia do pagamento)
            int shipped = orderService.transitionToShipped();
            log.info("[ecommerce-lifecycle] pedidos enviados (SHIPPED) nesta execução: {}", shipped);

            // 2. Transição de SHIPPED para DELIVERED (ex: após 5 dias do envio)
            int delivered = orderService.transitionToDelivered();
            log.info("[ecommerce-lifecycle] pedidos entregues (DELIVERED) nesta execução: {}", delivered);

            // 3. Liberação de estoques de carrinhos abandonados
            int expiredReservations = cartService.releaseExpiredReservations();
            log.info("[ecommerce-lifecycle] reservas de carrinho expiradas e revertidas nesta execução: {}", expiredReservations);

            log.info("[ecommerce-lifecycle] ciclo diário concluído em {} ms (today={})",
                    System.currentTimeMillis() - t0, today);
        } catch (RuntimeException ex) {
            log.error("[ecommerce-lifecycle] falha durante o ciclo diário (today={}): {}",
                    today, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }
}