package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.User;
import org.springframework.stereotype.Repository;

@Repository
interface UserRepository extends NameableRepository<User, String> {

    boolean existsByNameAndSurnameAndEmail(String name, String surname, String email);
}
