package dev.esdras.releaseops.deployment.domain;

import dev.esdras.releaseops.deployment.domain.exception.DuplicateDecisionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidCancellationReasonException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDecisionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentRequestException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRejectionReasonException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRequiredApprovalsException;
import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-08-01T10:05:00Z");
    private static final Instant DECIDED_AT = Instant.parse("2026-08-01T10:10:00Z");
    private static final Instant CANCELED_AT = Instant.parse("2026-08-01T10:15:00Z");

    @Test
    void shouldCreateDraftWithIdentityAndCreationData() {
        UUID requesterId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        DeploymentRequest deployment = create(requesterId, releaseId, environmentId, 2);

        assertThat(deployment.getId()).isNotNull();
        assertThat(deployment.getRequesterId()).isEqualTo(requesterId);
        assertThat(deployment.getReleaseId()).isEqualTo(releaseId);
        assertThat(deployment.getEnvironmentId()).isEqualTo(environmentId);
        assertThat(deployment.getTitle()).isEqualTo("Release API");
        assertThat(deployment.getDescription()).isEqualTo("Deploy the API release");
        assertThat(deployment.getRollbackPlan()).isEqualTo("Restore the previous release");
        assertThat(deployment.getRequiredApprovals()).isEqualTo(2);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.DRAFT);
        assertThat(deployment.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(deployment.getReviewRounds()).isEmpty();
    }

    @Test
    void shouldRejectNullRequiredCreationData() {
        assertThatThrownBy(() -> create(null, UUID.randomUUID(), UUID.randomUUID(), 1))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> create(UUID.randomUUID(), null, UUID.randomUUID(), 1))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> create(UUID.randomUUID(), UUID.randomUUID(), null, 1))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Release API", "Deploy the API release", "Restore the previous release", 1, null
        )).isInstanceOf(InvalidDeploymentRequestException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveRequiredApprovals(int requiredApprovals) {
        assertThatThrownBy(() -> create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), requiredApprovals))
                .isInstanceOf(InvalidRequiredApprovalsException.class)
                .hasMessage("Required approvals must be greater than zero");
    }

    @Test
    void shouldRejectEmptyOrWhitespaceText() {
        assertThatThrownBy(() -> createWithText("", "Description", "Rollback"))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("   ", "Description", "Rollback"))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("Title", "", "Rollback"))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("Title", "   ", "Rollback"))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("Title", "Description", ""))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("Title", "Description", "   "))
                .isInstanceOf(InvalidDeploymentRequestException.class);
    }

    @Test
    void shouldRejectTextAboveLimits() {
        assertThatThrownBy(() -> createWithText("x".repeat(121), "Description", "Rollback"))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("Title", "x".repeat(5_001), "Rollback"))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        assertThatThrownBy(() -> createWithText("Title", "Description", "x".repeat(5_001)))
                .isInstanceOf(InvalidDeploymentRequestException.class);
    }

    @Test
    void shouldEditDraftOnly() {
        DeploymentRequest deployment = create();

        deployment.edit("Updated title", "Updated description", "Updated rollback");

        assertThat(deployment.getTitle()).isEqualTo("Updated title");
        assertThat(deployment.getDescription()).isEqualTo("Updated description");
        assertThat(deployment.getRollbackPlan()).isEqualTo("Updated rollback");
    }

    @Test
    void shouldPreserveImmutableCreationDataWhenEdited() {
        DeploymentRequest deployment = create();
        UUID id = deployment.getId();
        UUID requesterId = deployment.getRequesterId();
        UUID releaseId = deployment.getReleaseId();
        UUID environmentId = deployment.getEnvironmentId();

        deployment.edit("Updated title", "Updated description", "Updated rollback");

        assertThat(deployment.getId()).isEqualTo(id);
        assertThat(deployment.getRequesterId()).isEqualTo(requesterId);
        assertThat(deployment.getReleaseId()).isEqualTo(releaseId);
        assertThat(deployment.getEnvironmentId()).isEqualTo(environmentId);
        assertThat(deployment.getRequiredApprovals()).isEqualTo(1);
        assertThat(deployment.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void shouldRejectEditingNonDraftStates() {
        DeploymentRequest pending = create();
        pending.submit(SUBMITTED_AT);
        assertEditRejected(pending);

        DeploymentRequest approved = create();
        approved.submit(SUBMITTED_AT);
        approved.approve(UUID.randomUUID(), "Approved", DECIDED_AT);
        assertEditRejected(approved);

        DeploymentRequest canceled = create();
        canceled.cancel("No longer needed", CANCELED_AT);
        assertEditRejected(canceled);
    }

    @Test
    void shouldSubmitDraftIntoNewReviewRound() {
        DeploymentRequest deployment = create();

        deployment.submit(SUBMITTED_AT);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING_APPROVAL);
        assertThat(deployment.getReviewRounds()).hasSize(1);
        assertThat(deployment.getReviewRounds().get(0).getRoundNumber()).isEqualTo(1);
        assertThat(deployment.getReviewRounds().get(0).getSubmittedAt()).isEqualTo(SUBMITTED_AT);
        assertThat(deployment.getReviewRounds().get(0).getDecisions()).isEmpty();
    }

    @Test
    void shouldRejectSubmissionWithoutSubmissionTime() {
        assertThatThrownBy(() -> create().submit(null))
                .isInstanceOf(InvalidDecisionException.class)
                .hasMessage("Submitted at must not be null");
    }

    @Test
    void shouldRejectSecondSubmissionWhilePending() {
        DeploymentRequest deployment = create();
        deployment.submit(SUBMITTED_AT);

        assertThatThrownBy(() -> deployment.submit(SUBMITTED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidDeploymentTransitionException.class)
                .hasMessage("Only draft deployments can be submitted");
    }

    @Test
    void shouldRecordApprovalAndApproveAtRequiredCount() {
        DeploymentRequest deployment = create(2);
        UUID reviewerId = UUID.randomUUID();
        deployment.submit(SUBMITTED_AT);

        deployment.approve(reviewerId, "Looks good", DECIDED_AT);

        ApprovalDecision decision = deployment.getReviewRounds().get(0).getDecisions().get(0);
        assertThat(decision.getReviewerId()).isEqualTo(reviewerId);
        assertThat(decision.getType()).isEqualTo(ApprovalDecisionType.APPROVED);
        assertThat(decision.getComment()).isEqualTo("Looks good");
        assertThat(decision.getDecidedAt()).isEqualTo(DECIDED_AT);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING_APPROVAL);

        deployment.approve(UUID.randomUUID(), null, DECIDED_AT.plusSeconds(1));

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.APPROVED);
    }

    @Test
    void shouldRequireReviewerAndDecisionTimeAndRejectWhitespaceApprovalComment() {
        DeploymentRequest deployment = create();
        deployment.submit(SUBMITTED_AT);

        assertThatThrownBy(() -> deployment.approve(null, null, DECIDED_AT))
                .isInstanceOf(InvalidDecisionException.class);
        assertThatThrownBy(() -> deployment.approve(UUID.randomUUID(), null, null))
                .isInstanceOf(InvalidDecisionException.class);
        assertThatThrownBy(() -> deployment.approve(UUID.randomUUID(), "   ", DECIDED_AT))
                .isInstanceOf(InvalidDecisionException.class);
    }

    @Test
    void shouldRejectSelfApprovalAndSelfRejection() {
        UUID requesterId = UUID.randomUUID();
        DeploymentRequest deployment = create(requesterId, UUID.randomUUID(), UUID.randomUUID(), 1);
        deployment.submit(SUBMITTED_AT);

        assertThatThrownBy(() -> deployment.approve(requesterId, "Approved", DECIDED_AT))
                .isInstanceOf(SelfApprovalNotAllowedException.class);
        assertThatThrownBy(() -> deployment.reject(requesterId, "Rejected", DECIDED_AT))
                .isInstanceOf(SelfApprovalNotAllowedException.class);
    }

    @Test
    void shouldRejectDuplicateDecisionInSameRound() {
        DeploymentRequest deployment = create(2);
        UUID reviewerId = UUID.randomUUID();
        deployment.submit(SUBMITTED_AT);
        deployment.approve(reviewerId, "Approved", DECIDED_AT);

        assertThatThrownBy(() -> deployment.reject(reviewerId, "Rejected", DECIDED_AT.plusSeconds(1)))
                .isInstanceOf(DuplicateDecisionException.class);
    }

    @Test
    void shouldRejectDecisionAfterApproval() {
        DeploymentRequest deployment = create();
        deployment.submit(SUBMITTED_AT);
        deployment.approve(UUID.randomUUID(), "Approved", DECIDED_AT);

        assertThatThrownBy(() -> deployment.approve(UUID.randomUUID(), "Approved", DECIDED_AT.plusSeconds(1)))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
    }

    @Test
    void shouldRecordRejectionAndReturnToDraft() {
        DeploymentRequest deployment = create();
        UUID reviewerId = UUID.randomUUID();
        deployment.submit(SUBMITTED_AT);

        deployment.reject(reviewerId, "Rollback plan is incomplete", DECIDED_AT);

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.DRAFT);
        ApprovalDecision decision = deployment.getReviewRounds().get(0).getDecisions().get(0);
        assertThat(decision.getReviewerId()).isEqualTo(reviewerId);
        assertThat(decision.getType()).isEqualTo(ApprovalDecisionType.REJECTED);
        assertThat(decision.getComment()).isEqualTo("Rollback plan is incomplete");
        assertThat(decision.getDecidedAt()).isEqualTo(DECIDED_AT);
    }

    @Test
    void shouldRequireValidRejectionReason() {
        DeploymentRequest deployment = create();
        deployment.submit(SUBMITTED_AT);

        assertThatThrownBy(() -> deployment.reject(UUID.randomUUID(), null, DECIDED_AT))
                .isInstanceOf(InvalidRejectionReasonException.class);
        assertThatThrownBy(() -> deployment.reject(UUID.randomUUID(), "   ", DECIDED_AT))
                .isInstanceOf(InvalidRejectionReasonException.class);
        assertThatThrownBy(() -> deployment.reject(UUID.randomUUID(), "Rejected", null))
                .isInstanceOf(InvalidDecisionException.class);
    }

    @Test
    void shouldCreateNewEmptyRoundAfterRejectionAndNotCountPreviousApprovals() {
        DeploymentRequest deployment = create(2);
        UUID firstReviewer = UUID.randomUUID();
        UUID secondReviewer = UUID.randomUUID();
        deployment.submit(SUBMITTED_AT);
        deployment.approve(firstReviewer, "Approved", DECIDED_AT);
        deployment.reject(secondReviewer, "Needs changes", DECIDED_AT.plusSeconds(1));

        deployment.edit("Updated title", "Updated description", "Updated rollback");
        deployment.submit(SUBMITTED_AT.plusSeconds(2));

        assertThat(deployment.getReviewRounds()).hasSize(2);
        assertThat(deployment.getReviewRounds().get(1).getRoundNumber()).isEqualTo(2);
        assertThat(deployment.getReviewRounds().get(1).getDecisions()).isEmpty();
        deployment.approve(firstReviewer, "Approved again", DECIDED_AT.plusSeconds(3));
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING_APPROVAL);
        deployment.approve(secondReviewer, "Approved", DECIDED_AT.plusSeconds(4));
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.APPROVED);
    }

    @Test
    void shouldAllowSameReviewerToDecideInLaterRound() {
        DeploymentRequest deployment = create(2);
        UUID reviewerId = UUID.randomUUID();
        deployment.submit(SUBMITTED_AT);
        deployment.reject(reviewerId, "Needs changes", DECIDED_AT);
        deployment.submit(SUBMITTED_AT.plusSeconds(1));

        deployment.approve(reviewerId, "Approved", DECIDED_AT.plusSeconds(2));

        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING_APPROVAL);
    }

    @Test
    void shouldCancelFromDraftPendingAndApproved() {
        DeploymentRequest draft = create();
        draft.cancel("No longer needed", CANCELED_AT);
        assertCanceled(draft);

        DeploymentRequest pending = create();
        pending.submit(SUBMITTED_AT);
        pending.cancel("Deployment window closed", CANCELED_AT);
        assertCanceled(pending);

        DeploymentRequest approved = create();
        approved.submit(SUBMITTED_AT);
        approved.approve(UUID.randomUUID(), "Approved", DECIDED_AT);
        approved.cancel("Release withdrawn", CANCELED_AT);
        assertCanceled(approved);
    }

    @Test
    void shouldRecordCancellationAndPreserveHistory() {
        DeploymentRequest deployment = create();
        deployment.submit(SUBMITTED_AT);
        deployment.cancel("Release withdrawn", CANCELED_AT);

        assertThat(deployment.getCancellationReason()).isEqualTo("Release withdrawn");
        assertThat(deployment.getCanceledAt()).isEqualTo(CANCELED_AT);
        assertThat(deployment.getReviewRounds()).hasSize(1);
    }

    @Test
    void shouldRequireValidCancellationReasonAndTime() {
        assertThatThrownBy(() -> create().cancel(null, CANCELED_AT))
                .isInstanceOf(InvalidCancellationReasonException.class);
        assertThatThrownBy(() -> create().cancel("   ", CANCELED_AT))
                .isInstanceOf(InvalidCancellationReasonException.class);
        assertThatThrownBy(() -> create().cancel("No longer needed", null))
                .isInstanceOf(InvalidDecisionException.class);
    }

    @Test
    void shouldMakeCanceledDeploymentTerminal() {
        DeploymentRequest deployment = create();
        deployment.cancel("No longer needed", CANCELED_AT);

        assertThatThrownBy(() -> deployment.edit("Updated", "Updated", "Updated"))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        assertThatThrownBy(() -> deployment.submit(SUBMITTED_AT))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        assertThatThrownBy(() -> deployment.approve(UUID.randomUUID(), "Approved", DECIDED_AT))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        assertThatThrownBy(() -> deployment.reject(UUID.randomUUID(), "Rejected", DECIDED_AT))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        assertThatThrownBy(() -> deployment.cancel("Again", CANCELED_AT))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
    }

    @Test
    void shouldExposeImmutableReviewRoundsAndDecisions() {
        DeploymentRequest deployment = create();
        deployment.submit(SUBMITTED_AT);
        deployment.approve(UUID.randomUUID(), "Approved", DECIDED_AT);
        List<ReviewRound> rounds = deployment.getReviewRounds();
        List<ApprovalDecision> decisions = rounds.get(0).getDecisions();

        assertThatThrownBy(() -> rounds.clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> decisions.clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(deployment.getReviewRounds()).hasSize(1);
        assertThat(deployment.getReviewRounds().get(0).getDecisions()).hasSize(1);
    }

    private static void assertEditRejected(DeploymentRequest deployment) {
        assertThatThrownBy(() -> deployment.edit("Updated", "Updated", "Updated"))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
    }

    private static void assertCanceled(DeploymentRequest deployment) {
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.CANCELED);
        assertThat(deployment.getCancellationReason()).isNotBlank();
        assertThat(deployment.getCanceledAt()).isEqualTo(CANCELED_AT);
    }

    private static DeploymentRequest create() {
        return create(1);
    }

    private static DeploymentRequest create(int requiredApprovals) {
        return create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), requiredApprovals);
    }

    private static DeploymentRequest create(
            UUID requesterId, UUID releaseId, UUID environmentId, int requiredApprovals
    ) {
        return DeploymentRequest.create(
                requesterId,
                releaseId,
                environmentId,
                "Release API",
                "Deploy the API release",
                "Restore the previous release",
                requiredApprovals,
                CREATED_AT
        );
    }

    private static DeploymentRequest createWithText(String title, String description, String rollbackPlan) {
        return DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                title, description, rollbackPlan, 1, CREATED_AT
        );
    }
}
