package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.entities.Profile;
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
    private final ProfileRepository repository;

    @Getter(AccessLevel.PROTECTED)
    private final ProfileMapper entityMapper;

    @Getter(AccessLevel.PROTECTED)
    private final String entityName = "Profile";

    @Getter(AccessLevel.PROTECTED)
    private final Sort defaultSorting = Sort.by("name").ascending();

    public void setLastUpdated(String id) {
        Profile profile = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Profile not found: " + id));
        profile.setLastAlertedAt(Instant.now());
        repository.save(profile);
    }

    public void setAllLastUpdated(Duration backoff) {
        repository.findAll().forEach(profile -> {
            profile.setLastAlertedAt(Instant.now().minus(backoff));
            repository.save(profile);
        });
    }
}
