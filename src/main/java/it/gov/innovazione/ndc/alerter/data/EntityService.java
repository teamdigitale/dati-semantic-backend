package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Nameable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequiredArgsConstructor
public abstract class EntityService<T extends Nameable, D extends Nameable> {

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
        return toDto(getRepository().save(getEntityMapper().toEntity(dto)));
    }

    public D update(D dto) {
        if (dto.getId() == null) {
            throw new ConflictingOperationException(getEntityName() + " id must not be null for update operation");
        }
        assertExistsById(dto.getId());
        return toDto(getRepository().save(getEntityMapper().toEntity(dto)));
    }

    private void assertExistsById(String id) {
        if (!getRepository().existsById(id)) {
            throw entityDoesNotExistsException();
        }
    }

    public D delete(String id) {
        T entity = getEntityById(id);
        D dto = toDto(entity);
        getRepository().delete(entity);
        return dto;
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

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ConflictingOperationException extends RuntimeException {

        public ConflictingOperationException(String message) {
            super(message);
        }
    }
}
