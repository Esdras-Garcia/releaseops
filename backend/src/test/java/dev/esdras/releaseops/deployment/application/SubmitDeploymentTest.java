package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.DeploymentStatus;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.application.exception.DeploymentNotFoundException;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitDeploymentTest {

    @Test
    void shouldSubmitExistingDeployment() {
        UUID deploymentId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Release API",
                "Deploy the API release",
                "Restore the previous release",
                1,
                Instant.parse("2026-08-01T10:00:00Z")
        );

        DeploymentRepository repository = mock(DeploymentRepository.class);

        when(repository.findById(deploymentId))
                .thenReturn(Optional.of(deployment));

        SubmitDeployment useCase = new SubmitDeployment(repository);

        useCase.execute(deploymentId);

        assertThat(deployment.getStatus())
                .isEqualTo(DeploymentStatus.PENDING_APPROVAL);

        verify(repository).save(deployment);
    }

    @Test
    void shouldFailWhenDeploymentDoesNotExist() {
        UUID deploymentId = UUID.randomUUID();
        DeploymentRepository repository = mock(DeploymentRepository.class);
        when(repository.findById(deploymentId)).thenReturn(Optional.empty());

        SubmitDeployment useCase = new SubmitDeployment(repository);

        assertThatThrownBy(() -> useCase.execute(deploymentId))
                .isInstanceOf(DeploymentNotFoundException.class)
                .hasMessage("Deployment not found: " + deploymentId);
        verify(repository, never()).save(any());
    }
}
