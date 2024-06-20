package it.gov.innovazione.ndc.alerter.controller;

import it.gov.innovazione.ndc.alerter.data.ProfileMapper;
import it.gov.innovazione.ndc.alerter.data.ProfileService;
import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.entities.Profile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileController extends AbstractCrudController<Profile, ProfileDto> {

    @Getter(AccessLevel.PROTECTED)
    private final ProfileService entityService;

    @Getter(AccessLevel.PROTECTED)
    private final ProfileMapper entityMapper;
}
