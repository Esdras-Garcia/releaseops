package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.RejectDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.time.Clock;
import java.time.Instant;

public class RejectDeploymentRequest {

    private final DeploymentRequestFinder finder;
    private final DeploymentRepository repository;
    private final Clock clock;

    public RejectDeploymentRequest(DeploymentRepository repository, Clock clock) {
        this.finder = new DeploymentRequestFinder(repository);
        this.repository = repository;
        this.clock = clock;
    }

    public DeploymentRequest execute(RejectDeploymentRequestCommand command) {
        DeploymentRequest deploymentRequest = finder.find(command.deploymentRequestId());
        deploymentRequest.reject(command.reviewerId(), command.reason(), Instant.now(clock));
        repository.save(deploymentRequest);
        return deploymentRequest;
    }
}
