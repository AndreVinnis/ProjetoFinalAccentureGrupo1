package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class BankingExceptionHandler {

    @ExceptionHandler({
            AccountNotFoundException.class,
            CardNotFoundException.class,
            CreditCardNotFoundException.class,
            InvoiceNotFoundException.class,
            PaymentRequestNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            AccountAlreadyExistsException.class,
            CreditCardAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
            AccountBlockedException.class,
            AccountNotActiveException.class,
            AccountRestrictedException.class,
            CreditCardBlockedException.class,
            InsufficientBalanceException.class,
            InsufficientCreditLimitException.class,
            InvalidAmountException.class,
            InvoiceNotCloseableException.class,
            InvoiceNotPayableException.class,
            PaymentRequestNotPayableException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessRule(RuntimeException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler({
            InvalidCardException.class,
            WrongCvvException.class,
            WrongPasswordException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), message));
    }

    public record ErrorResponse(Instant timestamp, int status, String message) {}
}
