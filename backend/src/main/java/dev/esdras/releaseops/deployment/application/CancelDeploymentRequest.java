package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.CancelDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.time.Clock;
import java.time.Instant;

public class CancelDeploymentRequest {

    private final DeploymentRequestFinder finder;
    private final DeploymentRepository repository;
    private final Clock clock;

    public CancelDeploymentRequest(DeploymentRepository repository, Clock clock) {
        this.finder = new DeploymentRequestFinder(repository);
        this.repository = repository;
        this.clock = clock;
    }

    public DeploymentRequest execute(CancelDeploymentRequestCommand command) {
        DeploymentRequest deploymentRequest = finder.find(command.deploymentRequestId());
        deploymentRequest.cancel(command.reason(), Instant.now(clock));
        repository.save(deploymentRequest);
        return deploymentRequest;
    }
}
