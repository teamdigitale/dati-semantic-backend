package it.gov.innovazione.ndc.alerter.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements NameableEntity {
    @Id
    private String id;
    private String name;
    private String surname;
    private String email;
    @ManyToOne
    private Profile profile;
}
