package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.api.BankingFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Cart;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Order;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.OrderItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.SavedCard;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.DiscountApplication;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.OrderResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.OrderStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderCancelledEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderPaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartWasNotClosedException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.OrderNotFound;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.OrderDiscountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role.CUSTOMER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CartService cartService;
    @Mock private CartRepository cartRepository;
    @Mock private ProductService productService;
    @Mock private CustomerService customerService;
    @Mock private OrderRepository orderRepository;
    @Mock private BankingFacade bankingFacade;
    @Mock private UserFacade userFacade;
    @Mock private SavedCardService savedCardService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private DiscountService discountService;
    @Mock private OrderDiscountRepository orderDiscountRepository;
    @Mock private Clock clock;

    @InjectMocks
    private OrderService orderService;

    private static final String EMAIL = "ana@email.com";

    private Customer customer;
    private Product product;
    private Cart cart;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .userId(10L)
                .quantityPurchases(0)
                .shippingAddress("Rua A, 123")
                .phone("11999999999")
                .build();

        product = Product.builder()
                .id(100L)
                .name("Mouse Gamer")
                .price(new BigDecimal("150.00"))
                .totalStock(10)
                .reservedStock(2)
                .active(true)
                .build();

        CartItem item = CartItem.builder()
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("150.00"))
                .build();

        cart = Cart.builder()
                .id(50L)
                .customer(customer)
                .status(CartStatus.RESERVED)
                .build();
        cart.getItems().add(item);
        item.setCart(cart);
    }

    @Test
    void checkoutPix_DeveSalvarOrderPendenteEEnviarCobranca_ComDesconto() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(cart));
        when(cartService.isClosed(cart)).thenReturn(true);

        DiscountApplication discount = new DiscountApplication("TEST_RULE", "Desc Teste", new BigDecimal("15.00"));
        when(discountService.applyAll(eq(customer), any(), eq(PaymentMethod.PIX)))
                .thenReturn(List.of(discount));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(200L);
            return o;
        });

        when(bankingFacade.createPaymentRequest(eq(new BigDecimal("285.00")), any(), eq("ORDER-200")))
                .thenReturn("PIX-XYZ");

        String code = orderService.checkoutPix(EMAIL);

        assertEquals("PIX-XYZ", code);
        ArgumentCaptor<Order> orderCap = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCap.capture());
        Order saved = orderCap.getValue();
        assertEquals(new BigDecimal("300.00"), saved.getSubtotal());
        assertEquals(new BigDecimal("15.00"), saved.getDiscountTotal());
        assertEquals(new BigDecimal("285.00"), saved.getTotalAmount());
        verify(orderDiscountRepository).save(any());
    }

    @Test
    void checkoutPix_DeveLancarException_QuandoCarrinhoNaoEstaFechado() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(cart));
        when(cartService.isClosed(cart)).thenReturn(false);

        assertThrows(CartWasNotClosedException.class, () -> orderService.checkoutPix(EMAIL));
        verify(orderRepository, never()).save(any());
        verify(bankingFacade, never()).createPaymentRequest(any(), any(), any());
    }

    @Test
    void checkoutCard_DeveCobrarCartaoSalvoECriarPedidoPago_QuandoCarrinhoFechado() {
        SavedCard savedCard = SavedCard.builder()
                .id(30L)
                .customer(customer)
                .bankingCardId(300L)
                .last4Digits("1111")
                .build();
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(cart));
        when(cartService.isClosed(cart)).thenReturn(true);
        when(savedCardService.findByIdAndCustomer(30L, 1L)).thenReturn(savedCard);
        when(userFacade.findByEmail(EMAIL)).thenReturn(userInfo());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(201L);
            return o;
        });

        OrderResponse response = orderService.checkoutCard(EMAIL, 30L, "123");

        assertEquals(201L, response.orderId());
        assertEquals(OrderStatus.PAID, response.status());
        assertEquals(PaymentMethod.CREDIT_CARD, response.paymentMethod());
        verify(bankingFacade).chargeCard(eq(300L), eq(new BigDecimal("300.00")), eq("123"), any(), eq("ORDER-201"));
        verify(productService).consumeReserved(100L, 2);
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertEquals(0, cart.getItems().size());

        ArgumentCaptor<OrderPaidEvent> evCap = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(eventPublisher).publishEvent(evCap.capture());
        assertEquals("CREDIT_CARD", evCap.getValue().paymentMethod());
    }

    @Test
    void confirmPaidByPix_DeveMarcarPagoEConsumirEstoqueELimparCarrinho() {
        Order order = orderWithItem(200L, OrderStatus.PENDING, PaymentMethod.PIX);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(cartRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(cart));
        when(userFacade.findById(10L)).thenReturn(userInfo());

        orderService.confirmPaidByPix("ORDER-200", 10L);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertNotNull(order.getPaidAt());
        verify(productService).consumeReserved(100L, 2);
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertEquals(0, cart.getItems().size());

        ArgumentCaptor<OrderPaidEvent> evCap = ArgumentCaptor.forClass(OrderPaidEvent.class);
        verify(eventPublisher).publishEvent(evCap.capture());
        OrderPaidEvent ev = evCap.getValue();
        assertEquals(200L, ev.orderId());
        assertEquals(10L, ev.userId());
        assertEquals("Ana Silva", ev.customerName());
        assertEquals("PIX", ev.paymentMethod());
    }

    @Test
    void confirmPaidByPix_DeveLancarException_QuandoOrderNaoEstaPendente() {
        Order order = orderWithItem(200L, OrderStatus.PAID, PaymentMethod.PIX);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.confirmPaidByPix("ORDER-200", 10L));
        verify(productService, never()).consumeReserved(any(), anyInt());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void confirmPaidByPix_DeveLancarOrderNotFound_QuandoOrderNaoExiste() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFound.class, () -> orderService.confirmPaidByPix("ORDER-999", 10L));
    }

    @Test
    void cancelByPixExpiration_DeveCancelarELiberarEstoqueELimparCarrinho() {
        Order order = orderWithItem(200L, OrderStatus.PENDING, PaymentMethod.PIX);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(cartRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(cart));
        when(customerService.findById(1L)).thenReturn(customer);
        when(userFacade.findById(10L)).thenReturn(userInfo());

        orderService.cancelByPixExpiration("ORDER-200");

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertNotNull(order.getCancelledAt());
        verify(productService).releaseReservation(100L, 2);
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertEquals(0, cart.getItems().size());

        ArgumentCaptor<OrderCancelledEvent> evCap = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishEvent(evCap.capture());
        OrderCancelledEvent ev = evCap.getValue();
        assertEquals(200L, ev.orderId());
        assertFalse(ev.refundIssued());
        assertEquals("ana@email.com", ev.customerEmail());
    }

    @Test
    void cancel_PendingPath_DeveLiberarEstoqueELimparCarrinhoSemEstorno() {
        Order order = orderWithItem(200L, OrderStatus.PENDING, PaymentMethod.PIX);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(cart));
        when(userFacade.findByEmail(EMAIL)).thenReturn(userInfo());

        orderService.cancel(200L, EMAIL);

        verify(bankingFacade).cancelPaymentRequest("ORDER-200");
        verify(productService).releaseReservation(100L, 2);
        verify(bankingFacade, never()).issueRefund(any(), any(), any(), any());
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertEquals(0, cart.getItems().size());

        ArgumentCaptor<OrderCancelledEvent> evCap = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishEvent(evCap.capture());
        assertFalse(evCap.getValue().refundIssued());
    }

    @Test
    void cancel_PaidPath_DeveEstornarERestockEPublicarEventoComRefundIssuedTrue() {
        Order order = orderWithItem(200L, OrderStatus.PAID, PaymentMethod.CREDIT_CARD);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(userFacade.findByEmail(EMAIL)).thenReturn(userInfo());

        orderService.cancel(200L, EMAIL);

        verify(bankingFacade).issueRefund(eq(10L), eq(order.getTotalAmount()), eq("ORDER-200"), any());
        verify(productService).restock(100L, 2);
        verify(productService, never()).releaseReservation(any(), anyInt());

        ArgumentCaptor<OrderCancelledEvent> evCap = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publishEvent(evCap.capture());
        assertTrue(evCap.getValue().refundIssued());
    }

    @Test
    void cancel_DeveLancarException_QuandoOrderJaFoiEnviado() {
        Order order = orderWithItem(200L, OrderStatus.SHIPPED, PaymentMethod.PIX);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.cancel(200L, EMAIL));
        verify(bankingFacade, never()).cancelPaymentRequest(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void findMyOrders_DeveRetornarListaMapeada() {
        Order order = orderWithItem(200L, OrderStatus.PAID, PaymentMethod.PIX);
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.findMyOrders(EMAIL);

        assertEquals(1, orders.size());
        OrderResponse r = orders.get(0);
        assertEquals(200L, r.orderId());
        assertEquals(OrderStatus.PAID, r.status());
        assertEquals(1, r.items().size());
        assertEquals("Mouse Gamer", r.items().get(0).productName());
        assertEquals(0, new BigDecimal("300.00").compareTo(r.items().get(0).lineTotal()));
    }

    @Test
    void findMyOrder_DeveLancarOrderNotFound_QuandoNaoPertenceAoCliente() {
        Order order = orderWithItem(200L, OrderStatus.PAID, PaymentMethod.PIX);
        order.setCustomerId(999L);
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));

        assertThrows(OrderNotFound.class, () -> orderService.findMyOrder(200L, EMAIL));
    }

    private Order orderWithItem(Long orderId, OrderStatus status, PaymentMethod method) {
        Order order = Order.builder()
                .id(orderId)
                .customerId(1L)
                .status(status)
                .paymentMethod(method)
                .subtotal(new BigDecimal("300.00"))
                .discountTotal(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("300.00"))
                .build();
        order.addItem(OrderItem.builder()
                .productId(100L)
                .productName("Mouse Gamer")
                .quantity(2)
                .unitPrice(new BigDecimal("150.00"))
                .build());
        return order;
    }

    private UserInfo userInfo() {
        return new UserInfo(10L, "Ana Silva", "ana@email.com", "12345678901",
                LocalDate.of(1990, 1, 1), Set.of(CUSTOMER));
    }
}
