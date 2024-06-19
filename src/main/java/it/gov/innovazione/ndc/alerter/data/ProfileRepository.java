package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Profile;
import org.springframework.stereotype.Repository;

@Repository
interface ProfileRepository extends NameableRepository<Profile, String> {

}
