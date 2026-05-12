package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CustomerResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CustomerNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
//Autor: Cainã Moura Araújo
public class CustomerService {

    private static final int SILVER_MIN_PURCHASES = 5;
    private static final int GOLD_MIN_PURCHASES = 10;

    private final CustomerRepository customerRepository;
    private final UserFacade userFacade;

    @Transactional
    public Customer createCustomer(Long userId, String address, String phone) {
        userFacade.findById(userId);
        return customerRepository.findByUserId(userId)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .userId(userId)
                        .shippingAddress(address)
                        .phone(phone)
                        .tier(CustomerTier.BRONZE)
                        .quantityPurchases(0)
                        .build()));
    }

    @Transactional(readOnly = true)
    public Customer findByUserId(Long userId) {
        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> CustomerNotFoundException.byUserId(userId));
    }

    @Transactional(readOnly = true)
    public Customer findByEmail(String email) {
        UserInfo user = userFacade.findByEmail(email);
        return findByUserId(user.id());
    }

    @Transactional(readOnly = true)
    public CustomerResponse findMyCustomer(String email) {
        return toResponse(findByEmail(email));
    }

    @Transactional
    public CustomerResponse updateMyCustomer(String email, String shippingAddress, String phone) {
        Customer customer = findByEmail(email);
        customer.setShippingAddress(shippingAddress);
        customer.setPhone(phone);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public Customer incrementCompletedOrders(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customer.setQuantityPurchases(customer.getQuantityPurchases() + 1);
        recalculateTier(customer);
        return customerRepository.save(customer);
    }

    public void recalculateTier(Customer customer) {
        int purchases = customer.getQuantityPurchases();
        if (purchases >= GOLD_MIN_PURCHASES) {
            customer.setTier(CustomerTier.GOLD);
        } else if (purchases >= SILVER_MIN_PURCHASES) {
            customer.setTier(CustomerTier.SILVER);
        } else {
            customer.setTier(CustomerTier.BRONZE);
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getUserId(),
                customer.getQuantityPurchases(),
                customer.getTier(),
                customer.getShippingAddress(),
                customer.getPhone(),
                customer.getCreatedAt()
        );
    }
}
