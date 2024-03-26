package it.gov.innovazione.ndc.controller;

import it.gov.innovazione.ndc.harvester.HarvesterService;
import it.gov.innovazione.ndc.harvester.service.RepositoryService;
import it.gov.innovazione.ndc.model.harvester.Repository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/config/repository")
@Slf4j
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final HarvesterService harvesterService;

    @GetMapping
    public List<Repository> getAllRepositories() {
        return repositoryService.getActiveRepos();
    }

    @PostMapping
    @ResponseStatus(CREATED)
    @SneakyThrows
    public void createRepository(
            @RequestBody CreateRepository repository,
            Principal principal) {
        assertValidUrl(repository);
        repositoryService.createRepo(
                repository.getUrl(),
                repository.getName(),
                repository.getDescription(),
                repository.getMaxFileSizeBytes(),
                principal);
    }

    private void assertValidUrl(@RequestBody CreateRepository repository) throws BadRequestException {
        try {
            new URL(repository.getUrl());
        } catch (Exception e) {
            log.error("Invalid URL", e);
            throw new BadRequestException("Invalid URL", e);
        }
        if (StringUtils.isEmpty(repository.getName())) {
            log.error("Name cannot be empty");
            throw new BadRequestException("Name cannot be empty");
        }
    }

    @PatchMapping("/{id}")
    @SneakyThrows
    public ResponseEntity<Void> updateRepository(
            @PathVariable String id,
            @RequestBody CreateRepository repository,
            Principal principal) {
        assertValidUrl(repository);
        int updated = repositoryService.updateRepo(id, repository, principal);

        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @SneakyThrows
    public ResponseEntity<?> deleteRepository(
            @PathVariable String id,
            Principal principal) {
        Optional<Repository> optionalRepository = repositoryService.getActiveRepos().stream()
                .filter(repo -> repo.getId().equals(id))
                .findFirst();

        if (optionalRepository.isEmpty()) {
            log.warn("Repository {} not found or not active", id);
            return ResponseEntity.notFound().build();
        }

        Repository repository = optionalRepository.get();

        harvesterService.clear(repository.getUrl());

        int deleted = repositoryService.delete(id, principal);

        if (deleted == 0) {
            log.warn("Repository {} not found", id);
            return ResponseEntity.notFound().build();
        }

        log.info("Repository {} deleted", repository);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateRepository {
        private String url;
        private String name;
        private String description;
        private Long maxFileSizeBytes;
    }
}
