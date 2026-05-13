package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.discount;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.DiscountApplication;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
// André Vinícius Barros Macambira
public class GoldTierDiscountRule implements DiscountRule {

    private final static String ruleName = "GOLD_TIER_10_PERCENT";

    @Override
    public Optional<DiscountApplication> apply(Customer customer, List<CartItem> items, PaymentMethod paymentMethod) {
        if (customer.getTier() != CustomerTier.GOLD) {
            return Optional.empty();
        }

        BigDecimal subtotal = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 10% de desconto
        BigDecimal discountAmount = subtotal.multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_EVEN);

        return Optional.of(new DiscountApplication(
                ruleName,
                "Desconto de 10% exclusivo para cliente Gold",
                discountAmount
        ));
    }
}