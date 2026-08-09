package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.util.UUID;

public class GetDeploymentRequest {

    private final DeploymentRequestFinder finder;

    public GetDeploymentRequest(DeploymentRepository repository) {
        this.finder = new DeploymentRequestFinder(repository);
    }

    public DeploymentRequest execute(UUID deploymentRequestId) {
        return finder.find(deploymentRequestId);
    }
}
