package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidCancellationReasonException extends RuntimeException {
    public InvalidCancellationReasonException(String message) {
        super(message);
    }
}
