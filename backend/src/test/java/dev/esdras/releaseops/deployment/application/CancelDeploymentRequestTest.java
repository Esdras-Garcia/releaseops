package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.CancelDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.exception.DeploymentRequestNotFoundException;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelDeploymentRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-01T10:05:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldCancelUsingClockAndSaveTheRequest() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        DeploymentRequest result = new CancelDeploymentRequest(repository, CLOCK).execute(
                new CancelDeploymentRequestCommand(request.getId(), "No longer needed"));

        assertThat(result).isSameAs(request);
        assertThat(request.getCanceledAt()).isEqualTo(NOW);
        verify(repository).save(request);
    }

    @Test
    void shouldThrowWhenRequestDoesNotExistAndNotSave() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CancelDeploymentRequest(repository, CLOCK).execute(
                new CancelDeploymentRequestCommand(id, "No longer needed")))
                .isInstanceOf(DeploymentRequestNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectSecondCancellationAndNotSave() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        request.cancel("First cancellation", NOW);
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new CancelDeploymentRequest(repository, CLOCK).execute(
                new CancelDeploymentRequestCommand(request.getId(), "Again")))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        verify(repository, never()).save(any());
    }

    private static DeploymentRequest request() {
        return DeploymentRequest.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Description", "Rollback", 1, CREATED_AT);
    }
}
