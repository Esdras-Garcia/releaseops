package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.exception.DeploymentRequestNotFoundException;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.util.UUID;

final class DeploymentRequestFinder {

    private final DeploymentRepository repository;

    DeploymentRequestFinder(DeploymentRepository repository) {
        this.repository = repository;
    }

    DeploymentRequest find(UUID deploymentRequestId) {
        return repository.findById(deploymentRequestId)
                .orElseThrow(() -> new DeploymentRequestNotFoundException(
                        "Deployment request not found: " + deploymentRequestId
                ));
    }

}
