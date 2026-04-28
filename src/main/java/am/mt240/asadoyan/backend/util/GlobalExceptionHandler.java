package am.mt240.asadoyan.backend.util;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public List<Map<String, String>> jakartaValidationExceptionHandler(HttpServletResponse response,
                                                                       MethodArgumentNotValidException ex) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(Map.of("field", error.getField(), "message", error.getDefaultMessage())));
        return errors;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public List<Map<String, String>> constraintViolationExceptionHandler(HttpServletResponse response,
                                                                         ConstraintViolationException ex) {

        response.setStatus(HttpStatus.BAD_REQUEST.value());

        return ex.getConstraintViolations()
                .stream()
                .map(v -> Map.of(
                        "field", v.getPropertyPath().toString(),
                        "message", v.getMessage()
                ))
                .toList();
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public Map<String, Object> handleResponseStatus(ResponseStatusException ex, HttpServletResponse response) {
        response.setStatus(ex.getStatusCode().value());
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getReason());
        return body;
    }
}
