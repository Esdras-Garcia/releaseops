package dev.esdras.releaseops.deployment.domain;

import java.time.Instant;
import java.util.UUID;

public class ApprovalDecision {

    private final UUID reviewerId;
    private final ApprovalDecisionType type;
    private final String comment;
    private final Instant decidedAt;

    ApprovalDecision(UUID reviewerId, ApprovalDecisionType type, String comment, Instant decidedAt) {
        this.reviewerId = reviewerId;
        this.type = type;
        this.comment = comment;
        this.decidedAt = decidedAt;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public ApprovalDecisionType getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
