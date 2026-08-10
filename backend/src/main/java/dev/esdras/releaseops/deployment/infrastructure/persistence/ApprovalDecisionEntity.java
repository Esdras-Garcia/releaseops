package dev.esdras.releaseops.deployment.infrastructure.persistence;

import dev.esdras.releaseops.deployment.domain.ApprovalDecision;
import dev.esdras.releaseops.deployment.domain.ApprovalDecisionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_decisions")
public class ApprovalDecisionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_round_id", nullable = false)
    private ReviewRoundEntity reviewRound;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 32)
    private ApprovalDecisionType type;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected ApprovalDecisionEntity() {
    }

    private ApprovalDecisionEntity(
            UUID id,
            ReviewRoundEntity reviewRound,
            UUID reviewerId,
            ApprovalDecisionType type,
            String comment,
            Instant decidedAt
    ) {
        this.id = id;
        this.reviewRound = reviewRound;
        this.reviewerId = reviewerId;
        this.type = type;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }

    public static ApprovalDecisionEntity fromDomain(
            ApprovalDecision decision,
            ReviewRoundEntity reviewRound
    ) {
        return new ApprovalDecisionEntity(
                UUID.randomUUID(),
                reviewRound,
                decision.getReviewerId(),
                decision.getType(),
                decision.getComment(),
                decision.getDecidedAt()
        );
    }

    public ApprovalDecision toDomain() {
        return ApprovalDecision.restore(
                reviewerId,
                type,
                comment,
                decidedAt
        );
    }
}