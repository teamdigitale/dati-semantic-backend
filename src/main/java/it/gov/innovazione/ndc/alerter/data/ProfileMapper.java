package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.entities.Profile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ProfileMapper implements EntityMapper<Profile, ProfileDto> {

}
