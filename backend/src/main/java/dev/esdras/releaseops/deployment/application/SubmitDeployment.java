package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.application.exception.DeploymentNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class SubmitDeployment {

    private final DeploymentRepository repository;
    private final Clock clock;

    public SubmitDeployment(DeploymentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void execute(UUID deploymentId) {
        DeploymentRequest deployment = repository
                .findById(deploymentId)
                .orElseThrow(() -> new DeploymentNotFoundException(
                        "Deployment not found: " + deploymentId
                ));

        deployment.submit(Instant.now(clock));

        repository.save(deployment);
    }
}
