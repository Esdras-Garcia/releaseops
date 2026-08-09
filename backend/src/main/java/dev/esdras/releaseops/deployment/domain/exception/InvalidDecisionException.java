package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidDecisionException extends RuntimeException {
    public InvalidDecisionException(String message) {
        super(message);
    }
}
