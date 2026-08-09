package dev.esdras.releaseops.deployment.domain;

import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
import dev.esdras.releaseops.deployment.domain.exception.DuplicateApprovalException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRequiredApprovalsException;
import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;

import java.util.Set;
import java.util.UUID;

public class DeploymentRequest {

    private final UUID id;
    private final UUID requesterId;
    private final UUID releaseId;
    private final UUID environmentId;
    private final int requiredApprovals;
    private final Set<UUID> approverIds;
    private DeploymentStatus status;

    private DeploymentRequest(
        UUID requesterId,
        UUID releaseId,
        UUID environmentId,
        int requiredApprovals
    ) {
        if (requiredApprovals <= 0) {
            throw new InvalidRequiredApprovalsException("Required approvals must be greater than zero");
        }

        this.id = UUID.randomUUID();
        this.requesterId = requesterId;
        this.releaseId = releaseId;
        this.environmentId = environmentId;
        this.requiredApprovals = requiredApprovals;
        this.approverIds = new java.util.HashSet<>();
        this.status = DeploymentStatus.DRAFT;
    }

    public static DeploymentRequest create(
        UUID requesterId,
        UUID releaseId,
        UUID environmentId,
        int requiredApprovals
    ) {
        return new DeploymentRequest(
                requesterId,
                releaseId,
                environmentId,
                requiredApprovals
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

        if (!approverIds.add(approverId)) {
            throw new DuplicateApprovalException("User has already approved this deployment");
        }

        if (approverIds.size() >= requiredApprovals) {
            status = DeploymentStatus.APPROVED;
        }
    }

    public Set<UUID> getApproverIds() {
        return Set.copyOf(approverIds);
    }
}
