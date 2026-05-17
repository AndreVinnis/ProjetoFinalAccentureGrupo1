package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.discount;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.DiscountApplication;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilverTierDiscountRuleTest {

    private final SilverTierDiscountRule rule = new SilverTierDiscountRule();

    private Customer customerWithTier(CustomerTier tier) {
        return Customer.builder()
                .id(1L)
                .userId(10L)
                .tier(tier)
                .shippingAddress("Rua Exemplo, 1")
                .phone("11999999999")
                .build();
    }

    private CartItem item(BigDecimal unitPrice, int quantity) {
        return CartItem.builder()
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();
    }

    @Test
    void apply_DeveRetornarVazio_QuandoCustomerEhBronze() {
        Customer customer = customerWithTier(CustomerTier.BRONZE);
        List<CartItem> items = List.of(item(new BigDecimal("100.00"), 1));

        Optional<DiscountApplication> result = rule.apply(customer, items, PaymentMethod.PIX);

        assertTrue(result.isEmpty());
    }

    @Test
    void apply_DeveRetornarVazio_QuandoCustomerEhGold() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(item(new BigDecimal("100.00"), 1));

        Optional<DiscountApplication> result = rule.apply(customer, items, PaymentMethod.CREDIT_CARD);

        assertTrue(result.isEmpty());
    }

    @Test
    void apply_DeveCalcular5PorCento_QuandoUmItemUnitario() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        List<CartItem> items = List.of(item(new BigDecimal("100.00"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("5.00"), discount.discountAmount());
        assertEquals("SILVER_TIER_5_PERCENT", discount.ruleName());
        assertEquals("Desconto de 5% exclusivo para cliente Silver", discount.description());
    }

    @Test
    void apply_DeveCalcular5PorCento_QuandoMultiplosItens() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        List<CartItem> items = List.of(
                item(new BigDecimal("50.00"), 2),   // 100.00
                item(new BigDecimal("30.00"), 4),   // 120.00
                item(new BigDecimal("10.00"), 1)    //  10.00
        );                                          // subtotal = 230.00 → 5% = 11.50

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("11.50"), discount.discountAmount());
    }

    @Test
    void apply_DeveConsiderarQuantidades_NoCalculoDoSubtotal() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        List<CartItem> items = List.of(item(new BigDecimal("19.90"), 10)); // 199.00 → 5% = 9.95

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.CREDIT_CARD).orElseThrow();

        assertEquals(new BigDecimal("9.95"), discount.discountAmount());
    }

    @Test
    void apply_DeveArredondarHalfEven_ParaCima() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        // subtotal = 33.33 → 5% = 1.6665 → HALF_EVEN com 2 casas → 1.67 (dígito anterior ímpar arredonda pra cima)
        List<CartItem> items = List.of(item(new BigDecimal("33.33"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("1.67"), discount.discountAmount());
    }

    @Test
    void apply_DeveArredondarHalfEven_ParaPar() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        // subtotal = 25.00 → 5% = 1.2500 → HALF_EVEN com 2 casas → 1.25 (já é exato, scale=2)
        List<CartItem> items = List.of(item(new BigDecimal("25.00"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("1.25"), discount.discountAmount());
    }

    @Test
    void apply_DeveZerarDesconto_QuandoCarrinhoVazio() {
        Customer customer = customerWithTier(CustomerTier.SILVER);

        DiscountApplication discount = rule.apply(customer, Collections.emptyList(), PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("0.00"), discount.discountAmount());
        assertEquals("SILVER_TIER_5_PERCENT", discount.ruleName());
    }

    @Test
    void apply_DeveSerIndependenteDoMetodoDePagamento() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        List<CartItem> items = List.of(item(new BigDecimal("80.00"), 1));

        DiscountApplication pix = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();
        DiscountApplication card = rule.apply(customer, items, PaymentMethod.CREDIT_CARD).orElseThrow();

        assertEquals(pix.discountAmount(), card.discountAmount());
        assertEquals(pix.ruleName(), card.ruleName());
    }

    @Test
    void apply_DeveRetornarDiscountApplicationNaoNulo_QuandoTierValido() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        List<CartItem> items = List.of(item(new BigDecimal("10.00"), 1));

        Optional<DiscountApplication> result = rule.apply(customer, items, PaymentMethod.PIX);

        assertTrue(result.isPresent());
        assertNotNull(result.get().discountAmount());
        assertNotNull(result.get().ruleName());
        assertNotNull(result.get().description());
    }
}
