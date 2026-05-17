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

class GoldTierDiscountRuleTest {

    private final GoldTierDiscountRule rule = new GoldTierDiscountRule();

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
    void apply_DeveRetornarVazio_QuandoCustomerEhSilver() {
        Customer customer = customerWithTier(CustomerTier.SILVER);
        List<CartItem> items = List.of(item(new BigDecimal("100.00"), 1));

        Optional<DiscountApplication> result = rule.apply(customer, items, PaymentMethod.CREDIT_CARD);

        assertTrue(result.isEmpty());
    }

    @Test
    void apply_DeveCalcular10PorCento_QuandoUmItemUnitario() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(item(new BigDecimal("100.00"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("10.00"), discount.discountAmount());
        assertEquals("GOLD_TIER_10_PERCENT", discount.ruleName());
        assertEquals("Desconto de 10% exclusivo para cliente Gold", discount.description());
    }

    @Test
    void apply_DeveCalcular10PorCento_QuandoMultiplosItens() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(
                item(new BigDecimal("50.00"), 2),   // 100.00
                item(new BigDecimal("30.00"), 4),   // 120.00
                item(new BigDecimal("10.00"), 1)    //  10.00
        );                                          // subtotal = 230.00 → 10% = 23.00

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("23.00"), discount.discountAmount());
    }

    @Test
    void apply_DeveConsiderarQuantidades_NoCalculoDoSubtotal() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(item(new BigDecimal("19.90"), 10)); // 199.00 → 10% = 19.90

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.CREDIT_CARD).orElseThrow();

        assertEquals(new BigDecimal("19.90"), discount.discountAmount());
    }

    @Test
    void apply_DeveArredondarHalfEven_ParaCima() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        // subtotal = 33.33 → 10% = 3.333 → HALF_EVEN com 2 casas → 3.33
        List<CartItem> items = List.of(item(new BigDecimal("33.33"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("3.33"), discount.discountAmount());
    }

    @Test
    void apply_DeveArredondarHalfEven_QuandoDigitoExatamenteMeio() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        // subtotal = 12.55 → 10% = 1.255 → HALF_EVEN: dígito anterior 5 (ímpar) → arredonda pra par → 1.26
        List<CartItem> items = List.of(item(new BigDecimal("12.55"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("1.26"), discount.discountAmount());
    }

    @Test
    void apply_DeveZerarDesconto_QuandoCarrinhoVazio() {
        Customer customer = customerWithTier(CustomerTier.GOLD);

        DiscountApplication discount = rule.apply(customer, Collections.emptyList(), PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("0.00"), discount.discountAmount());
        assertEquals("GOLD_TIER_10_PERCENT", discount.ruleName());
    }

    @Test
    void apply_DeveSerIndependenteDoMetodoDePagamento() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(item(new BigDecimal("80.00"), 1));

        DiscountApplication pix = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();
        DiscountApplication card = rule.apply(customer, items, PaymentMethod.CREDIT_CARD).orElseThrow();

        assertEquals(pix.discountAmount(), card.discountAmount());
        assertEquals(pix.ruleName(), card.ruleName());
    }

    @Test
    void apply_DeveDarDescontoMaiorQueSilver_ParaMesmoSubtotal() {
        // Sanity check: Gold (10%) > Silver (5%) sobre o mesmo subtotal.
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(item(new BigDecimal("200.00"), 1));

        DiscountApplication discount = rule.apply(customer, items, PaymentMethod.PIX).orElseThrow();

        assertEquals(new BigDecimal("20.00"), discount.discountAmount());
        // 20.00 > 10.00 (que seria o valor da regra Silver)
        assertTrue(discount.discountAmount().compareTo(new BigDecimal("10.00")) > 0);
    }

    @Test
    void apply_DeveRetornarDiscountApplicationNaoNulo_QuandoTierValido() {
        Customer customer = customerWithTier(CustomerTier.GOLD);
        List<CartItem> items = List.of(item(new BigDecimal("10.00"), 1));

        Optional<DiscountApplication> result = rule.apply(customer, items, PaymentMethod.PIX);

        assertTrue(result.isPresent());
        assertNotNull(result.get().discountAmount());
        assertNotNull(result.get().ruleName());
        assertNotNull(result.get().description());
    }
}
