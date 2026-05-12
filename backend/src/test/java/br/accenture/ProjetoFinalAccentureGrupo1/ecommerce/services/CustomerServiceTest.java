package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.auth.api.UserInfo;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.CustomerResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.enums.CustomerTier;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.CustomerNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static br.accenture.ProjetoFinalAccentureGrupo1.auth.enums.Role.CUSTOMER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .userId(10L)
                .quantityPurchases(0)
                .tier(CustomerTier.BRONZE)
                .shippingAddress("Rua A, 123")
                .phone("11999999999")
                .build();
    }

    @Test
    void createCustomer_DeveCriarCliente_QuandoUsuarioAindaNaoTemCustomer() {
        when(userFacade.findById(10L)).thenReturn(userInfo());
        when(customerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer created = customerService.createCustomer(10L, "Rua B, 456", "11888888888");

        assertEquals(10L, created.getUserId());
        assertEquals("Rua B, 456", created.getShippingAddress());
        assertEquals("11888888888", created.getPhone());
        assertEquals(CustomerTier.BRONZE, created.getTier());
        assertEquals(0, created.getQuantityPurchases());
    }

    @Test
    void createCustomer_DeveSerIdempotente_QuandoClienteJaExiste() {
        when(userFacade.findById(10L)).thenReturn(userInfo());
        when(customerRepository.findByUserId(10L)).thenReturn(Optional.of(customer));

        Customer result = customerService.createCustomer(10L, "Rua B, 456", "11888888888");

        assertSame(customer, result);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void findByUserId_DeveLancarException_QuandoClienteNaoExiste() {
        when(customerRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.findByUserId(99L));
    }

    @Test
    void updateMyCustomer_DeveAtualizarEnderecoETelefone() {
        when(userFacade.findByEmail("ana@email.com")).thenReturn(userInfo());
        when(customerRepository.findByUserId(10L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerResponse response = customerService.updateMyCustomer("ana@email.com", "Rua Nova, 10", "11777777777");

        assertEquals("Rua Nova, 10", response.shippingAddress());
        assertEquals("11777777777", response.phone());
    }

    @Test
    void incrementCompletedOrders_DevePromoverParaSilver_QuandoAtingeCincoCompras() {
        customer.setQuantityPurchases(4);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = customerService.incrementCompletedOrders(1L);

        assertEquals(5, updated.getQuantityPurchases());
        assertEquals(CustomerTier.SILVER, updated.getTier());
    }

    @Test
    void incrementCompletedOrders_DevePromoverParaGold_QuandoAtingeDezCompras() {
        customer.setQuantityPurchases(9);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);

        Customer updated = customerService.incrementCompletedOrders(1L);

        assertEquals(10, updated.getQuantityPurchases());
        assertEquals(CustomerTier.GOLD, updated.getTier());
    }

    private UserInfo userInfo() {
        return new UserInfo(
                10L,
                "Ana Silva",
                "ana@email.com",
                "12345678901",
                LocalDate.of(1990, 1, 1),
                Set.of(CUSTOMER)
        );
    }
}
