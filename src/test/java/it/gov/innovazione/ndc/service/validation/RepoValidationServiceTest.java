package it.gov.innovazione.ndc.service.validation;

import it.gov.innovazione.ndc.harvester.AgencyRepositoryService;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetModelFactory;
import it.gov.innovazione.ndc.harvester.model.SemanticAssetPath;
import it.gov.innovazione.ndc.harvester.service.RepositoryStructureValidator;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidationResult;
import it.gov.innovazione.ndc.harvester.validation.RdfSyntaxValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepoValidationServiceTest {

    @Mock
    private AgencyRepositoryService agencyRepositoryService;
    @Mock
    private RdfSyntaxValidator rdfSyntaxValidator;
    @Mock
    private RepositoryStructureValidator repositoryStructureValidator;
    @Mock
    private SemanticAssetModelFactory modelFactory;

    private RepoValidationService service;

    @BeforeEach
    void setUp() {
        service = new RepoValidationService(
                agencyRepositoryService, rdfSyntaxValidator,
                repositoryStructureValidator, modelFactory);
    }

    @Test
    void shouldAcquireAndReleaseSemaphore() {
        assertThat(service.tryAcquire()).isTrue();
        assertThat(service.tryAcquire()).isTrue();
        assertThat(service.tryAcquire()).isTrue();
        assertThat(service.tryAcquire()).isFalse();
    }

    @Test
    void shouldCompleteValidationSuccessfully() throws IOException {
        Path clonedPath = Path.of("/tmp/test-repo");
        ValidationJob job = createJob();

        when(agencyRepositoryService.cloneRepoWithoutTracking(any(), any()))
                .thenReturn(clonedPath);
        when(agencyRepositoryService.getOntologyPaths(clonedPath))
                .thenReturn(Collections.emptyList());
        when(agencyRepositoryService.getControlledVocabularyPaths(clonedPath))
                .thenReturn(Collections.emptyList());
        when(agencyRepositoryService.getSchemaPaths(clonedPath))
                .thenReturn(Collections.emptyList());
        when(repositoryStructureValidator.validate(clonedPath))
                .thenReturn(Optional.empty());

        service.tryAcquire();
        service.executeValidation(job);

        assertThat(job.getStatus()).isEqualTo(ValidationJob.Status.COMPLETED);
        assertThat(job.getReport()).isNotNull();
        assertThat(job.getCompletedAt()).isNotNull();
        verify(agencyRepositoryService).removeClonedRepo(clonedPath);
    }

    @Test
    void shouldHandleCloneFailure() throws IOException {
        ValidationJob job = createJob();

        when(agencyRepositoryService.cloneRepoWithoutTracking(any(), any()))
                .thenThrow(new IOException("Clone failed"));

        service.tryAcquire();
        service.executeValidation(job);

        assertThat(job.getStatus()).isEqualTo(ValidationJob.Status.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Clone failed");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void shouldValidateDiscoveredAssets() throws IOException {
        Path clonedPath = Path.of("/tmp/test-repo");
        ValidationJob job = createJob();

        SemanticAssetPath ontologyPath = new SemanticAssetPath(
                clonedPath.resolve("ontologies/test.ttl").toString());

        when(agencyRepositoryService.cloneRepoWithoutTracking(any(), any()))
                .thenReturn(clonedPath);
        when(agencyRepositoryService.getOntologyPaths(clonedPath))
                .thenReturn(Collections.singletonList(ontologyPath));
        when(agencyRepositoryService.getControlledVocabularyPaths(clonedPath))
                .thenReturn(Collections.emptyList());
        when(agencyRepositoryService.getSchemaPaths(clonedPath))
                .thenReturn(Collections.emptyList());
        when(repositoryStructureValidator.validate(clonedPath))
                .thenReturn(Optional.empty());
        when(rdfSyntaxValidator.validateTurtle(any()))
                .thenReturn(RdfSyntaxValidationResult.builder().build());

        service.tryAcquire();
        service.executeValidation(job);

        assertThat(job.getStatus()).isEqualTo(ValidationJob.Status.COMPLETED);
        assertThat(job.getReport().getAssetChecks()).hasSize(1);
        assertThat(job.getTotalAssets()).isEqualTo(1);
    }

    private static ValidationJob createJob() {
        return ValidationJob.builder()
                .id("test-id")
                .owner("owner")
                .repo("repo")
                .createdAt(Instant.now())
                .status(ValidationJob.Status.PENDING)
                .build();
    }
}
