package dev.esdras.releaseops.deployment.infrastructure.persistence;

import dev.esdras.releaseops.deployment.domain.ApprovalDecision;
import dev.esdras.releaseops.deployment.domain.ReviewRound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.util.ArrayList;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "review_rounds")
public class ReviewRoundEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deployment_request_id", nullable = false)
    private DeploymentRequestEntity deploymentRequest;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected ReviewRoundEntity() {
    }

    private ReviewRoundEntity(
            UUID id,
            DeploymentRequestEntity deploymentRequest,
            int roundNumber,
            Instant submittedAt
    ) {
        this.id = id;
        this.deploymentRequest = deploymentRequest;
        this.roundNumber = roundNumber;
        this.submittedAt = submittedAt;
    }

    public static ReviewRoundEntity fromDomain(
            ReviewRound round,
            DeploymentRequestEntity deploymentRequest
    ) {
        ReviewRoundEntity entity = new ReviewRoundEntity(
                UUID.randomUUID(),
                deploymentRequest,
                round.getRoundNumber(),
                round.getSubmittedAt()
        );

        round.getDecisions()
                .stream()
                .map(decision ->
                        ApprovalDecisionEntity.fromDomain(decision, entity)
                )
                .forEach(entity.decisions::add);

        return entity;
    }

    int getRoundNumber() {
        return roundNumber;
    }

    void updateFromDomain(ReviewRound round) {
        this.submittedAt = round.getSubmittedAt();
        List<ApprovalDecision> domainDecisions = round.getDecisions();
        decisions.removeIf(entity -> domainDecisions.stream()
                .noneMatch(decision -> decision.getReviewerId().equals(entity.getReviewerId())));

        for (ApprovalDecision domainDecision : domainDecisions) {
            decisions.stream()
                    .filter(entity -> entity.getReviewerId().equals(domainDecision.getReviewerId()))
                    .findFirst()
                    .ifPresentOrElse(
                            entity -> entity.updateFromDomain(domainDecision),
                            () -> decisions.add(ApprovalDecisionEntity.fromDomain(domainDecision, this))
                    );
        }
    }

    public ReviewRound toDomain() {
        List<ApprovalDecision> restoredDecisions =
                decisions.stream()
                        .map(ApprovalDecisionEntity::toDomain)
                        .toList();

        return ReviewRound.restore(
                roundNumber,
                submittedAt,
                restoredDecisions
        );
    }

    @OneToMany(
            mappedBy = "reviewRound",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("decidedAt ASC")
    private List<ApprovalDecisionEntity> decisions = new ArrayList<>();
}
