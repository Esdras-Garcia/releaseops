package dev.esdras.releaseops.deployment.domain;

import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepository {

    Optional<DeploymentRequest> findById(UUID id);

    void save(DeploymentRequest deployment);
}
