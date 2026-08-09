package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public class SubmitDeploymentRequest {

    private final DeploymentRequestFinder finder;
    private final DeploymentRepository repository;
    private final Clock clock;

    public SubmitDeploymentRequest(DeploymentRepository repository, Clock clock) {
        this.finder = new DeploymentRequestFinder(repository);
        this.repository = repository;
        this.clock = clock;
    }

    public DeploymentRequest execute(UUID deploymentRequestId) {
        DeploymentRequest deploymentRequest = finder.find(deploymentRequestId);
        deploymentRequest.submit(Instant.now(clock));
        repository.save(deploymentRequest);
        return deploymentRequest;
    }
}
