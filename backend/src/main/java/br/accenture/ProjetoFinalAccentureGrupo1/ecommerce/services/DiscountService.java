package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.discount.DiscountRule;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.CartItem;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.DiscountApplication;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
// André Vinícius Barros Macambira
public class DiscountService {

    private final List<DiscountRule> rules;

    public List<DiscountApplication> applyAll(Customer customer, List<CartItem> items, PaymentMethod paymentMethod) {
        return rules.stream()
                .map(rule -> rule.apply(customer, items, paymentMethod))
                .flatMap(Optional::stream)
                .toList();
    }
}
