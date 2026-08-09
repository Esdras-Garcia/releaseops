package dev.esdras.releaseops.deployment.domain;

import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;
import dev.esdras.releaseops.deployment.domain.exception.DuplicateApprovalException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRequiredApprovalsException;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentRequestTest {

    @Test
    void shouldStartAsDraft() {
        UUID requesterId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        DeploymentRequest deploymentRequest = DeploymentRequest.create(
                requesterId,
                releaseId,
                environmentId,
                1
        );

        assertThat(deploymentRequest.getStatus()).isEqualTo(DeploymentStatus.DRAFT);
    }

    @Test
    void shouldSubmitDraftForApproval() {
        UUID requesterId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                requesterId,
                releaseId,
                environmentId,
                1
        );

        deployment.submit();

        assertThat(deployment.getStatus())
            .isEqualTo(DeploymentStatus.PENDING_APPROVAL);
    }

    @Test
    void shouldNotSubmitDeploymentThatIsNotDraft() {
        UUID requesterId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                requesterId,
                releaseId,
                environmentId,
                1
        );

        deployment.submit();

        assertThatThrownBy(() -> deployment.submit())
                .isInstanceOf(InvalidDeploymentTransitionException.class)
                .hasMessage("Only draft deployments can be submitted");
    }

    @Test
    void shouldNotAllowRequesterToApproveOwnDeployment() {
        UUID requesterId = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID environmentId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                requesterId,
                releaseId,
                environmentId,
                1
        );

        deployment.submit();

        assertThatThrownBy(() -> deployment.approve(requesterId))
                .isInstanceOf(SelfApprovalNotAllowedException.class)
                .hasMessage("Requester cannot approve their own deployment");
    }

    @Test
    void shouldRegisterApproverAndRemainPendingUntilRequiredApprovalsAreReached() {
        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2
        );
        UUID firstApproverId = UUID.randomUUID();
        UUID secondApproverId = UUID.randomUUID();

        deployment.submit();
        deployment.approve(firstApproverId);

        assertThat(deployment.getApproverIds()).containsExactly(firstApproverId);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.PENDING_APPROVAL);

        deployment.approve(secondApproverId);

        assertThat(deployment.getApproverIds()).containsExactlyInAnyOrder(firstApproverId, secondApproverId);
        assertThat(deployment.getStatus()).isEqualTo(DeploymentStatus.APPROVED);
    }

    @Test
    void shouldNotApproveDeploymentThatIsNotPending() {
        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1
        );

        assertThatThrownBy(() -> deployment.approve(UUID.randomUUID()))
                .isInstanceOf(InvalidDeploymentTransitionException.class)
                .hasMessage("Only pending deployments can be approved");
    }

    @Test
    void shouldRejectDuplicateApproval() {
        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2
        );
        UUID approverId = UUID.randomUUID();

        deployment.submit();
        deployment.approve(approverId);

        assertThatThrownBy(() -> deployment.approve(approverId))
                .isInstanceOf(DuplicateApprovalException.class)
                .hasMessage("User has already approved this deployment");
    }

    @Test
    void shouldRejectApprovalsAfterDeploymentIsApproved() {
        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1
        );

        deployment.submit();
        deployment.approve(UUID.randomUUID());

        assertThatThrownBy(() -> deployment.approve(UUID.randomUUID()))
                .isInstanceOf(InvalidDeploymentTransitionException.class)
                .hasMessage("Only pending deployments can be approved");
    }

    @Test
    void shouldRejectNonPositiveRequiredApprovals() {
        assertThatThrownBy(() -> DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0
        ))
                .isInstanceOf(InvalidRequiredApprovalsException.class)
                .hasMessage("Required approvals must be greater than zero");
    }

    @Test
    void shouldNotAllowExternalModificationOfApprovers() {
        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2
        );
        UUID approverId = UUID.randomUUID();

        deployment.submit();
        deployment.approve(approverId);
        Set<UUID> approvers = deployment.getApproverIds();

        assertThatThrownBy(() -> approvers.add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(deployment.getApproverIds()).containsExactly(approverId);
    }
}
