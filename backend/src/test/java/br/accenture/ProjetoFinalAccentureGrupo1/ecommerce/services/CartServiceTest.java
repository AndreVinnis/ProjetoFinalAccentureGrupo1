package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Cart;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Category;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Product;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.AddToCartRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CartResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.UpdateCartItemRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CartStatus;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartEmptyException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartItemNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CartNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.InsufficientStockException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.ProductNotAvailableException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartItemRepository;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService - Unit Tests")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProductService productService;

    private Clock clock;

    private CartService cartService;

    private static final String EMAIL = "user@test.com";

    private Customer customer;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-01-10T10:00:00Z"), ZoneOffset.UTC);
        cartService = new CartService(cartRepository, cartItemRepository, customerService, productService, clock);

        customer = Customer.builder()
                .id(1L)
                .userId(10L)
                .shippingAddress("Endereço de teste")
                .phone("11999999999")
                .build();

        category = Category.builder().id(1L).name("Eletrônicos").build();

        product = Product.builder()
                .id(100L)
                .name("Smartphone")
                .description("Smartphone top")
                .price(new BigDecimal("1000.00"))
                .totalStock(10)
                .reservedStock(0)
                .active(true)
                .category(category)
                .build();
    }

    private Cart activeCart() {
        return Cart.builder()
                .id(50L)
                .customer(customer)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .build();
    }

    private CartItem cartItem(Cart cart, Product product, int qty, BigDecimal price) {
        return CartItem.builder()
                .id(500L)
                .cart(cart)
                .product(product)
                .quantity(qty)
                .unitPrice(price)
                .build();
    }

    @Test
    @DisplayName("getMyCart retorna o carrinho do cliente quando existe")
    void getMyCart_returnsCart() {
        Cart cart = activeCart();
        cart.getItems().add(cartItem(cart, product, 2, new BigDecimal("1000.00")));

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getMyCart(EMAIL);

        assertThat(response.cartId()).isEqualTo(50L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.subtotal()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("getMyCart lança CartNotFoundException quando não existe carrinho")
    void getMyCart_throwsWhenCartMissing() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getMyCart(EMAIL))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("closeCart reserva estoque para cada item e marca o carrinho como RESERVED")
    void closeCart_success() {
        Cart cart = activeCart();
        cart.getItems().add(cartItem(cart, product, 3, new BigDecimal("1000.00")));

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        CartResponse response = cartService.closeCart(EMAIL);

        verify(productService).reserveStock(100L, 3);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.RESERVED);
        assertThat(cart.getReservedAt()).isNotNull();
        assertThat(response.subtotal()).isEqualByComparingTo("3000.00");
    }

    @Test
    @DisplayName("closeCart lança CartNotFoundException quando o carrinho não existe")
    void closeCart_throwsWhenCartMissing() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.closeCart(EMAIL))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("closeCart lança CartEmptyException quando o carrinho está vazio")
    void closeCart_throwsWhenCartEmpty() {
        Cart cart = activeCart();
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.closeCart(EMAIL))
                .isInstanceOf(CartEmptyException.class);
    }

    @Test
    @DisplayName("closeCart lança IllegalStateException quando o carrinho já está RESERVED")
    void closeCart_throwsWhenAlreadyClosed() {
        Cart cart = activeCart();
        cart.setStatus(CartStatus.RESERVED);
        cart.getItems().add(cartItem(cart, product, 1, new BigDecimal("1000.00")));

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.closeCart(EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fechado");
    }

    @Test
    @DisplayName("openCart libera reservas e volta o carrinho para ACTIVE")
    void openCart_success() {
        Cart cart = activeCart();
        cart.setStatus(CartStatus.RESERVED);
        cart.getItems().add(cartItem(cart, product, 2, new BigDecimal("1000.00")));

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        cartService.openCart(EMAIL);

        verify(productService).releaseReservation(100L, 2);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("openCart lança IllegalStateException quando o carrinho não está RESERVED")
    void openCart_throwsWhenStatusInvalid() {
        Cart cart = activeCart();
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.openCart(EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESERVED");
    }

    @Test
    @DisplayName("openCart lança CartNotFoundException quando o carrinho não existe")
    void openCart_throwsWhenMissing() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.openCart(EMAIL))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("addItem adiciona novo item quando ainda não existe no carrinho")
    void addItem_createsNewLine() {
        Cart cart = activeCart();

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(productService.findById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());
        when(cartRepository.save(cart)).thenReturn(cart);

        CartResponse response = cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 2));

        assertThat(cart.getItems()).hasSize(1);
        CartItem inserted = cart.getItems().get(0);
        assertThat(inserted.getQuantity()).isEqualTo(2);
        assertThat(inserted.getUnitPrice()).isEqualByComparingTo("1000.00");
        assertThat(response.subtotal()).isEqualByComparingTo("2000.00");
    }

    @Test
    @DisplayName("addItem soma quantidade quando item já existe")
    void addItem_updatesExistingLine() {
        Cart cart = activeCart();
        CartItem existing = cartItem(cart, product, 1, new BigDecimal("900.00"));
        cart.getItems().add(existing);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(productService.findById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(cart)).thenReturn(cart);

        cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 3));

        assertThat(existing.getQuantity()).isEqualTo(4);
        assertThat(existing.getUnitPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("addItem cria carrinho novo quando cliente ainda não tem um")
    void addItem_createsCartWhenAbsent() {
        Cart newCart = activeCart();

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(productService.findById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCart_IdAndProduct_Id(newCart.getId(), product.getId()))
                .thenReturn(Optional.empty());

        cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 1));

        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    @DisplayName("addItem lança IllegalStateException quando o carrinho está fechado")
    void addItem_throwsWhenCartClosed() {
        Cart cart = activeCart();
        cart.setStatus(CartStatus.RESERVED);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("addItem lança ProductNotAvailableException para produto inativo")
    void addItem_throwsWhenProductInactive() {
        Cart cart = activeCart();
        product.setActive(false);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(productService.findById(product.getId())).thenReturn(product);

        assertThatThrownBy(() -> cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 1)))
                .isInstanceOf(ProductNotAvailableException.class);
    }

    @Test
    @DisplayName("addItem lança InsufficientStockException quando estoque é insuficiente")
    void addItem_throwsWhenStockInsufficient() {
        Cart cart = activeCart();
        product.setTotalStock(2);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(productService.findById(product.getId())).thenReturn(product);

        assertThatThrownBy(() -> cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 5)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("addItem lança InsufficientStockException quando soma com item atual excede o estoque")
    void addItem_throwsWhenAccumulatedStockExceeds() {
        Cart cart = activeCart();
        CartItem existing = cartItem(cart, product, 6, new BigDecimal("1000.00"));
        cart.getItems().add(existing);
        product.setTotalStock(8);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(productService.findById(product.getId())).thenReturn(product);
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cartService.addItem(EMAIL, new AddToCartRequest(product.getId(), 5)))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("updateItemQuantity com quantidade 0 delega para removeItem")
    void updateItemQuantity_zeroDelegatesToRemove() {
        Cart cart = activeCart();
        CartItem existing = cartItem(cart, product, 1, new BigDecimal("1000.00"));
        cart.getItems().add(existing);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existing));

        cartService.updateItemQuantity(EMAIL, product.getId(), new UpdateCartItemRequest(0));

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("updateItemQuantity altera a quantidade e o preço unitário do item")
    void updateItemQuantity_success() {
        Cart cart = activeCart();
        CartItem existing = cartItem(cart, product, 1, new BigDecimal("900.00"));
        cart.getItems().add(existing);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existing));
        when(productService.findById(product.getId())).thenReturn(product);

        cartService.updateItemQuantity(EMAIL, product.getId(), new UpdateCartItemRequest(4));

        assertThat(existing.getQuantity()).isEqualTo(4);
        assertThat(existing.getUnitPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("updateItemQuantity lança CartItemNotFoundException quando carrinho não existe")
    void updateItemQuantity_throwsWhenCartMissing() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, product.getId(), new UpdateCartItemRequest(2)))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    @DisplayName("updateItemQuantity lança IllegalStateException quando o carrinho está fechado")
    void updateItemQuantity_throwsWhenCartClosed() {
        Cart cart = activeCart();
        cart.setStatus(CartStatus.RESERVED);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, product.getId(), new UpdateCartItemRequest(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateItemQuantity lança CartItemNotFoundException quando item não está no carrinho")
    void updateItemQuantity_throwsWhenItemMissing() {
        Cart cart = activeCart();
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, product.getId(), new UpdateCartItemRequest(1)))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    @DisplayName("updateItemQuantity lança ProductNotAvailableException quando produto está inativo")
    void updateItemQuantity_throwsWhenProductInactive() {
        Cart cart = activeCart();
        CartItem existing = cartItem(cart, product, 1, new BigDecimal("1000.00"));
        cart.getItems().add(existing);
        product.setActive(false);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existing));
        when(productService.findById(product.getId())).thenReturn(product);

        assertThatThrownBy(() -> cartService.updateItemQuantity(EMAIL, product.getId(), new UpdateCartItemRequest(2)))
                .isInstanceOf(ProductNotAvailableException.class);
    }

    @Test
    @DisplayName("removeItem remove o item do carrinho")
    void removeItem_success() {
        Cart cart = activeCart();
        CartItem existing = cartItem(cart, product, 2, new BigDecimal("1000.00"));
        cart.getItems().add(existing);

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.of(existing));

        cartService.removeItem(EMAIL, product.getId());

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("removeItem lança CartItemNotFoundException quando carrinho não existe")
    void removeItem_throwsWhenCartMissing() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(EMAIL, product.getId()))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    @DisplayName("removeItem lança CartItemNotFoundException quando item não está no carrinho")
    void removeItem_throwsWhenItemMissing() {
        Cart cart = activeCart();
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(EMAIL, product.getId()))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    @DisplayName("clearMyCart limpa os itens do carrinho")
    void clearMyCart_success() {
        Cart cart = activeCart();
        cart.getItems().add(cartItem(cart, product, 2, new BigDecimal("1000.00")));

        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.of(cart));

        CartResponse response = cartService.clearMyCart(EMAIL);

        assertThat(cart.getItems()).isEmpty();
        assertThat(response.subtotal()).isEqualByComparingTo("0.00");
        verify(cartRepository).save(cart);
    }

    @Test
    @DisplayName("clearMyCart lança CartNotFoundException quando o carrinho não existe")
    void clearMyCart_throwsWhenCartMissing() {
        when(customerService.findByEmail(EMAIL)).thenReturn(customer);
        when(cartRepository.findByCustomer_Id(customer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.clearMyCart(EMAIL))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("releaseExpiredReservations libera reservas e devolve carrinhos para ACTIVE")
    void releaseExpiredReservations_success() {
        Cart cart = activeCart();
        cart.setStatus(CartStatus.RESERVED);
        cart.setReservedAt(Instant.now(clock).minusSeconds(60 * 60 * 24 * 5));
        cart.getItems().add(cartItem(cart, product, 2, new BigDecimal("1000.00")));

        when(cartRepository.findByStatusAndReservedAtBefore(eq(CartStatus.RESERVED), any(Instant.class)))
                .thenReturn(List.of(cart));

        int released = cartService.releaseExpiredReservations();

        assertThat(released).isEqualTo(1);
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getReservedAt()).isNull();
        verify(productService).releaseReservation(100L, 2);
        verify(cartRepository).saveAll(List.of(cart));
    }

    @Test
    @DisplayName("releaseExpiredReservations retorna 0 quando não há carrinhos expirados")
    void releaseExpiredReservations_empty() {
        when(cartRepository.findByStatusAndReservedAtBefore(eq(CartStatus.RESERVED), any(Instant.class)))
                .thenReturn(List.of());

        int released = cartService.releaseExpiredReservations();

        assertThat(released).isZero();
        verify(productService, never()).releaseReservation(anyLong(), anyInt());
    }

    @Test
    @DisplayName("isClosed retorna true para status diferente de ACTIVE")
    void isClosed_returnsTrueWhenReserved() {
        Cart cart = activeCart();
        cart.setStatus(CartStatus.RESERVED);
        assertThat(cartService.isClosed(cart)).isTrue();
    }

    @Test
    @DisplayName("isClosed retorna false quando o carrinho está ACTIVE")
    void isClosed_returnsFalseWhenActive() {
        assertThat(cartService.isClosed(activeCart())).isFalse();
    }
}
