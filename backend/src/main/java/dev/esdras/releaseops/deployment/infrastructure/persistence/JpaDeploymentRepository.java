package dev.esdras.releaseops.deployment.infrastructure.persistence;

import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDeploymentRepository implements DeploymentRepository {

    private final SpringDataDeploymentRequestRepository repository;

    public JpaDeploymentRepository(
            SpringDataDeploymentRequestRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeploymentRequest> findById(UUID id) {
        return repository.findById(id)
                .map(DeploymentRequestEntity::toDomain);
    }

    @Override
    @Transactional
    public void save(DeploymentRequest deployment) {
        repository.findById(deployment.getId())
                .ifPresentOrElse(
                        entity -> entity.updateFromDomain(deployment),
                        () -> repository.save(DeploymentRequestEntity.fromDomain(deployment))
                );
    }
}
