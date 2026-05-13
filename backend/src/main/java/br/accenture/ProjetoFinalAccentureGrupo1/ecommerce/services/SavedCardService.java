package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.services;

import br.accenture.ProjetoFinalAccentureGrupo1.banking.api.BankingFacade;
import br.accenture.ProjetoFinalAccentureGrupo1.banking.dto.CardValidationResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.Customer;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.domain.SavedCard;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.RegisterSavedCardRequest;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.dto.SavedCardResponse;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.SavedCardAlreadyExistsException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions.SavedCardNotFoundException;
import br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.repository.SavedCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedCardService {

    private final SavedCardRepository savedCardRepository;
    private final CustomerService customerService;
    private final BankingFacade bankingFacade;

    @Transactional
    public SavedCardResponse registerCard(String customerEmail, RegisterSavedCardRequest request) {
        Customer customer = customerService.findByEmail(customerEmail);
        CardValidationResponse validation = bankingFacade.verifyCard(
                request.cardNumber(),
                request.cvv(),
                request.expirationMonth(),
                request.expirationYear()
        );

        if (savedCardRepository.existsByCustomer_IdAndBankingCardId(customer.getId(), validation.cardId())) {
            throw new SavedCardAlreadyExistsException();
        }

        SavedCard savedCard = SavedCard.builder()
                .customer(customer)
                .bankingCardId(validation.cardId())
                .last4Digits(last4Digits(request.cardNumber()))
                .holderName(normalizeHolderName(request.holderName()))
                .build();

        return toResponse(savedCardRepository.save(savedCard));
    }

    @Transactional(readOnly = true)
    public List<SavedCardResponse> listMyCards(String customerEmail) {
        Customer customer = customerService.findByEmail(customerEmail);
        return savedCardRepository.findByCustomer_IdOrderByCreatedAtDesc(customer.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String customerEmail, Long savedCardId) {
        Customer customer = customerService.findByEmail(customerEmail);
        SavedCard savedCard = findByIdAndCustomer(savedCardId, customer.getId());
        savedCardRepository.delete(savedCard);
    }

    @Transactional(readOnly = true)
    public SavedCard findByIdAndCustomer(Long savedCardId, Long customerId) {
        return savedCardRepository.findByIdAndCustomer_Id(savedCardId, customerId)
                .orElseThrow(() -> new SavedCardNotFoundException(savedCardId));
    }

    private String last4Digits(String cardNumber) {
        return cardNumber.substring(cardNumber.length() - 4);
    }

    private String normalizeHolderName(String holderName) {
        if (holderName == null || holderName.isBlank()) {
            return null;
        }
        return holderName.trim();
    }

    private SavedCardResponse toResponse(SavedCard savedCard) {
        return new SavedCardResponse(
                savedCard.getId(),
                savedCard.getLast4Digits(),
                savedCard.getHolderName(),
                savedCard.getCreatedAt()
        );
    }
}
