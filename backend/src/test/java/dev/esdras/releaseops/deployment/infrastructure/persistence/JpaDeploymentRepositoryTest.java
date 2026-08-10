package dev.esdras.releaseops.deployment.infrastructure.persistence;

import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentStatus;
import dev.esdras.releaseops.deployment.domain.ReviewRound;
import dev.esdras.releaseops.deployment.domain.ApprovalDecision;
import dev.esdras.releaseops.deployment.domain.ApprovalDecisionType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
@Import(JpaDeploymentRepository.class)
public class JpaDeploymentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private DeploymentRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindDraftDeploymentRequest() {
        DeploymentRequest deployment = DeploymentRequest.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Deploy ReleaseOps",
            "Deploy the persistence feature",
            "Restore the previous application version",
            2,
            Instant.parse("2026-08-09T18:00:00Z")
        );

        repository.save(deployment);

        entityManager.flush();
        entityManager.clear();

        DeploymentRequest found = repository.findById(deployment.getId())
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(deployment.getId());
        assertThat(found.getRequesterId()).isEqualTo(deployment.getRequesterId());
        assertThat(found.getReleaseId()).isEqualTo(deployment.getReleaseId());
        assertThat(found.getEnvironmentId()).isEqualTo(deployment.getEnvironmentId());
        assertThat(found.getTitle()).isEqualTo(deployment.getTitle());
        assertThat(found.getDescription()).isEqualTo(deployment.getDescription());
        assertThat(found.getRollbackPlan()).isEqualTo(deployment.getRollbackPlan());
        assertThat(found.getRequiredApprovals()).isEqualTo(2);
        assertThat(found.getStatus()).isEqualTo(deployment.getStatus());
        assertThat(found.getCreatedAt()).isEqualTo(deployment.getCreatedAt());
    }

    @Test
    void shouldSaveAndFindSubmittedDeploymentRequest() {
        Instant createdAt =
                Instant.parse("2026-08-09T18:00:00Z");

        Instant submittedAt =
                Instant.parse("2026-08-09T19:00:00Z");

        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Deploy ReleaseOps",
                "Deploy the persistence feature",
                "Restore the previous application version",
                2,
                createdAt
        );

        deployment.submit(submittedAt);

        repository.save(deployment);

        entityManager.flush();
        entityManager.clear();

        DeploymentRequest found = repository.findById(deployment.getId())
                .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(DeploymentStatus.PENDING_APPROVAL);

        assertThat(found.getReviewRounds())
                .hasSize(1);

        ReviewRound firstRound = found.getReviewRounds().getFirst();

        assertThat(firstRound.getRoundNumber())
                .isEqualTo(1);

        assertThat(firstRound.getSubmittedAt())
                .isEqualTo(submittedAt);

        assertThat(firstRound.getDecisions())
                .isEmpty();
    }

    @Test
    void shouldSaveAndFindApprovalDecision() {
        Instant createdAt =
                Instant.parse("2026-08-09T18:00:00Z");

        Instant submittedAt =
                Instant.parse("2026-08-09T19:00:00Z");

        Instant decidedAt =
                Instant.parse("2026-08-09T20:00:00Z");

        UUID reviewerId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Deploy ReleaseOps",
                "Deploy the persistence feature",
                "Restore the previous application version",
                2,
                createdAt
        );

        deployment.submit(submittedAt);
        deployment.approve(
                reviewerId,
                "Approved after validation",
                decidedAt
        );

        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        DeploymentRequest found = repository.findById(deployment.getId())
                .orElseThrow();

        assertThat(found.getStatus())
                .isEqualTo(DeploymentStatus.PENDING_APPROVAL);

        assertThat(found.getReviewRounds())
                .hasSize(1);

        ReviewRound firstRound = found.getReviewRounds().getFirst();

        assertThat(firstRound.getDecisions())
                .hasSize(1);

        ApprovalDecision decision =
                firstRound.getDecisions().getFirst();

        assertThat(decision.getReviewerId())
                .isEqualTo(reviewerId);

        assertThat(decision.getType())
                .isEqualTo(ApprovalDecisionType.APPROVED);

        assertThat(decision.getComment())
                .isEqualTo("Approved after validation");

        assertThat(decision.getDecidedAt())
                .isEqualTo(decidedAt);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRestoreCompleteAggregateOutsideTransaction() {
        Instant createdAt =
                Instant.parse("2026-08-09T18:00:00Z");

        Instant submittedAt =
                Instant.parse("2026-08-09T19:00:00Z");

        Instant decidedAt =
                Instant.parse("2026-08-09T20:00:00Z");

        UUID reviewerId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Deploy ReleaseOps",
                "Deploy the persistence feature",
                "Restore the previous application version",
                2,
                createdAt
        );

        deployment.submit(submittedAt);
        deployment.approve(
                reviewerId,
                "Approved after validation",
                decidedAt
        );

        repository.save(deployment);

        DeploymentRequest found = repository.findById(deployment.getId())
                .orElseThrow();

        assertThat(found.getReviewRounds())
                .hasSize(1);

        assertThat(found.getReviewRounds().getFirst().getDecisions())
                .hasSize(1);
    }
}

