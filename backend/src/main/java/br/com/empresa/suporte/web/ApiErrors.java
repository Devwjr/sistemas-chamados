package br.com.empresa.suporte.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.*;

import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    public record FieldError(String field, String message) {}

    public record Problem(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            String code,
            List<FieldError> fieldErrors) {}

    public static Problem problem(int status, String code, String detail, String path) {
        return new Problem(
                "about:blank",
                HttpStatus.valueOf(status).getReasonPhrase(),
                status,
                detail,
                path,
                code,
                List.of());
    }

    public static void write(
            ObjectMapper mapper,
            HttpServletRequest req,
            HttpServletResponse res,
            int status,
            String code,
            String detail)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json;charset=UTF-8");
        mapper.writeValue(
                res.getOutputStream(), problem(status, code, detail, req.getRequestURI()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Problem> custom(ApiException e, HttpServletRequest r) {
        return ResponseEntity.status(e.status)
                .body(problem(e.status, e.code, e.getMessage(), r.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Problem> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        var fields =
                e.getBindingResult().getFieldErrors().stream()
                        .map(f -> new FieldError(f.getField(), f.getDefaultMessage()))
                        .toList();
        return ResponseEntity.badRequest()
                .body(
                        new Problem(
                                "about:blank",
                                "Dados inválidos",
                                400,
                                "Revise os campos informados.",
                                r.getRequestURI(),
                                "VALIDATION_ERROR",
                                fields));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<Problem> malformed(Exception e, HttpServletRequest r) {
        return ResponseEntity.badRequest()
                .body(problem(400, "INVALID_INPUT", "Requisição inválida.", r.getRequestURI()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Problem> concurrent(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(409)
                .body(
                        problem(
                                409,
                                "STALE_VERSION",
                                "O chamado foi alterado. Recarregue e revise sua ação.",
                                r.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Problem> integrity(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(409)
                .body(
                        problem(
                                409,
                                "DATA_CONFLICT",
                                "Dados duplicados ou incompatíveis com os registros existentes.",
                                r.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Problem> denied(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(403)
                .body(
                        problem(
                                403,
                                "FORBIDDEN",
                                "Você não tem permissão para esta ação.",
                                r.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Problem> unexpected(Exception e, HttpServletRequest r) {
        String id = UUID.randomUUID().toString();
        // Não registrar bodies, senhas ou mensagens de SQL potencialmente sensíveis.
        log.error("Falha {} na rota {}: {}", id, r.getRequestURI(), e.getClass().getName());
        return ResponseEntity.internalServerError()
                .body(
                        problem(
                                500,
                                "INTERNAL_ERROR",
                                "Falha inesperada. Referência: " + id,
                                r.getRequestURI()));
    }
}
