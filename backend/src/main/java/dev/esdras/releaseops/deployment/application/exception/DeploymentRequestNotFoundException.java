package dev.esdras.releaseops.deployment.application.exception;

public class DeploymentRequestNotFoundException extends RuntimeException {

    public DeploymentRequestNotFoundException(String message) {
        super(message);
    }
}
