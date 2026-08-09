package dev.esdras.releaseops.deployment.domain;

import dev.esdras.releaseops.deployment.domain.exception.DuplicateDecisionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidCancellationReasonException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDecisionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentRequestException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidLifecycleTimestampException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRejectionReasonException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRequiredApprovalsException;
import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DeploymentRequest {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_TEXT_LENGTH = 5_000;

    private final UUID id;
    private final UUID requesterId;
    private final UUID releaseId;
    private final UUID environmentId;
    private final int requiredApprovals;
    private final Instant createdAt;
    private final List<ReviewRound> reviewRounds;
    private String title;
    private String description;
    private String rollbackPlan;
    private DeploymentStatus status;
    private String cancellationReason;
    private Instant canceledAt;

    private DeploymentRequest(
            UUID id,
            UUID requesterId,
            UUID releaseId,
            UUID environmentId,
            String title,
            String description,
            String rollbackPlan,
            int requiredApprovals,
            Instant createdAt
    ) {
        validateRequiredIds(requesterId, releaseId, environmentId, createdAt);
        validateRequiredApprovals(requiredApprovals);
        validateText("title", title, MAX_TITLE_LENGTH);
        validateText("description", description, MAX_TEXT_LENGTH);
        validateText("rollback plan", rollbackPlan, MAX_TEXT_LENGTH);

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.requesterId = requesterId;
        this.releaseId = releaseId;
        this.environmentId = environmentId;
        this.title = title;
        this.description = description;
        this.rollbackPlan = rollbackPlan;
        this.requiredApprovals = requiredApprovals;
        this.createdAt = createdAt;
        this.reviewRounds = new ArrayList<>();
        this.status = DeploymentStatus.DRAFT;
    }

    public static DeploymentRequest create(
            UUID requesterId,
            UUID releaseId,
            UUID environmentId,
            String title,
            String description,
            String rollbackPlan,
            int requiredApprovals,
            Instant createdAt
    ) {
        return new DeploymentRequest(
                UUID.randomUUID(),
                requesterId,
                releaseId,
                environmentId,
                title,
                description,
                rollbackPlan,
                requiredApprovals,
                createdAt
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public UUID getReleaseId() {
        return releaseId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRollbackPlan() {
        return rollbackPlan;
    }

    public int getRequiredApprovals() {
        return requiredApprovals;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ReviewRound> getReviewRounds() {
        return List.copyOf(reviewRounds);
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public void edit(String title, String description, String rollbackPlan) {
        ensureStatus(DeploymentStatus.DRAFT, "Only draft deployments can be edited");
        validateText("title", title, MAX_TITLE_LENGTH);
        validateText("description", description, MAX_TEXT_LENGTH);
        validateText("rollback plan", rollbackPlan, MAX_TEXT_LENGTH);

        this.title = title;
        this.description = description;
        this.rollbackPlan = rollbackPlan;
    }

    public void submit(Instant submittedAt) {
        ensureStatus(DeploymentStatus.DRAFT, "Only draft deployments can be submitted");
        if (submittedAt == null) {
            throw new InvalidLifecycleTimestampException("Submitted at must not be null");
        }
        if (submittedAt.isBefore(createdAt)) {
            throw new InvalidLifecycleTimestampException("Submitted at cannot be before created at");
        }

        reviewRounds.add(new ReviewRound(reviewRounds.size() + 1, submittedAt));
        status = DeploymentStatus.PENDING_APPROVAL;
    }

    public void approve(UUID reviewerId, String comment, Instant decidedAt) {
        ensurePendingApproval();
        validateDecisionReviewer(reviewerId);
        validateDecisionTime(decidedAt);
        if (comment != null && comment.isBlank()) {
            throw new InvalidDecisionException("Approval comment must not contain only whitespace");
        }

        ReviewRound currentRound = currentRound();
        ensureNoPreviousDecision(currentRound, reviewerId);
        currentRound.addDecision(new ApprovalDecision(
                reviewerId, ApprovalDecisionType.APPROVED, comment, decidedAt
        ));

        if (currentRound.approvedDecisionCount() >= requiredApprovals) {
            status = DeploymentStatus.APPROVED;
        }
    }

    public void reject(UUID reviewerId, String reason, Instant decidedAt) {
        ensurePendingApproval();
        validateDecisionReviewer(reviewerId);
        validateDecisionTime(decidedAt);
        if (reason == null || reason.isBlank()) {
            throw new InvalidRejectionReasonException("Rejection reason must not be empty");
        }

        ReviewRound currentRound = currentRound();
        ensureNoPreviousDecision(currentRound, reviewerId);
        currentRound.addDecision(new ApprovalDecision(
                reviewerId, ApprovalDecisionType.REJECTED, reason, decidedAt
        ));
        status = DeploymentStatus.DRAFT;
    }

    public void cancel(String reason, Instant canceledAt) {
        if (status == DeploymentStatus.CANCELED) {
            throw new InvalidDeploymentTransitionException("Canceled deployments cannot be canceled again");
        }
        if (status != DeploymentStatus.DRAFT
                && status != DeploymentStatus.PENDING_APPROVAL
                && status != DeploymentStatus.APPROVED) {
            throw new InvalidDeploymentTransitionException("Deployment cannot be canceled from its current state");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidCancellationReasonException("Cancellation reason must not be empty");
        }
        if (canceledAt == null) {
            throw new InvalidLifecycleTimestampException("Canceled at must not be null");
        }
        if (canceledAt.isBefore(latestEventAt())) {
            throw new InvalidLifecycleTimestampException("Canceled at cannot be before the latest deployment event");
        }

        this.cancellationReason = reason;
        this.canceledAt = canceledAt;
        this.status = DeploymentStatus.CANCELED;
    }

    private void ensurePendingApproval() {
        ensureStatus(DeploymentStatus.PENDING_APPROVAL, "Only pending deployments can receive decisions");
    }

    private void ensureStatus(DeploymentStatus expectedStatus, String message) {
        if (status != expectedStatus) {
            throw new InvalidDeploymentTransitionException(message);
        }
    }

    private ReviewRound currentRound() {
        return reviewRounds.getLast();
    }

    private void ensureNoPreviousDecision(ReviewRound round, UUID reviewerId) {
        if (round.hasDecisionBy(reviewerId)) {
            throw new DuplicateDecisionException("Reviewer has already decided in this review round");
        }
    }

    private void validateDecisionReviewer(UUID reviewerId) {
        if (reviewerId == null) {
            throw new InvalidDecisionException("Reviewer ID must not be null");
        }
        if (reviewerId.equals(requesterId)) {
            throw new SelfApprovalNotAllowedException("Requester cannot decide on their own deployment");
        }
    }

    private void validateDecisionTime(Instant decidedAt) {
        if (decidedAt == null) {
            throw new InvalidLifecycleTimestampException("Decision time must not be null");
        }
        if (decidedAt.isBefore(currentRound().getSubmittedAt())) {
            throw new InvalidLifecycleTimestampException("Decision time cannot be before the current round submission");
        }
    }

    private Instant latestEventAt() {
        Instant latestEvent = createdAt;
        for (ReviewRound round : reviewRounds) {
            if (round.getSubmittedAt().isAfter(latestEvent)) {
                latestEvent = round.getSubmittedAt();
            }
            for (ApprovalDecision decision : round.getDecisions()) {
                if (decision.getDecidedAt().isAfter(latestEvent)) {
                    latestEvent = decision.getDecidedAt();
                }
            }
        }
        return latestEvent;
    }

    private static void validateRequiredIds(
            UUID requesterId, UUID releaseId, UUID environmentId, Instant createdAt
    ) {
        if (requesterId == null || releaseId == null || environmentId == null || createdAt == null) {
            throw new InvalidDeploymentRequestException(
                    "Requester ID, release ID, environment ID and created at are required"
            );
        }
    }

    private static void validateRequiredApprovals(int requiredApprovals) {
        if (requiredApprovals <= 0) {
            throw new InvalidRequiredApprovalsException("Required approvals must be greater than zero");
        }
    }

    private static void validateText(String fieldName, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidDeploymentRequestException(fieldName + " must not be empty");
        }
        if (value.length() > maxLength) {
            throw new InvalidDeploymentRequestException(
                    fieldName + " must not exceed " + maxLength + " characters"
            );
        }
    }
}
