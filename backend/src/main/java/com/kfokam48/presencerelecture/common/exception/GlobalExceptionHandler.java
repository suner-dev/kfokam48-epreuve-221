package com.kfokam48.presencerelecture.common.exception;

import com.kfokam48.presencerelecture.common.api.ApiErrorResponse;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ApiErrorResponse(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        boolean hasNote = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> error.getField().equals("note"));
        boolean hasLink = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> error.getField().equals("lien"));
        String code = hasNote ? "NOTE_INVALIDE" : hasLink ? "LIEN_INVALIDE" : "CHAMP_MANQUANT";
        String message = hasNote
                ? "La note doit être un entier compris entre 0 et 20."
                : hasLink
                ? "Le lien doit être une URI valide."
                : "La requête contient des champs invalides ou manquants.";
        return ResponseEntity.badRequest().body(new ApiErrorResponse(code, message));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception) {
        boolean hasNote = exception.getMessage() != null && exception.getMessage().contains("note");
        String code = hasNote ? "NOTE_INVALIDE" : "REQUETE_INVALIDE";
        String message = hasNote
                ? "La note doit être un entier compris entre 0 et 20."
                : "La requête est invalide.";
        return ResponseEntity.badRequest().body(new ApiErrorResponse(code, message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponse("METHODE_NON_AUTORISEE", "La méthode HTTP n'est pas autorisée pour cette ressource."));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("RESOURCE_INTROUVABLE", "La ressource demandée est introuvable."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(Exception exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("CONFLIT_CONCURRENCE", "La ressource existe déjà ou est en cours de modification."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("ERREUR_INTERNE", "Une erreur interne est survenue."));
    }
}
