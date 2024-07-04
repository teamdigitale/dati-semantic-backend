package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Nameable;
import it.gov.innovazione.ndc.alerter.entities.Severity;
import it.gov.innovazione.ndc.alerter.event.DefaultAlertableEvent;
import it.gov.innovazione.ndc.eventhandler.NdcEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class EntityService<T extends Nameable, D extends Nameable> {

    abstract NdcEventPublisher getEventPublisher();

    abstract NameableRepository<T, String> getRepository();

    abstract EntityMapper<T, D> getEntityMapper();

    abstract String getEntityName();

    protected abstract Sort getDefaultSorting();

    public Page<D> getPaginated(Pageable pageable) {
        return getRepository().findAllBy(
                        PageRequest.of(
                                pageable.getPageNumber(),
                                pageable.getPageSize(),
                                pageable.getSortOr(
                                        getDefaultSorting())))
                .map(this::toDto);
    }

    private D toDto(T entity) {
        return getEntityMapper().toDto(entity);
    }

    protected void assertEntityDoesNotExists(D dto) {
        getRepository().findByName(dto.getName())
                .ifPresent(tr -> {
                    throw new ConflictingOperationException("An " + getEntityName() + " with the same name already exists: " + dto.getName());
                });
    }

    public D create(D dto) {
        if (dto.getId() != null) {
            throw new ConflictingOperationException(getEntityName() + " id must be null for create operation, use update instead: " + dto.getId());
        }
        assertEntityDoesNotExists(dto);
        D returnable = toDto(getRepository().save(getEntityMapper().toEntity(dto)));
        afterCreate(returnable);
        return returnable;
    }

    protected void afterCreate(D returnable) {
        getEventPublisher().publishAlertableEvent(
                getEntityName() + " created",
                DefaultAlertableEvent.builder()
                        .name(getEntityName() + " created")
                        .description(getEntityName() + " " + returnable + " created")
                        .severity(Severity.INFO)
                        .category(EventCategory.APPLICATION)
                        .context(Map.of("entity", returnable))
                        .build()
        );
    }

    public D update(D dto) {
        if (dto.getId() == null) {
            throw new ConflictingOperationException(getEntityName() + " id must not be null for update operation");
        }
        assertExistsById(dto.getId());
        beforeUpdate(dto);
        D returnable = toDto(getRepository().save(getEntityMapper().toEntity(dto)));
        afterUpdate(returnable);
        return returnable;
    }

    protected void afterUpdate(D returnable) {
        getEventPublisher().publishAlertableEvent(
                getEntityName() + " updated",
                DefaultAlertableEvent.builder()
                        .name(getEntityName() + " updated")
                        .description(getEntityName() + " " + returnable + " updated")
                        .severity(Severity.INFO)
                        .category(EventCategory.APPLICATION)
                        .context(Map.of("entity", returnable))
                        .build()
        );
    }

    protected void beforeUpdate(D dto) {

    }

    private void assertExistsById(String id) {
        if (!getRepository().existsById(id)) {
            throw entityDoesNotExistsException();
        }
    }

    public D delete(String id) {
        T entity = getEntityById(id);
        beforeDelete(entity);
        D dto = toDto(entity);
        getRepository().delete(entity);
        afterDelete(dto);
        return dto;
    }

    protected void afterDelete(D dto) {
        getEventPublisher().publishAlertableEvent(
                getEntityName() + " deleted",
                DefaultAlertableEvent.builder()
                        .name(getEntityName() + " deleted")
                        .description(getEntityName() + " " + dto + " deleted")
                        .severity(Severity.INFO)
                        .category(EventCategory.APPLICATION)
                        .context(Map.of("entity", dto))
                        .build()
        );
    }

    protected void beforeDelete(T entity) {

    }

    public D getById(String id) {
        return toDto(getEntityById(id));
    }

    private T getEntityById(String id) {
        return getRepository().findById(id)
                .orElseThrow(this::entityDoesNotExistsException);
    }

    public T getEntityByName(String name) {
        return getRepository().findByName(name)
                .orElseThrow(this::entityDoesNotExistsException);
    }

    private ConflictingOperationException entityDoesNotExistsException() {
        return new ConflictingOperationException(getEntityName() + " does not exist");
    }

    @Transactional(readOnly = true)
    public List<D> findAll() {
        return getRepository().findAll().stream()
                .map(a -> getEntityMapper().toDto(a))
                .collect(Collectors.toList());
    }

    @Slf4j
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ConflictingOperationException extends RuntimeException {

        public ConflictingOperationException(String message) {
            super(message);
            log.error(message);
        }
    }
}
