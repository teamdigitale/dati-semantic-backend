package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.NameableEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class EntityService<T extends NameableEntity> {

    abstract NameableRepository<T, String> getRepository();

    public List<T> getAll() {
        return getRepository().findAll();
    }

    public T create(T entity) {
        if (entity.getId() != null) {
            throw new IllegalArgumentException("Entity id must be null for create operation");
        }

        Optional<T> entityIfExists = getRepository().findByName(entity.getName());
        if (entityIfExists.isPresent()) {
            throw new IllegalArgumentException("Entity already exists");
        }

        return getRepository().save(entity);
    }

    public T update(T entity) {
        Optional<T> entityIfExists = getRepository().findById(entity.getId());
        if (entityIfExists.isEmpty()) {
            throw new IllegalArgumentException("Entity does not exist");
        }
        return getRepository().save(entity);

    }

    public T delete(String id) {
        Optional<T> entityIfExists = getRepository().findById(id);
        if (entityIfExists.isEmpty()) {
            throw new IllegalArgumentException("Entity does not exist");
        }
        getRepository().delete(entityIfExists.get());
        return entityIfExists.get();
    }

    public T getById(String id) {
        Optional<T> entityIfExists = getRepository().findById(id);
        if (entityIfExists.isEmpty()) {
            throw new IllegalArgumentException("Entity does not exist");
        }
        return entityIfExists.get();
    }
}
