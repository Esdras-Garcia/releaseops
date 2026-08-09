package dev.esdras.releaseops.deployment.application;

import dev.esdras.releaseops.deployment.application.command.CreateDeploymentRequestCommand;
import dev.esdras.releaseops.deployment.domain.DeploymentRepository;
import dev.esdras.releaseops.deployment.domain.DeploymentRequest;
import dev.esdras.releaseops.deployment.domain.DeploymentStatus;
import dev.esdras.releaseops.deployment.domain.exception.InvalidDeploymentRequestException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CreateDeploymentRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T10:05:00Z");
    private static final Clock CLOCK = Clock.fixed(CREATED_AT, ZoneOffset.UTC);

    @Test
    void shouldCreateDraftUsingClockAndSaveIt() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        CreateDeploymentRequestCommand command = new CreateDeploymentRequestCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Release API", "Deploy API", "Restore previous", 1);

        DeploymentRequest result = new CreateDeploymentRequest(repository, CLOCK).execute(command);

        assertThat(result.getStatus()).isEqualTo(DeploymentStatus.DRAFT);
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
        verify(repository).save(result);
    }

    @Test
    void shouldPropagateDomainExceptionAndNotSaveWhenCreationIsInvalid() {
        DeploymentRepository repository = mock(DeploymentRepository.class);
        CreateDeploymentRequestCommand command = new CreateDeploymentRequestCommand(
                null, UUID.randomUUID(), UUID.randomUUID(), "Title", "Description", "Rollback", 1);

        assertThatThrownBy(() -> new CreateDeploymentRequest(repository, CLOCK).execute(command))
                .isInstanceOf(InvalidDeploymentRequestException.class);
        verify(repository, never()).save(any());
    }
}
