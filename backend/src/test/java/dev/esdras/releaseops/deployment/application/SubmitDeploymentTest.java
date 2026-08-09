package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.DeploymentStatus;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitDeploymentTest {

    @Test
    void shouldSubmitExistingDeployment() {
        UUID deploymentId = UUID.randomUUID();

        DeploymentRequest deployment = DeploymentRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
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
}
