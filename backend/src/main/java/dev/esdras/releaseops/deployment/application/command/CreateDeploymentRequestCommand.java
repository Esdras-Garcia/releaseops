package dev.esdras.releaseops.deployment.application.command;

import java.util.UUID;

public record CreateDeploymentRequestCommand(
        UUID requesterId, UUID releaseId, UUID environmentId,
        String title, String description, String rollbackPlan,
        int requiredApprovals
) {
}
