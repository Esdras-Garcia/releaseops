package dev.esdras.releaseops.deployment.application.command;

import java.util.UUID;

public record UpdateDeploymentRequestCommand(
        UUID deploymentRequestId, String title, String description, String rollbackPlan
) {
}
