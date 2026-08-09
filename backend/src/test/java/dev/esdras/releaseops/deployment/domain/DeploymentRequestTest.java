package dev.esdras.releaseops.deployment.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
