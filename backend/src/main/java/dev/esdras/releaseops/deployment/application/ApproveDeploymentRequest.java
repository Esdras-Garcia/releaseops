package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.ApproveDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.time.Clock;
import java.time.Instant;

public class ApproveDeploymentRequest {

    private final DeploymentRequestFinder finder;
    private final DeploymentRepository repository;
    private final Clock clock;

    public ApproveDeploymentRequest(DeploymentRepository repository, Clock clock) {
        this.finder = new DeploymentRequestFinder(repository);
        this.repository = repository;
        this.clock = clock;
    }

    public DeploymentRequest execute(ApproveDeploymentRequestCommand command) {
        DeploymentRequest deploymentRequest = finder.find(command.deploymentRequestId());
        deploymentRequest.approve(command.reviewerId(), command.comment(), Instant.now(clock));
        repository.save(deploymentRequest);
        return deploymentRequest;
    }
}
