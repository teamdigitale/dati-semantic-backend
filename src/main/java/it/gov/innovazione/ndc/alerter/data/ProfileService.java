package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Profile;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProfileService extends EntityService<Profile, ProfileDto> {

    private static final List<Pair<String, List<EventCategory>>> DEFAULT_PROFILES = List.of(
            Pair.of("Content Manager", List.of(EventCategory.SEMANTIC)),
            Pair.of("Application Manager", List.of(EventCategory.APPLICATION)),
            Pair.of("Infrastructure Manager", List.of(EventCategory.INFRASTRUCTURE)),
            Pair.of("Administrator", List.of(EventCategory.SEMANTIC, EventCategory.APPLICATION, EventCategory.INFRASTRUCTURE)));
    @Getter(AccessLevel.PROTECTED)
    private final ProfileRepository repository;

    @Getter(AccessLevel.PROTECTED)
    private final ProfileMapper entityMapper;

    @Getter(AccessLevel.PROTECTED)
    private final String entityName = "Profile";

    @Getter(AccessLevel.PROTECTED)
    private final Sort defaultSorting = Sort.by("name").ascending();

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        List<String> existingProfileNames = repository.findAll().stream()
                .map(Profile::getName)
                .collect(Collectors.toList());
        DEFAULT_PROFILES.stream()
                .filter(not(p -> existingProfileNames.contains(p.getLeft())))
                .map(pair -> Profile.builder()
                        .name(pair.getLeft())
                        .eventCategories(pair.getRight())
                        .minSeverity(Severity.INFO)
                        .aggregationTime(60)
                        .build())
                .forEach(repository::save);
    }
}
