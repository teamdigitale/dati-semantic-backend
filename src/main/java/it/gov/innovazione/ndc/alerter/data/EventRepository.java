package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Event;
import org.springframework.stereotype.Repository;

@Repository
interface EventRepository extends NameableRepository<Event, String> {

}
