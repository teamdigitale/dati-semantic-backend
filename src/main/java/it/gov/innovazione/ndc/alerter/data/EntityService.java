package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Nameable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class EntityService<T extends Nameable> {

    abstract NameableRepository<T, String> getRepository();

    abstract String getEntityName();

    public List<T> getAll() {
        return getRepository().findAll();
    }

    public T create(T entity) {
        if (entity.getId() != null) {
            throw new ConflictingOperationException(getEntityName() + " id must be null for create operation");
        }

        Optional<T> entityIfExists = getRepository().findByName(entity.getName());
        if (entityIfExists.isPresent()) {
            throw new ConflictingOperationException(getEntityName() + " already exists");
        }

        return getRepository().save(entity);
    }

    public T update(T entity) {
        if (entity.getId() == null) {
            throw new ConflictingOperationException(getEntityName() + " id must not be null for update operation");
        }
        T existingEntity = getById(entity.getId());
        return getRepository().save(existingEntity);
    }

    public T delete(String id) {
        T entity = getById(id);
        getRepository().delete(entity);
        return entity;
    }

    public T getById(String id) {
        return getRepository().findById(id)
                .orElseThrow(this::entityDoesNotExistsException);
    }

    public T getByName(String name) {
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
