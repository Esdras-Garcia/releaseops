package dev.esdras.releaseops.deployment.domain.exception;

public class InvalidDeploymentTransitionException extends RuntimeException {
    public InvalidDeploymentTransitionException(String message) {
        super(message);
    }
}
