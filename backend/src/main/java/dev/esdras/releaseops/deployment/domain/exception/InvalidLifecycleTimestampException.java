package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidLifecycleTimestampException extends RuntimeException {
    public InvalidLifecycleTimestampException(String message) {
        super(message);
    }
}
