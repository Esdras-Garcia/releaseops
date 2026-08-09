package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidRejectionReasonException extends RuntimeException {
    public InvalidRejectionReasonException(String message) {
        super(message);
    }
}
