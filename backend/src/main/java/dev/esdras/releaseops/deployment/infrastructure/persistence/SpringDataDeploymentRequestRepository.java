package dev.esdras.releaseops.deployment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataDeploymentRequestRepository
        extends JpaRepository<DeploymentRequestEntity, UUID> {
}