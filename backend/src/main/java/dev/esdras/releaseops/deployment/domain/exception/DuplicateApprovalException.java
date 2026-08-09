package dev.esdras.releaseops.deployment.domain.exception;

public class DuplicateApprovalException extends RuntimeException {
    public DuplicateApprovalException(String message) {
        super(message);
    }
}
