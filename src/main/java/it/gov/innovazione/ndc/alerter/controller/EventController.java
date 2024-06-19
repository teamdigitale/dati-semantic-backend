package it.gov.innovazione.ndc.alerter.controller;

import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.entities.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/event")
public class EventController extends AbstractCrudController<Event> {

    private static final RuntimeException IMMUTABLE_EXCEPTION = new IllegalArgumentException("Events are immutable, so they cannot be deleted/updated");

    @Getter
    private final EventService entityService;

    @Override
    public void create(Event entity) {
        entityService.assertContextIsValid(entity.getContext());
        // todo: logic to handle the event
        super.create(entity);
    }

    @Override
    protected void handlePreUpdate(Event entity) {
        throw IMMUTABLE_EXCEPTION;
    }

    @Override
    protected void handlePreDelete(String id) {
        throw IMMUTABLE_EXCEPTION;
    }
}
