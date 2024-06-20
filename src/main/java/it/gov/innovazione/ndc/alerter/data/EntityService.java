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
        Optional<T> entityIfExists = getRepository().findById(entity.getId());
        if (entityIfExists.isEmpty()) {
            throw new ConflictingOperationException(getEntityName() + " does not exist");
        }
        return getRepository().save(entity);

    }

    public T delete(String id) {
        Optional<T> entityIfExists = getRepository().findById(id);
        if (entityIfExists.isEmpty()) {
            throw new ConflictingOperationException(getEntityName() + " does not exist");
        }
        getRepository().delete(entityIfExists.get());
        return entityIfExists.get();
    }

    public T getById(String id) {
        Optional<T> entityIfExists = getRepository().findById(id);
        if (entityIfExists.isEmpty()) {
            throw new ConflictingOperationException(getEntityName() + " does not exist");
        }
        return entityIfExists.get();
    }

    public T getByName(String name) {
        Optional<T> entityIfExists = getRepository().findByName(name);
        if (entityIfExists.isEmpty()) {
            throw new ConflictingOperationException(getEntityName() + " does not exist");
        }
        return entityIfExists.get();
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class ConflictingOperationException extends RuntimeException {

        public ConflictingOperationException(String message) {
            super(message);
        }
    }
}
