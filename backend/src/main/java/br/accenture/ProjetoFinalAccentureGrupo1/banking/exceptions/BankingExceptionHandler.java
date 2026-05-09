package br.accenture.ProjetoFinalAccentureGrupo1.banking.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class BankingExceptionHandler {

//    @ExceptionHandler(BankAccountNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleBankAccountNotFound(BankAccountNotFoundException ex) {
//        return error(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(BankAccountInsufficientFundsException.class)
//    public ResponseEntity<ErrorResponse> handleBankAccountInsufficientFunds(BankAccountInsufficientFundsException ex) {
//        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
//    }
//
//    @ExceptionHandler(CreditCardNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleCreditCardNotFound(CreditCardNotFoundException ex) {
//        return error(HttpStatus.NOT_FOUND, ex.getMessage());
//    }
//
//    @ExceptionHandler(CreditCardAlreadyExistsException.class)
//    public ResponseEntity<ErrorResponse> handleCreditCardAlreadyExists(CreditCardAlreadyExistsException ex) {
//        return error(HttpStatus.CONFLICT, ex.getMessage());
//    }
//
//    @ExceptionHandler(CreditCardBlockedException.class)
//    public ResponseEntity<ErrorResponse> handleCreditCardBlocked(CreditCardBlockedException ex) {
//        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
//    }
//
//    @ExceptionHandler(InsufficientCreditLimitException.class)
//    public ResponseEntity<ErrorResponse> handleInsufficientCreditLimit(InsufficientCreditLimitException ex) {
//        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
//    }
//
//    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
//        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), message));
//    }
//
//    public record ErrorResponse(Instant timestamp, int status, String message) {}
}
