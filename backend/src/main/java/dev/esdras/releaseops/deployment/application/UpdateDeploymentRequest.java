package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.UpdateDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

public class UpdateDeploymentRequest {

    private final DeploymentRequestFinder finder;
    private final DeploymentRepository repository;

    public UpdateDeploymentRequest(DeploymentRepository repository) {
        this.finder = new DeploymentRequestFinder(repository);
        this.repository = repository;
    }

    public DeploymentRequest execute(UpdateDeploymentRequestCommand command) {
        DeploymentRequest deploymentRequest = finder.find(command.deploymentRequestId());
        deploymentRequest.edit(command.title(), command.description(), command.rollbackPlan());
        repository.save(deploymentRequest);
        return deploymentRequest;
    }
}
