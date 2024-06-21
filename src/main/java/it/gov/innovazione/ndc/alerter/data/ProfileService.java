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
}
