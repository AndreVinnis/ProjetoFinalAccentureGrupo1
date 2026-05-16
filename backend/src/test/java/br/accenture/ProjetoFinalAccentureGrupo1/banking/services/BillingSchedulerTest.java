package br.accenture.ProjetoFinalAccentureGrupo1.banking.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingScheduler - Unit Tests")
class BillingSchedulerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private BillingScheduler scheduler;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-16T01:00:00Z"), ZoneOffset.UTC);
        scheduler.setClock(fixedClock);
    }

    @Test
    @DisplayName("runDailyBilling chama closeDueInvoices e chargeDueInvoices nessa ordem")
    void runDailyBilling_executesCloseThenCharge() {
        when(invoiceService.closeDueInvoices()).thenReturn(2);
        when(invoiceService.chargeDueInvoices()).thenReturn(1);

        scheduler.runDailyBilling();

        InOrder order = inOrder(invoiceService);
        order.verify(invoiceService).closeDueInvoices();
        order.verify(invoiceService).chargeDueInvoices();
    }

    @Test
    @DisplayName("scheduledDailyRun delega para runDailyBilling")
    void scheduledDailyRun_delegatesToRunDailyBilling() {
        when(invoiceService.closeDueInvoices()).thenReturn(0);
        when(invoiceService.chargeDueInvoices()).thenReturn(0);

        scheduler.scheduledDailyRun();

        verify(invoiceService).closeDueInvoices();
        verify(invoiceService).chargeDueInvoices();
    }

    @Test
    @DisplayName("onApplicationReady delega para runDailyBilling")
    void onApplicationReady_delegatesToRunDailyBilling() {
        when(invoiceService.closeDueInvoices()).thenReturn(0);
        when(invoiceService.chargeDueInvoices()).thenReturn(0);

        scheduler.onApplicationReady();

        verify(invoiceService).closeDueInvoices();
        verify(invoiceService).chargeDueInvoices();
    }

    @Test
    @DisplayName("runDailyBilling não propaga RuntimeException — apenas loga")
    void runDailyBilling_doesNotPropagateRuntimeException() {
        doThrow(new RuntimeException("falha simulada"))
                .when(invoiceService).closeDueInvoices();

        // Não deve lançar exception
        scheduler.runDailyBilling();

        verify(invoiceService).closeDueInvoices();
        // chargeDueInvoices não é chamado porque closeDueInvoices falhou
        verify(invoiceService, times(0)).chargeDueInvoices();
    }

    @Test
    @DisplayName("runDailyBilling permite re-execução após terminar (reset do flag running)")
    void runDailyBilling_canBeCalledMultipleTimesSequentially() {
        when(invoiceService.closeDueInvoices()).thenReturn(0);
        when(invoiceService.chargeDueInvoices()).thenReturn(0);

        scheduler.runDailyBilling();
        scheduler.runDailyBilling();

        verify(invoiceService, times(2)).closeDueInvoices();
        verify(invoiceService, times(2)).chargeDueInvoices();
    }

    @Test
    @DisplayName("runDailyBilling reseta flag mesmo após RuntimeException")
    void runDailyBilling_resetsRunningFlagAfterException() {
        doThrow(new RuntimeException("falha intermitente"))
                .when(invoiceService).closeDueInvoices();

        scheduler.runDailyBilling();

        // Próxima execução: agora o método funciona
        doAnswer(inv -> 0).when(invoiceService).closeDueInvoices();
        when(invoiceService.chargeDueInvoices()).thenReturn(0);

        scheduler.runDailyBilling();

        verify(invoiceService, times(2)).closeDueInvoices();
        verify(invoiceService).chargeDueInvoices();
    }

    @Test
    @DisplayName("runDailyBilling ignora chamadas concorrentes via AtomicBoolean")
    void runDailyBilling_isThreadSafe_preventsConcurrentExecution() throws Exception {
        AtomicInteger concurrentExecutions = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch insideClose = new CountDownLatch(1);

        when(invoiceService.closeDueInvoices()).thenAnswer(inv -> {
            int n = concurrentExecutions.incrementAndGet();
            maxConcurrent.updateAndGet(prev -> Math.max(prev, n));
            // Aguarda a segunda thread chegar antes de liberar
            insideClose.countDown();
            Thread.sleep(50);
            concurrentExecutions.decrementAndGet();
            return 0;
        });
        when(invoiceService.chargeDueInvoices()).thenReturn(0);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> {
                try {
                    start.await();
                } catch (InterruptedException ignored) {
                }
                scheduler.runDailyBilling();
            });
            pool.submit(() -> {
                try {
                    start.await();
                    // Garante que a primeira thread entrou primeiro no método
                    insideClose.await(200, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }
                scheduler.runDailyBilling();
            });

            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            if (!pool.isTerminated()) {
                pool.shutdownNow();
            }
        }

        // Garante que nunca houve duas execuções simultâneas
        assertThat(maxConcurrent.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("runDailyBilling não interage com InvoiceService além dos dois métodos esperados")
    void runDailyBilling_onlyCallsExpectedMethods() {
        when(invoiceService.closeDueInvoices()).thenReturn(0);
        when(invoiceService.chargeDueInvoices()).thenReturn(0);

        scheduler.runDailyBilling();

        verify(invoiceService).closeDueInvoices();
        verify(invoiceService).chargeDueInvoices();
        // não há outras invocações
        org.mockito.Mockito.verifyNoMoreInteractions(invoiceService);
    }
}
