package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleSchedulerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderLifecycleScheduler scheduler;

    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-05-13T10:00:00Z"), ZoneId.of("UTC"));
        scheduler.setClock(fixedClock);
    }

    @Test
    @DisplayName("Deve chamar todos os serviços de transição e expiração com sucesso")
    void runDailyLifecycle_ShouldCallServicesAndCompleteSuccessfully() {
        when(orderService.transitionToShipped()).thenReturn(5);
        when(orderService.transitionToDelivered()).thenReturn(10);
        when(cartService.releaseExpiredReservations()).thenReturn(3);

        scheduler.runDailyLifecycle();

        verify(orderService, times(1)).transitionToShipped();
        verify(orderService, times(1)).transitionToDelivered();
        verify(cartService, times(1)).releaseExpiredReservations();
    }

    @Test
    @DisplayName("Deve ignorar execuções simultâneas se já estiver rodando (testando o AtomicBoolean)")
    void runDailyLifecycle_ShouldIgnoreConcurrentExecutions() {
        doAnswer(invocation -> {
            scheduler.runDailyLifecycle();
            return 1;
        }).when(orderService).transitionToShipped();

        scheduler.runDailyLifecycle();

        verify(orderService, times(1)).transitionToShipped();
        verify(orderService, times(1)).transitionToDelivered();
        verify(cartService, times(1)).releaseExpiredReservations();
    }

    @Test
    @DisplayName("Deve capturar exceções e sempre liberar o lock no bloco finally")
    void runDailyLifecycle_ShouldCatchExceptionsAndReleaseLock() {
        when(orderService.transitionToShipped()).thenThrow(new RuntimeException("Falha de conexão com o banco"));

        scheduler.runDailyLifecycle();

        verify(orderService, times(1)).transitionToShipped();
        verify(orderService, never()).transitionToDelivered();
        verify(cartService, never()).releaseExpiredReservations();


        doReturn(1).when(orderService).transitionToShipped();
        scheduler.runDailyLifecycle();
        verify(orderService, times(2)).transitionToShipped();
        verify(orderService, times(1)).transitionToDelivered();
        verify(cartService, times(1)).releaseExpiredReservations();
    }

    @Test
    @DisplayName("Gatilho do cron deve acionar a lógica principal")
    void scheduledDailyRun_ShouldTriggerLifecycle() {
        scheduler.scheduledDailyRun();
        verify(orderService, times(1)).transitionToShipped();
    }

    @Test
    @DisplayName("Gatilho de startup (ApplicationReadyEvent) deve acionar a lógica principal")
    void onApplicationReady_ShouldTriggerLifecycle() {
        scheduler.onApplicationReady();
        verify(orderService, times(1)).transitionToShipped();
    }
}