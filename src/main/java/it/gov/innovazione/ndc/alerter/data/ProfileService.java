package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.entities.Profile;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProfileService extends EntityService<Profile, ProfileDto> {

    @Getter(AccessLevel.PROTECTED)
    private final NdcEventPublisher eventPublisher;

    @Getter(AccessLevel.PROTECTED)
    private final ProfileRepository repository;

    private final ProfileInitializer profileInitializer;

    @Getter(AccessLevel.PROTECTED)
    private final ProfileMapper entityMapper;

    @Getter(AccessLevel.PROTECTED)
    private final String entityName = "Profile";

    @Getter(AccessLevel.PROTECTED)
    private final Sort defaultSorting = Sort.by("name").ascending();

    public void setLastAlertedAt(String id, Instant instant) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Profile not found: " + id));
        profile.setLastAlertedAt(instant);
        repository.save(profile);
    }

    public void setAllLastAlertedAt(Duration backoff) {
        repository.findAll().forEach(profile -> {
            Instant lastAlertedAt = Instant.now().minus(backoff);
            log.info("Setting last updated {} for profile {}", lastAlertedAt, profile.getName());
            profile.setLastAlertedAt(lastAlertedAt);
            repository.save(profile);
        });
    }

    @Override
    public void beforeDelete(Profile profile) {
        if (profileInitializer.isProfileNameUnmodifiable(profile.getName())) {
            throw new ConflictingOperationException("Profile " + profile.getName() + " cannot be deleted");
        }
    }

    @Override
    public void beforeUpdate(ProfileDto dto) {
        Profile profile = repository.findById(dto.getId())
                .orElseThrow(() -> new IllegalStateException("Profile not found: " + dto.getId()));
        if (profileInitializer.isProfileNameUnmodifiable(dto.getName()) && !profile.getName().equals(dto.getName())) {
            throw new ConflictingOperationException("Profile " + profile.getName() + " cannot be renamed");
        }
    }
}
