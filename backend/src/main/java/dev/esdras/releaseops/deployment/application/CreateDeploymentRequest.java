package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.CreateDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import java.time.Clock;
import java.time.Instant;

public class CreateDeploymentRequest {

    private final DeploymentRepository repository;
    private final Clock clock;

    public CreateDeploymentRequest(DeploymentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public DeploymentRequest execute(CreateDeploymentRequestCommand command) {
        DeploymentRequest deploymentRequest = DeploymentRequest.create(
                command.requesterId(), command.releaseId(), command.environmentId(),
                command.title(), command.description(), command.rollbackPlan(),
                command.requiredApprovals(), Instant.now(clock)
        );
        repository.save(deploymentRequest);
        return deploymentRequest;
    }
}
