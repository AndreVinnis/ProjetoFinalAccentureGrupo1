package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.discount;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.DiscountApplication;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import java.util.List;
import java.util.Optional;

// André Vinícius Barros Macambira
public interface DiscountRule {
    Optional<DiscountApplication> apply(Customer customer, List<CartItem> items, PaymentMethod paymentMethod);
}
