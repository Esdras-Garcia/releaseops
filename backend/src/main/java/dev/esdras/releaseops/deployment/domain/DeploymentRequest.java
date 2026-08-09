package dev.esdras.releaseops.deployment.domain;

import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;

import java.util.UUID;

public class DeploymentRequest {

    private final UUID requesterId;
    private final UUID releaseId;
    private final UUID environmentId;
    private DeploymentStatus status;

    private DeploymentRequest(
        UUID requesterId,
        UUID releaseId,
        UUID environmentId
    ) {
        this.requesterId = requesterId;
        this.releaseId = releaseId;
        this.environmentId = environmentId;
        this.status = DeploymentStatus.DRAFT;
    }

    public static DeploymentRequest create(
        UUID requesterId,
        UUID releaseId,
        UUID environmentId
    ) {
        return new DeploymentRequest(
            requesterId,
            releaseId,
            environmentId
        );
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void submit() {

        if (status != DeploymentStatus.DRAFT) {
            throw new InvalidDeploymentTransitionException("Only draft deployments can be submitted");
        }

        this.status = DeploymentStatus.PENDING_APPROVAL;
    }

    public void approve(UUID approverId) {
        if (status != DeploymentStatus.PENDING_APPROVAL) {
            throw new InvalidDeploymentTransitionException("Only pending deployments can be approved");
        }

        if (approverId.equals(requesterId)) {
            throw new SelfApprovalNotAllowedException("Requester cannot approve their own deployment");
        }
    }
}