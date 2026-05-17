package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.api.BankingFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.*;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.DiscountApplication;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.InstallmentOptionResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.OrderItemResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.OrderResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.OrderStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderCancelledEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderDeliveredEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderPaidEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.events.OrderShippedEvent;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartWasNotClosedException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CustomerNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.OrderNotFound;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CustomerRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.OrderDiscountRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
// Autor: André Vinícius Barros Macambira
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String orderPrefix = "ORDER-";
    private static final BigDecimal GOLD_CARD_CASHBACK_RATE = new BigDecimal("0.05");
    private static final int MAX_CARD_INSTALLMENTS = 12;

    private final CartService cartService;
    private final CartRepository cartRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final BankingFacade bankingFacade;
    private final UserFacade userFacade;
    private final SavedCardService savedCardService;
    private final DiscountService discountService;
    private final OrderDiscountRepository orderDiscountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;


    @Transactional
    public String checkoutPix(String customerEmail) {
        String defaultDescription = "Compra no ecommerce feita pelo pix";

        Customer customer = customerService.findByEmail(customerEmail);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        if(!cartService.isClosed(cart)){
            throw new CartWasNotClosedException(cart.getId());
        }

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.PIX);

        BigDecimal subTotal = BigDecimal.ZERO;
        for(CartItem item: cart.getItems()){
            subTotal = subTotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
            order.addItem(orderItem);
        }

        BigDecimal discountTotal = BigDecimal.ZERO;
        List<DiscountApplication> applications = discountService.applyAll(customer, cart.getItems(), PaymentMethod.PIX);
        for(DiscountApplication discountApplication: applications){
            discountTotal = discountTotal.add(discountApplication.discountAmount());
        }

        BigDecimal totalAmount = subTotal.subtract(discountTotal);
        order.setSubtotal(subTotal);
        order.setDiscountTotal(discountTotal);
        order.setTotalAmount(totalAmount);
        order = orderRepository.save(order);

        for(DiscountApplication discountApplication: applications){
            OrderDiscount orderDiscount = OrderDiscount.builder()
                    .order(order)
                    .ruleName(discountApplication.ruleName())
                    .description(discountApplication.description())
                    .amount(discountApplication.discountAmount())
                    .build();
            orderDiscountRepository.save(orderDiscount);
        }
        return bankingFacade.createPaymentRequest(totalAmount, defaultDescription, orderPrefix + order.getId());
    }

    @Transactional
    public OrderResponse checkoutCard(String customerEmail, Long savedCardId, String cvv) {
        return checkoutCard(customerEmail, savedCardId, cvv, 1);
    }

    @Transactional
    public OrderResponse checkoutCard(String customerEmail, Long savedCardId, String cvv, int installments) {
        String defaultDescription = "Compra no ecommerce feita pelo cartao de credito";
        validateInstallments(installments);

        Customer customer = customerService.findByEmail(customerEmail);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        if(!cartService.isClosed(cart)){
            throw new CartWasNotClosedException(cart.getId());
        }

        Order order = new Order();
        order.setCustomerId(customer.getId());
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        BigDecimal subTotal = BigDecimal.ZERO;

        for(CartItem item: cart.getItems()){
            subTotal = subTotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(item.getProduct().getId())
                    .productName(item.getProduct().getName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build();
            order.addItem(orderItem);
        }

        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal totalAmount = subTotal.subtract(discountTotal);
        order.setSubtotal(subTotal);
        order.setDiscountTotal(discountTotal);
        order.setTotalAmount(totalAmount);
        order.setPaidAt(Instant.now());
        order = orderRepository.save(order);

        SavedCard savedCard = savedCardService.findByIdAndCustomer(savedCardId, customer.getId());
        bankingFacade.chargeCard(
                savedCard.getBankingCardId(),
                totalAmount,
                cvv,
                defaultDescription,
                orderPrefix + order.getId(),
                installments
        );
        applyGoldCardCashback(customer, totalAmount, order.getId());

        for(OrderItem item: order.getItems()){
            productService.consumeReserved(item.getProductId(), item.getQuantity());
        }

        cart.clearAndReactivate();

        UserInfo user = userFacade.findByEmail(customerEmail);
        eventPublisher.publishEvent(new OrderPaidEvent(
                order.getId(),
                customer.getUserId(),
                user.name(),
                user.email(),
                order.getTotalAmount(),
                PaymentMethod.CREDIT_CARD.toString(),
                order.getPaidAt()
        ));
        customerService.incrementCompletedOrders(customer.getId());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<InstallmentOptionResponse> listCardInstallments(String customerEmail) {
        Customer customer = customerService.findByEmail(customerEmail);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        BigDecimal totalAmount = calculateCartSubtotal(cart);

        return java.util.stream.IntStream.rangeClosed(1, MAX_CARD_INSTALLMENTS)
                .mapToObj(installments -> toInstallmentOption(totalAmount, installments))
                .toList();
    }

    @Transactional
    public void confirmPaidByPix(String reference, Long payerUserId) {
        Long orderId = parseOrderId(reference);
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFound(orderId)
        );
        if(order.getStatus() != OrderStatus.PENDING){
            throw new IllegalStateException(order.getStatus().toString());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());

        for(OrderItem item: order.getItems()){
            productService.consumeReserved(item.getProductId(), item.getQuantity());
        }

        Cart cart = cartRepository.findByCustomer_Id(order.getCustomerId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        cart.clearAndReactivate();
        orderRepository.save(order);
        UserInfo payer = userFacade.findById(payerUserId);
        eventPublisher.publishEvent(new OrderPaidEvent(
                order.getId(),
                payerUserId,
                payer.name(),
                payer.email(),
                order.getTotalAmount(),
                PaymentMethod.PIX.toString(),
                order.getPaidAt()
        ));
        customerService.incrementCompletedOrders(order.getCustomerId());
    }

    @Transactional
    public void cancelByPixExpiration(String reference) {
        Long orderId = parseOrderId(reference);
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFound(orderId)
        );
        if(order.getStatus() != OrderStatus.PENDING){
            throw new IllegalStateException(order.getStatus().toString());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());

        for(OrderItem item: order.getItems()){
            productService.releaseReservation(item.getProductId(), item.getQuantity());
        }

        Cart cart = cartRepository.findByCustomer_Id(order.getCustomerId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        cart.clearAndReactivate();

        orderRepository.save(order);
        Customer customer = customerService.findById(order.getCustomerId());
        UserInfo user = userFacade.findById(customer.getUserId());
        eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                customer.getUserId(),
                user.name(),
                user.email(),
                order.getTotalAmount(),
                false,
                order.getCancelledAt()
        ));
    }


    @Transactional
    public void cancel(Long orderId, String customerEmail) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFound(orderId)
        );
        if(order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID){
            throw new IllegalStateException(order.getStatus().toString());
        }

        boolean refundIssued = false;
        Customer customer = customerService.findByEmail(customerEmail);

        if(order.getStatus() == OrderStatus.PENDING){
            bankingFacade.cancelPaymentRequest(orderPrefix + orderId);
            order.setCancelledAt(Instant.now());
            for(OrderItem item: order.getItems()){
                productService.releaseReservation(item.getProductId(), item.getQuantity());
            }
            Cart cart = cartRepository.findByCustomer_Id(order.getCustomerId()).orElseThrow(
                    () -> new CartNotFoundException()
            );
            cart.clearAndReactivate();
        }
        else{
            String defaultDescription = "Estorno referente a compra no ecommerce";
            order.setCancelledAt(Instant.now());
            bankingFacade.issueRefund((customerService.findByEmail(customerEmail).getUserId()),
                    order.getTotalAmount(),
                    (orderPrefix + orderId),
                    defaultDescription);
            refundIssued = true;
            for(OrderItem item: order.getItems()){
                productService.restock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCancelledEvent(
                orderId,
                customer.getUserId(),
                userFacade.findByEmail(customerEmail).name(),
                customerEmail,
                order.getTotalAmount(),
                refundIssued,
                order.getCancelledAt()
        ));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findMyOrders(String customerEmail) {
        Customer customer = customerService.findByEmail(customerEmail);
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findMyOrder(Long orderId, String customerEmail) {
        Customer customer = customerService.findByEmail(customerEmail);
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderNotFound(orderId)
        );
        if(!order.getCustomerId().equals(customer.getId())){
            throw new OrderNotFound(orderId);
        }
        return toResponse(order);
    }

    @Transactional
    public int transitionToShipped() {
        // Pedidos pagos há mais de 1 dia
        Instant threshold = Instant.now(clock).minus(1, ChronoUnit.DAYS);
        List<Order> ordersToShip = orderRepository.findByStatusAndPaidAtBefore(OrderStatus.PAID, threshold);

        for (Order order : ordersToShip) {
            order.setStatus(OrderStatus.SHIPPED);
            order.setShippedAt(Instant.now(clock));
            log.debug("Pedido ID {} atualizado para SHIPPED", order.getId());
            Customer customer = customerRepository.findById(order.getCustomerId()).orElseThrow(
                    () -> new CustomerNotFoundException(order.getCustomerId())
            );
            UserInfo userInfo = userFacade.findById(customer.getUserId());
            eventPublisher.publishEvent(new OrderShippedEvent(
                    order.getId(),
                    order.getCustomerId(),
                    userInfo.name(),
                    userInfo.email(),
                    Instant.now()
            ));
        }
        orderRepository.saveAll(ordersToShip);
        return ordersToShip.size();
    }

    @Transactional
    public int transitionToDelivered() {
        // Pedidos enviados há mais de 5 dias
        Instant threshold = Instant.now(clock).minus(5, ChronoUnit.DAYS);
        List<Order> ordersToDeliver = orderRepository.findByStatusAndShippedAtBefore(OrderStatus.SHIPPED, threshold);

        for (Order order : ordersToDeliver) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(Instant.now(clock));
            log.debug("Pedido ID {} atualizado para DELIVERED", order.getId());
            Customer customer = customerRepository.findById(order.getCustomerId()).orElseThrow(
                    () -> new CustomerNotFoundException(order.getCustomerId())
            );
            UserInfo userInfo = userFacade.findById(customer.getUserId());
            eventPublisher.publishEvent(new OrderDeliveredEvent(
                    order.getId(),
                    order.getCustomerId(),
                    userInfo.name(),
                    userInfo.email(),
                    Instant.now()
            ));
        }
        orderRepository.saveAll(ordersToDeliver);
        return ordersToDeliver.size();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getDiscountTotal(),
                order.getTotalAmount(),
                items,
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCancelledAt()
        );
    }

    private Long parseOrderId(String reference) {
        if (reference == null || !reference.startsWith(orderPrefix)) {
            throw new IllegalArgumentException("Referência inválida: " + reference);
        }
        try {
            return Long.valueOf(reference.substring(orderPrefix.length()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Referência inválida: " + reference);
        }
    }

    private void applyGoldCardCashback(Customer customer, BigDecimal totalAmount, Long orderId) {
        if (customer.getTier() != CustomerTier.GOLD) {
            return;
        }

        BigDecimal cashbackAmount = totalAmount.multiply(GOLD_CARD_CASHBACK_RATE)
                .setScale(2, RoundingMode.HALF_EVEN);

        bankingFacade.applyCashback(
                customer.getUserId(),
                cashbackAmount,
                orderPrefix + orderId,
                "Cashback de 5% referente a compra no cartao de credito"
        );
    }

    private BigDecimal calculateCartSubtotal(Cart cart) {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            subTotal = subTotal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return subTotal;
    }

    private InstallmentOptionResponse toInstallmentOption(BigDecimal totalAmount, int installments) {
        BigDecimal installmentAmount = totalAmount
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_EVEN);
        String label = installments == 1
                ? "A vista - R$ " + totalAmount
                : installments + "x de R$ " + installmentAmount + " sem juros";

        return new InstallmentOptionResponse(installments, installmentAmount, totalAmount, label);
    }

    private void validateInstallments(int installments) {
        if (installments < 1 || installments > MAX_CARD_INSTALLMENTS) {
            throw new IllegalArgumentException("Parcelamento deve estar entre 1 e " + MAX_CARD_INSTALLMENTS);
        }
    }
}
