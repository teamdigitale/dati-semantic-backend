package it.gov.innovazione.ndc.alerter.controller;

import it.gov.innovazione.ndc.alerter.data.UserMapper;
import it.gov.innovazione.ndc.alerter.data.UserService;
import it.gov.innovazione.ndc.alerter.dto.UserDto;
import it.gov.innovazione.ndc.alerter.entities.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("user")
public class UserController extends AbstractCrudController<User, UserDto> {

    @Getter(AccessLevel.PROTECTED)
    private final UserService entityService;
    @Getter(AccessLevel.PROTECTED)
    private final UserMapper entityMapper;

}
