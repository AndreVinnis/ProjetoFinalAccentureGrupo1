package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Cart;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.AddToCartRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartItemResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.UpdateCartItemRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.*;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartItemRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public CartResponse getMyCart(String email) {
        Customer customer = customerService.findByEmail(email);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId())
                .orElseThrow(
                        () -> new CartNotFoundException()
                );
        return toResponse(cart);
    }

    @Transactional
    public CartResponse closeCart(String email) {
        Customer customer = customerService.findByEmail(email);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        if(cart.getItems().isEmpty()){
            throw new CartEmptyException();
        }
        if (isClosed(cart)) {
            throw new IllegalStateException("Carrinho já está fechado");
        }
        for(CartItem item: cart.getItems()){
            productService.reserveStock(item.getId(), item.getQuantity());
        }
        cart.markReserved();
        return toResponse(cart);
    }

    @Transactional
    public CartResponse openCart(String email) {
        Customer customer = customerService.findByEmail(email);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId()).orElseThrow(
                () -> new CartNotFoundException()
        );
        if (cart.getStatus() != CartStatus.RESERVED) {
            throw new IllegalStateException("Só carrinho RESERVED pode ser reaberto");
        }
        for(CartItem item: cart.getItems()){
            productService.releaseReservation(item.getId(), item.getQuantity());
        }
        cart.setStatus(CartStatus.ACTIVE);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String email, AddToCartRequest request) {
        Customer customer = customerService.findByEmail(email);
        Cart cart = getOrCreateCart(customer);
        Product product = productService.findById(request.productId());
        assertProductActive(product);
        assertStock(product, request.quantity());

        cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId())
                .ifPresentOrElse(
                        item -> {
                            int newQty = item.getQuantity() + request.quantity();
                            assertStock(product, newQty);
                            item.setQuantity(newQty);
                            item.setUnitPrice(product.getPrice());
                        },
                        () -> {
                            CartItem line = CartItem.builder()
                                    .cart(cart)
                                    .product(product)
                                    .quantity(request.quantity())
                                    .unitPrice(product.getPrice())
                                    .build();
                            cart.getItems().add(line);
                        }
                );

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(String email, Long productId, UpdateCartItemRequest request) {
        if (request.quantity() == 0) {
            return removeItem(email, productId);
        }

        Customer customer = customerService.findByEmail(email);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId())
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        CartItem item = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        Product product = productService.findById(productId);
        assertProductActive(product);
        assertStock(product, request.quantity());

        item.setQuantity(request.quantity());
        item.setUnitPrice(product.getPrice());

        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String email, Long productId) {
        Customer customer = customerService.findByEmail(email);
        Cart cart = cartRepository.findByCustomer_Id(customer.getId())
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        CartItem item = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        cart.getItems().remove(item);
        cartRepository.save(cart);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse clearMyCart(String email) {
        Customer customer = customerService.findByEmail(email);
        return cartRepository.findByCustomer_Id(customer.getId())
                .map(cart -> {
                    cart.getItems().clear();
                    cartRepository.save(cart);
                    return toResponse(cart);
                })
                .orElseThrow(
                        () -> new CartNotFoundException()
                );
    }

    private Cart getOrCreateCart(Customer customer) {
        return cartRepository.findByCustomer_Id(customer.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().customer(customer).build()));
    }

    public boolean isClosed(Cart cart) {
        return cart.getStatus() != CartStatus.ACTIVE;
    }

    private void assertProductActive(Product product) {
        if (!product.isActive()) {
            throw new ProductNotAvailableException("Produto indisponível: " + product.getId());
        }
    }

    private void assertStock(Product product, int desiredQuantity) {
        if (desiredQuantity < 1) {
            return;
        }
        int available = product.getAvailableStock();
        if (available < desiredQuantity) {
            throw new InsufficientStockException(
                    "Estoque insuficiente para o produto " + product.getId() + ". Disponível: " + available
            );
        }
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> lines = cart.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getUnitPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    return new CartItemResponse(
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getUnitPrice().setScale(2, RoundingMode.HALF_UP),
                            lineTotal
                    );
                })
                .toList();

        BigDecimal subtotal = lines.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new CartResponse(cart.getId(), lines, subtotal);
    }
}
