package br.accenture.ProjetoFinalAccentureGrupo1.ecommerce.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class EcommerceExceptionHandler {

    @ExceptionHandler({
            CartItemNotFoundException.class,
            CartNotFoundException.class,
            CategoryNotFoundException.class,
            CustomerNotFoundException.class,
            OrderNotFound.class,
            SavedCardNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            CategoryAlreadyExistsException.class,
            SavedCardAlreadyExistsException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
            CartEmptyException.class,
            CartWasNotClosedException.class,
            CartAlreadyClosed.class,
            IllegalOrderStatusException.class,
            InsufficientStockException.class,
            OrderShippedException.class,
            ProductNotAvailableException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessRule(RuntimeException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), message));
    }

    public record ErrorResponse(Instant timestamp, int status, String message) {}
}
