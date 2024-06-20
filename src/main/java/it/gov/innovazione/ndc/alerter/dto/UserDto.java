package it.gov.innovazione.ndc.alerter.dto;

import it.gov.innovazione.ndc.alerter.entities.Nameable;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

@Data
public class UserDto implements Nameable {
    private String id;
    @NotBlank(message = "Name is mandatory")
    private String name;
    @NotBlank(message = "Surname is mandatory")
    private String surname;
    @NotBlank(message = "Email is mandatory")
    private String email;
    @NotEmpty(message = "Profile is mandatory")
    private String profile;
}
