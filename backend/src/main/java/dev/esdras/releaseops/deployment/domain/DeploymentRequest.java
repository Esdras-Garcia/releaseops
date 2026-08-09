package dev.esdras.releaseops.deployment.domain;

import java.util.UUID;

public class DeploymentRequest {

    private final UUID requesterId;
    private final UUID releaseId;
    private final UUID environmentId;
    private final DeploymentStatus status;

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
}