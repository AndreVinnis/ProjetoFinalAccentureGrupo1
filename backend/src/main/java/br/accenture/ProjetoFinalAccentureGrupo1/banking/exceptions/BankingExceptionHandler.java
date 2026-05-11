package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class BankingExceptionHandler {

    @ExceptionHandler(CreditCardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCreditCardNotFound(CreditCardNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CreditCardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCreditCardAlreadyExists(CreditCardAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAccountAlreadyExists(AccountAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CreditCardBlockedException.class)
    public ResponseEntity<ErrorResponse> handleCreditCardBlocked(CreditCardBlockedException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler({
            AccountBlockedException.class,
            AccountNotActiveException.class,
            AccountRestrictedException.class,
            InsufficientBalanceException.class,
            InvalidAmountException.class,
            InvoiceNotPayableException.class,
            PaymentRequestNotPayableException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessRule(RuntimeException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(PaymentRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentRequestNotFound(PaymentRequestNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientCreditLimitException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCreditLimit(InsufficientCreditLimitException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(WrongCvvException.class)
    public ResponseEntity<ErrorResponse> handleWrongCvv(WrongCvvException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvoiceNotCloseableException.class)
    public ResponseEntity<ErrorResponse> handleInvoiceNotCloseable(InvoiceNotCloseableException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InvalidCardException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCard(InvalidCardException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), message));
    }

    public record ErrorResponse(Instant timestamp, int status, String message) {}
}
