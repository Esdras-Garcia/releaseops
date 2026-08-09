package dev.esdras.releaseops.deployment.application.command;

import java.util.UUID;

public record CancelDeploymentRequestCommand(UUID deploymentRequestId, String reason) {
}
