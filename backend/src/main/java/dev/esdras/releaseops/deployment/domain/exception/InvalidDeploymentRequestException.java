package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidDeploymentRequestException extends RuntimeException {
    public InvalidDeploymentRequestException(String message) {
        super(message);
    }
}
