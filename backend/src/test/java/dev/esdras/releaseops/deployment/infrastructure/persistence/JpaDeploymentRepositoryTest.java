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

    @Test
    void shouldPersistDeploymentRequestAcrossCompleteApprovalLifecycleWithoutDuplicatingChildren() {
        Instant createdAt = Instant.parse("2026-08-09T18:00:00Z");
        Instant submittedAt = Instant.parse("2026-08-09T19:00:00Z");
        Instant firstDecisionAt = Instant.parse("2026-08-09T20:00:00Z");
        Instant secondDecisionAt = Instant.parse("2026-08-09T21:00:00Z");
        UUID requesterId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();
        UUID firstReviewerId = UUID.randomUUID();
        UUID secondReviewerId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                requesterId,
                releaseId,
                environmentId,
                "Deploy ReleaseOps",
                "Deploy the persistence feature",
                "Restore the previous application version",
                2,
                createdAt
        );

        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        deployment = repository.findById(deployment.getId()).orElseThrow();
        deployment.submit(submittedAt);
        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        deployment = repository.findById(deployment.getId()).orElseThrow();
        deployment.approve(firstReviewerId, "First approval", firstDecisionAt);
        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        deployment = repository.findById(deployment.getId()).orElseThrow();
        deployment.approve(secondReviewerId, "Second approval", secondDecisionAt);
        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        DeploymentRequest found = repository.findById(deployment.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(DeploymentStatus.APPROVED);
        assertThat(found.getReviewRounds()).hasSize(1);
        assertThat(found.getReviewRounds().getFirst().getDecisions()).hasSize(2);
        assertThat(found.getRequesterId()).isEqualTo(requesterId);
        assertThat(found.getReleaseId()).isEqualTo(releaseId);
        assertThat(found.getEnvironmentId()).isEqualTo(environmentId);
        assertThat(found.getTitle()).isEqualTo("Deploy ReleaseOps");
        assertThat(found.getDescription()).isEqualTo("Deploy the persistence feature");
        assertThat(found.getRollbackPlan()).isEqualTo("Restore the previous application version");
        assertThat(found.getRequiredApprovals()).isEqualTo(2);
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);

        assertThat(found.getReviewRounds().getFirst().getRoundNumber()).isEqualTo(1);
        assertThat(found.getReviewRounds().getFirst().getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(found.getReviewRounds().getFirst().getDecisions())
                .extracting(ApprovalDecision::getReviewerId)
                .containsExactlyInAnyOrder(firstReviewerId, secondReviewerId);
        assertThat(found.getReviewRounds().getFirst().getDecisions())
                .extracting(ApprovalDecision::getComment)
                .containsExactlyInAnyOrder("First approval", "Second approval");
    }

    @Test
    void shouldPreserveRejectedDecisionAndCreateOrderedSecondRoundOnResubmission() {
        Instant createdAt = Instant.parse("2026-08-09T18:00:00Z");
        Instant firstSubmittedAt = Instant.parse("2026-08-09T19:00:00Z");
        Instant rejectedAt = Instant.parse("2026-08-09T20:00:00Z");
        Instant secondSubmittedAt = Instant.parse("2026-08-09T21:00:00Z");
        UUID reviewerId = UUID.randomUUID();
        DeploymentRequest deployment = newDeployment(createdAt, 1);

        deployment.submit(firstSubmittedAt);
        deployment.reject(reviewerId, "Rollback plan is incomplete", rejectedAt);
        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        deployment = repository.findById(deployment.getId()).orElseThrow();
        deployment.submit(secondSubmittedAt);
        repository.save(deployment);
        entityManager.flush();
        entityManager.clear();

        DeploymentRequest found = repository.findById(deployment.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(DeploymentStatus.PENDING_APPROVAL);
        assertThat(found.getReviewRounds()).hasSize(2);
        assertThat(found.getReviewRounds()).extracting(ReviewRound::getRoundNumber)
                .containsExactly(1, 2);
        assertThat(found.getReviewRounds().getFirst().getSubmittedAt()).isEqualTo(firstSubmittedAt);
        assertThat(found.getReviewRounds().getFirst().getDecisions()).singleElement()
                .satisfies(decision -> {
                    assertThat(decision.getReviewerId()).isEqualTo(reviewerId);
                    assertThat(decision.getType()).isEqualTo(ApprovalDecisionType.REJECTED);
                    assertThat(decision.getComment()).isEqualTo("Rollback plan is incomplete");
                    assertThat(decision.getDecidedAt()).isEqualTo(rejectedAt);
                });
        assertThat(found.getReviewRounds().getLast().getSubmittedAt()).isEqualTo(secondSubmittedAt);
        assertThat(found.getReviewRounds().getLast().getDecisions()).isEmpty();
    }

    @Test
    void shouldPersistCancellationFromDraftAndPendingApproval() {
        Instant createdAt = Instant.parse("2026-08-09T18:00:00Z");
        Instant draftCanceledAt = Instant.parse("2026-08-09T19:00:00Z");
        Instant submittedAt = Instant.parse("2026-08-09T19:00:00Z");
        Instant pendingCanceledAt = Instant.parse("2026-08-09T20:00:00Z");

        DeploymentRequest draft = newDeployment(createdAt, 1);
        draft.cancel("No longer needed", draftCanceledAt);
        repository.save(draft);
        entityManager.flush();
        entityManager.clear();
        DeploymentRequest foundDraft = repository.findById(draft.getId()).orElseThrow();

        DeploymentRequest pending = newDeployment(createdAt, 1);
        pending.submit(submittedAt);
        pending.cancel("Release window closed", pendingCanceledAt);
        repository.save(pending);
        entityManager.flush();
        entityManager.clear();
        DeploymentRequest foundPending = repository.findById(pending.getId()).orElseThrow();

        assertThat(foundDraft.getStatus()).isEqualTo(DeploymentStatus.CANCELED);
        assertThat(foundDraft.getCancellationReason()).isEqualTo("No longer needed");
        assertThat(foundDraft.getCanceledAt()).isEqualTo(draftCanceledAt);
        assertThat(foundPending.getStatus()).isEqualTo(DeploymentStatus.CANCELED);
        assertThat(foundPending.getCancellationReason()).isEqualTo("Release window closed");
        assertThat(foundPending.getCanceledAt()).isEqualTo(pendingCanceledAt);
        assertThat(foundPending.getReviewRounds()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyForUnknownDeploymentRequestId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    private DeploymentRequest newDeployment(Instant createdAt, int requiredApprovals) {
        return DeploymentRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Deploy ReleaseOps",
                "Deploy the persistence feature",
                "Restore the previous application version",
                requiredApprovals,
                createdAt
        );
    }
}

