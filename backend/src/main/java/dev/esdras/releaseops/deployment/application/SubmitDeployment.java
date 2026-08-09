package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.application.exception.DeploymentNotFoundException;

import java.util.UUID;

public class SubmitDeployment {

    private final DeploymentRepository repository;

    public SubmitDeployment(DeploymentRepository repository) {
        this.repository = repository;
    }

    public void execute(UUID deploymentId) {
        DeploymentRequest deployment = repository
                .findById(deploymentId)
                .orElseThrow(() -> new DeploymentNotFoundException(
                        "Deployment not found: " + deploymentId
                ));

        deployment.submit();

        repository.save(deployment);
    }
}
