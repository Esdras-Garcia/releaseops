package dev.esdras.releaseops.deployment.application.exception;

public class DeploymentNotFoundException extends RuntimeException {
    public DeploymentNotFoundException(String message) {
        super(message);
    }
}
