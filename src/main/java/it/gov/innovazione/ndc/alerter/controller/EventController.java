package it.gov.innovazione.ndc.alerter.controller;

import it.gov.innovazione.ndc.alerter.data.EventMapper;
import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.entities.Event;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/event")
public class EventController extends AbstractCrudController<Event, EventDto> {

    private static final RuntimeException IMMUTABLE_EXCEPTION = new EventService.ConflictingOperationException("Events are immutable, so they cannot be deleted/updated");

    @Getter(AccessLevel.PROTECTED)
    private final EventService entityService;
    @Getter(AccessLevel.PROTECTED)
    private final EventMapper entityMapper;

    @Override
    public EventDto create(@Valid @RequestBody EventDto entity) {
        // todo: logic to handle the event
        return super.create(entity);
    }

    @Override
    protected void handlePreUpdate(EventDto entity) {
        throw IMMUTABLE_EXCEPTION;
    }

    @Override
    protected void handlePreDelete(String id) {
        throw IMMUTABLE_EXCEPTION;
    }

}
