package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.UserDto;
import it.gov.innovazione.ndc.alerter.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class UserMapper implements EntityMapper<User, UserDto> {

    protected ProfileService profileService;

    @Autowired
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Override
    @Mapping(target = "profile", source = "profile.name")
    public abstract UserDto toDto(User entity);

    @Override
    @Mapping(target = "profile", expression = "java(profileService.getByName(dto.getProfile()))")
    public abstract User toEntity(UserDto dto);
}
