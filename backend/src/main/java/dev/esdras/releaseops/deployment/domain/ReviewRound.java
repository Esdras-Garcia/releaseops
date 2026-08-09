package dev.esdras.releaseops.deployment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReviewRound {

    private final int number;
    private final Instant submittedAt;
    private final List<ApprovalDecision> decisions = new ArrayList<>();

    ReviewRound(int number, Instant submittedAt) {
        this.number = number;
        this.submittedAt = submittedAt;
    }

    public int getRoundNumber() {
        return number;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public List<ApprovalDecision> getDecisions() {
        return List.copyOf(decisions);
    }

    void addDecision(ApprovalDecision decision) {
        decisions.add(decision);
    }

    boolean hasDecisionBy(UUID reviewerId) {
        return decisions.stream().anyMatch(decision -> decision.getReviewerId().equals(reviewerId));
    }

    long approvedDecisionCount() {
        return decisions.stream()
                .filter(decision -> decision.getType() == ApprovalDecisionType.APPROVED)
                .count();
    }
}
