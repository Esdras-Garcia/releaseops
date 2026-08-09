package dev.esdras.releaseops.deployment.domain.exception;

public class SelfApprovalNotAllowedException extends RuntimeException {
    public SelfApprovalNotAllowedException(String message) {
        super(message);
    }
}
