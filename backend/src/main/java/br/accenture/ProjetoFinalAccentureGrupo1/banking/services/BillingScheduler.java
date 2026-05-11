package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

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
 * Agenda a execução diária do ciclo de cobrança do módulo banking.
 *
 * Três gatilhos disparam o mesmo método idempotente  #runDailyBilling():
 * 1. Scheduled @Scheduled(cron = "0 0 1 * * *")} — todo dia às 1h da manhã,enquanto o app está rodando.
 * 2. EventListener(ApplicationReadyEvent.class)} — uma vez no startup do app, recuperando dias
 * eventualmente perdidos enquanto ele esteve fora do ar.
 * 3. POST /banking/admin/billing/run-day} — disparo manual via endpoint administrativo
 * (atendido por um controller que delega a este componente).
 *
 * A robustez vem da lógica declarativa e idempotente dos métodos de InvoiceService:
 * eles processam o que precisa ser processado em vez de assumir que
 * hoje é exatamente o dia certo. Por isso é seguro rodar mais de uma vez no dia.
 */
@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);
    private final InvoiceService invoiceService;

    @Setter
    @Autowired
    private Clock clock;
    /*
     * Guarda contra execuções concorrentes. Mesmo com lógica idempotente, evitar
     * sobreposição entre um disparo manual e o cron poupa trabalho duplicado
     * (e mantém os logs legíveis). Como o estado real é persistido em banco,
     * qualquer execução perdida nessa janela é recuperada pelo próximo gatilho.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Gatilho do cron diário (1h da manhã, fuso do servidor).
    @Scheduled(cron = "0 0 1 * * *")
    public void scheduledDailyRun() {
        log.info("[billing] disparo agendado (cron) — iniciando ciclo diário");
        runDailyBilling();
    }

    /**
     * Gatilho de startup. Roda uma única vez assim que o {ApplicationContext}
     * fica pronto, garantindo que faturas que deveriam ter fechado ou sido cobradas
     * durante uma janela de inatividade sejam processadas imediatamente.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[billing] disparo de startup — recuperando dias eventualmente perdidos");
        runDailyBilling();
    }

    public void runDailyBilling() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[billing] ciclo diário já em execução — ignorando este disparo");
            return;
        }
        LocalDate today = LocalDate.now(clock);
        long t0 = System.currentTimeMillis();
        try {
            log.info("[billing] iniciando ciclo diário para today={}", today);

            int closed = invoiceService.closeDueInvoices();
            log.info("[billing] faturas fechadas nesta execução: {}", closed);

            int charged = invoiceService.chargeDueInvoices();
            log.info("[billing] faturas vencidas processadas nesta execução: {}", charged);

            log.info("[billing] ciclo diário concluído em {} ms (today={})",
                    System.currentTimeMillis() - t0, today);
        } catch (RuntimeException ex) {
            // Nunca deixar uma exceção escapar de um @Scheduled — isso desabilitaria
            // execuções futuras em algumas configurações. Logamos e seguimos.
            log.error("[billing] falha durante o ciclo diário (today={}): {}",
                    today, ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }
}
