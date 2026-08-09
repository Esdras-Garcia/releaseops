package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.ApproveDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.command.CancelDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.command.CreateDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.command.RejectDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.command.UpdateDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.application.exception.DeploymentRequestNotFoundException;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.DeploymentStatus;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentRequestException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentTransitionException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidCancellationReasonException;
import dev.esdras.releaseops.deployment.domain.exception.InvalidRejectionReasonException;
import dev.esdras.releaseops.deployment.domain.exception.SelfApprovalNotAllowedException;
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

class DeploymentRequestUseCasesTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:00:00Z");
    private static final Instant NOW = Instant.parse("2026-08-01T10:05:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldCreateAndSaveDraftUsingClockInstant() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        CreateDeploymentRequestCommand command = new CreateDeploymentRequestCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Release API", "Deploy API", "Restore previous", 1
        );

        DeploymentRequest result = new CreateDeploymentRequest(repository, CLOCK).execute(command);

        assertThat(result.getStatus()).isEqualTo(DeploymentStatus.DRAFT);
        assertThat(result.getCreatedAt()).isEqualTo(NOW);
        verify(repository).save(result);
    }

    @Test
    void shouldNotSaveWhenCreationIsInvalid() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        CreateDeploymentRequestCommand command = new CreateDeploymentRequestCommand(
                null, UUID.randomUUID(), UUID.randomUUID(), "Title", "Description", "Rollback", 1
        );

        assertThatThrownBy(() -> new CreateDeploymentRequest(repository, CLOCK).execute(command))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        verify(repository, never()).save(any());
    }

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
    void shouldNotSaveWhenUpdateIsInvalidOrRequestDoesNotExist() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new UpdateDeploymentRequest(repository).execute(
                new UpdateDeploymentRequestCommand(id, "Title", "Description", "Rollback")))
                .isInstanceOf(DeploymentRequestNotFoundException.class)
                .hasMessage("Deployment request not found: " + id);
        verify(repository, never()).save(any());

        DeploymentRequest submitted = request();
        submitted.submit(NOW);
        when(repository.findById(submitted.getId())).thenReturn(Optional.of(submitted));
        assertThatThrownBy(() -> new UpdateDeploymentRequest(repository).execute(
                new UpdateDeploymentRequestCommand(submitted.getId(), "Title", "Description", "Rollback")))
                .isInstanceOf(InvalidDeploymentTransitionException.class);
        verify(repository, never()).save(submitted);
    }

    @Test
    void shouldApproveWithClockInstantAndReturnRequest() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        request.submit(NOW);
        UUID reviewer = UUID.randomUUID();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        DeploymentRequest result = new ApproveDeploymentRequest(repository, CLOCK).execute(
                new ApproveDeploymentRequestCommand(request.getId(), reviewer, "Looks good"));

        assertThat(result).isSameAs(request);
        assertThat(request.getReviewRounds().getFirst().getDecisions().getFirst().getDecidedAt()).isEqualTo(NOW);
        verify(repository).save(request);
    }

    @Test
    void shouldRejectSelfApprovalAndNotSave() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        UUID requester = UUID.randomUUID();
        DeploymentRequest request = DeploymentRequest.create(
                requester, UUID.randomUUID(), UUID.randomUUID(), "Title", "Description", "Rollback", 1, CREATED_AT);
        request.submit(NOW);
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new ApproveDeploymentRequest(repository, CLOCK).execute(
                new ApproveDeploymentRequestCommand(request.getId(), requester, "Self")))
                .isInstanceOf(SelfApprovalNotAllowedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectAndReturnRequestToDraft() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        request.submit(NOW);
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        DeploymentRequest result = new RejectDeploymentRequest(repository, CLOCK).execute(
                new RejectDeploymentRequestCommand(request.getId(), UUID.randomUUID(), "Needs changes"));

        assertThat(result).isSameAs(request);
        assertThat(request.getStatus()).isEqualTo(DeploymentStatus.DRAFT);
        assertThat(request.getReviewRounds().getFirst().getDecisions().getFirst().getDecidedAt()).isEqualTo(NOW);
        verify(repository).save(request);
    }

    @Test
    void shouldNotSaveWhenRejectionIsInvalid() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        request.submit(NOW);
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new RejectDeploymentRequest(repository, CLOCK).execute(
                new RejectDeploymentRequestCommand(request.getId(), UUID.randomUUID(), "   ")))
                .isInstanceOf(InvalidRejectionReasonException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldCancelUsingClockInstant() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        DeploymentRequest result = new CancelDeploymentRequest(repository, CLOCK).execute(
                new CancelDeploymentRequestCommand(request.getId(), "No longer needed"));

        assertThat(result).isSameAs(request);
        assertThat(request.getStatus()).isEqualTo(DeploymentStatus.CANCELED);
        assertThat(request.getCanceledAt()).isEqualTo(NOW);
        verify(repository).save(request);
    }

    @Test
    void shouldNotSaveWhenCancellationIsInvalid() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> new CancelDeploymentRequest(repository, CLOCK).execute(
                new CancelDeploymentRequestCommand(request.getId(), "   ")))
                .isInstanceOf(InvalidCancellationReasonException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldGetRequestWithoutSaving() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        DeploymentRequest request = request();
        when(repository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThat(new GetDeploymentRequest(repository).execute(request.getId())).isSameAs(request);

        verify(repository, never()).save(any());
    }

    private static DeploymentRequest request() {
        return DeploymentRequest.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Title", "Description", "Rollback", 1, CREATED_AT);
    }
}
