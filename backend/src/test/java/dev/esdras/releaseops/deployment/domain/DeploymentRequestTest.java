package dev.esdras.releaseops.deployment.domain;

import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;
import org.junit.jupiter.api.Test;

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
                environmentId
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
                environmentId
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
                environmentId
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
                environmentId
        );

        deployment.submit();

        assertThatThrownBy(() -> deployment.approve(requesterId))
                .isInstanceOf(SelfApprovalNotAllowedException.class)
                .hasMessage("Requester cannot approve their own deployment");
    }
}
