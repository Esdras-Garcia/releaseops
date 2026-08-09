package dev.esdras.releaseops.deployment.application.command;

import java.util.UUID;

public record RejectDeploymentRequestCommand(UUID deploymentRequestId, UUID reviewerId, String reason) {
}
