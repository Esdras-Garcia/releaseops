package dev.esdras.releaseops.deployment.infrastructure.persistence;

import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.DeploymentStatus;
import dev.esdras.releaseops.deployment.domain.ReviewRound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
@Table(name = "deployment_requests")
public class DeploymentRequestEntity {

    @Id
    private UUID id;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "release_id", nullable = false)
    private UUID releaseId;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(name = "rollback_plan", nullable = false, length = 5000)
    private String rollbackPlan;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeploymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    protected DeploymentRequestEntity() {
    }

    private DeploymentRequestEntity(
            UUID id,
            UUID requesterId,
            UUID releaseId,
            UUID environmentId,
            String title,
            String description,
            String rollbackPlan,
            int requiredApprovals,
            DeploymentStatus status,
            Instant createdAt,
            String cancellationReason,
            Instant canceledAt
    ) {
        this.id = id;
        this.requesterId = requesterId;
        this.releaseId = releaseId;
        this.environmentId = environmentId;
        this.title = title;
        this.description = description;
        this.rollbackPlan = rollbackPlan;
        this.requiredApprovals = requiredApprovals;
        this.status = status;
        this.createdAt = createdAt;
        this.cancellationReason = cancellationReason;
        this.canceledAt = canceledAt;
    }

    public static DeploymentRequestEntity fromDomain(
            DeploymentRequest deployment
    ) {
        DeploymentRequestEntity entity =
                new DeploymentRequestEntity(
                        deployment.getId(),
                        deployment.getRequesterId(),
                        deployment.getReleaseId(),
                        deployment.getEnvironmentId(),
                        deployment.getTitle(),
                        deployment.getDescription(),
                        deployment.getRollbackPlan(),
                        deployment.getRequiredApprovals(),
                        deployment.getStatus(),
                        deployment.getCreatedAt(),
                        deployment.getCancellationReason(),
                        deployment.getCanceledAt()
                );

        deployment.getReviewRounds()
                .stream()
                .map(round -> ReviewRoundEntity.fromDomain(round, entity))
                .forEach(entity.reviewRounds::add);

        return entity;
    }

    void updateFromDomain(DeploymentRequest deployment) {
        this.requesterId = deployment.getRequesterId();
        this.releaseId = deployment.getReleaseId();
        this.environmentId = deployment.getEnvironmentId();
        this.title = deployment.getTitle();
        this.description = deployment.getDescription();
        this.rollbackPlan = deployment.getRollbackPlan();
        this.requiredApprovals = deployment.getRequiredApprovals();
        this.status = deployment.getStatus();
        this.createdAt = deployment.getCreatedAt();
        this.cancellationReason = deployment.getCancellationReason();
        this.canceledAt = deployment.getCanceledAt();

        List<ReviewRound> domainRounds = deployment.getReviewRounds();
        reviewRounds.removeIf(entity -> domainRounds.stream()
                .noneMatch(round -> round.getRoundNumber() == entity.getRoundNumber()));

        for (ReviewRound domainRound : domainRounds) {
            reviewRounds.stream()
                    .filter(entity -> entity.getRoundNumber() == domainRound.getRoundNumber())
                    .findFirst()
                    .ifPresentOrElse(
                            entity -> entity.updateFromDomain(domainRound),
                            () -> reviewRounds.add(ReviewRoundEntity.fromDomain(domainRound, this))
                    );
        }
    }

    public DeploymentRequest toDomain() {
        List<ReviewRound> restoredRounds = reviewRounds.stream()
                .map(ReviewRoundEntity::toDomain)
                .toList();

        return DeploymentRequest.restore(
                id,
                requesterId,
                releaseId,
                environmentId,
                title,
                description,
                rollbackPlan,
                requiredApprovals,
                createdAt,
                status,
                restoredRounds,
                cancellationReason,
                canceledAt
        );
    }

    @OneToMany(
            mappedBy = "deploymentRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )

    @OrderBy("roundNumber ASC")
    private List<ReviewRoundEntity> reviewRounds = new ArrayList<>();
}
