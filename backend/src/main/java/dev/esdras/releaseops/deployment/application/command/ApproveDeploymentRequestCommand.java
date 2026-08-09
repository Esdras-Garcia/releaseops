package dev.esdras.releaseops.deployment.application.command;

import java.util.UUID;

public record ApproveDeploymentRequestCommand(UUID deploymentRequestId, UUID reviewerId, String comment) {
}
