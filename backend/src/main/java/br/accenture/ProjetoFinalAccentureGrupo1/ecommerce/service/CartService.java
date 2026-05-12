package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.service;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Cart;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.AddToCartRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartItemResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.UpdateCartItemRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartItemNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.InsufficientStockException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.ProductNotAvailableException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartItemRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services.CustomerService;
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
                .orElse(null);
        if (cart == null) {
            return new CartResponse(null, List.of(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
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
                .orElseGet(() -> new CartResponse(null, List.of(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
    }

    private Cart getOrCreateCart(Customer customer) {
        return cartRepository.findByCustomer_Id(customer.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().customer(customer).build()));
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
