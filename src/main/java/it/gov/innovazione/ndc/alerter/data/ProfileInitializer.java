package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Profile;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileInitializer implements Initializer {

    private static final List<Pair<String, List<EventCategory>>> DEFAULT_PROFILES = List.of(
            Pair.of("Content Manager", List.of(EventCategory.SEMANTIC)),
            Pair.of("Application Manager", List.of(EventCategory.APPLICATION)),
            Pair.of("Infrastructure Manager", List.of(EventCategory.INFRASTRUCTURE)),
            Pair.of("Administrator", List.of(EventCategory.SEMANTIC, EventCategory.APPLICATION, EventCategory.INFRASTRUCTURE)));

    private final ProfileRepository repository;

    @Override
    public void init() {
        List<String> existingProfileNames = repository.findAll().stream()
                .map(Profile::getName)
                .collect(Collectors.toList());
        DEFAULT_PROFILES.stream()
                .filter(not(p -> existingProfileNames.contains(p.getLeft())))
                .map(pair -> Profile.builder()
                        .name(pair.getLeft())
                        .eventCategories(pair.getRight())
                        .minSeverity(Severity.INFO)
                        .aggregationTime(60L)
                        .build())
                .forEach(p -> {
                    log.info("Creating default profile: {}", p.getName());
                    repository.save(p);
                });

    }
}
