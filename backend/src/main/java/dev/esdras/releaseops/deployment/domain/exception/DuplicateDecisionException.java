package dev.esdras.releaseops.deployment.domain.exception;

public class DuplicateDecisionException extends RuntimeException {
    public DuplicateDecisionException(String message) {
        super(message);
    }
}
