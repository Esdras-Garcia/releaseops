package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidRequiredApprovalsException extends RuntimeException {
    public InvalidRequiredApprovalsException(String message) {
        super(message);
    }
}
