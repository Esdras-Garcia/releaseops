package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.exception.DeploymentRequestNotFoundException;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetDeploymentRequestTest {

    @Test
    void shouldReturnRequestWithoutSaving() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThat(new GetDeploymentRequest(repository).execute(request.getId())).isSameAs(request);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRequestDoesNotExistWithoutSaving() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new GetDeploymentRequest(repository).execute(id))
                .isInstanceOf(DeploymentRequestNotFoundException.class)
                .hasMessage("Deployment request not found: " + id);
        verify(repository, never()).save(any());
    }

    private static DeploymentRequest request() {
        return DeploymentRequest.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Description", "Rollback", 1, Instant.parse("2026-08-01T10:00:00Z"));
    }
}
