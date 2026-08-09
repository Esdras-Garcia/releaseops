package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.UpdateDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.exception.DeploymentRequestNotFoundException;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentRequestException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
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

class UpdateDeploymentRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant SUBMITTED_AT = Instant.parse("2026-08-01T10:05:00Z");

    @Test
    void shouldEditAndReturnTheUpdatedRequest() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        DeploymentRequest result = new UpdateDeploymentRequest(repository).execute(
                new UpdateDeploymentRequestCommand(request.getId(), "New title", "New description", "New rollback"));

        assertThat(result).isSameAs(request);
        assertThat(request.getTitle()).isEqualTo("New title");
        verify(repository).save(request);
    }

    @Test
    void shouldThrowWhenRequestDoesNotExistAndNotSave() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UpdateDeploymentRequest(repository).execute(
                new UpdateDeploymentRequestCommand(id, "Title", "Description", "Rollback")))
                .isInstanceOf(DeploymentRequestNotFoundException.class)
                .hasMessage("Deployment request not found: " + id);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldPropagateInvalidStateAndNotSave() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        request.submit(SUBMITTED_AT);
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new UpdateDeploymentRequest(repository).execute(
                new UpdateDeploymentRequestCommand(request.getId(), "Title", "Description", "Rollback")))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldPropagateInvalidTextAndNotSave() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new UpdateDeploymentRequest(repository).execute(
                new UpdateDeploymentRequestCommand(request.getId(), "   ", "Description", "Rollback")))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        verify(repository, never()).save(any());
    }

    private static DeploymentRequest request() {
        return DeploymentRequest.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Description", "Rollback", 1, CREATED_AT);
    }
}
