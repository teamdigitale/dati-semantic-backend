package it.gov.innovazione.ndc.alerter.controller;


import it.gov.innovazione.ndc.alerter.data.EntityService;
import it.gov.innovazione.ndc.alerter.entities.NameableEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCrudController<T extends NameableEntity> {

    abstract EntityService<T> getEntityService();

    @GetMapping
    public List<T> getAll() {
        return getEntityService().getAll();
    }

    @GetMapping("/{id}")
    public T getOne(@PathVariable String id) {
        return getEntityService().getById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public void create(T entity) {
        handlePreCreate(entity);
        T createdEntity = getEntityService().create(entity);
        handlePostCreate(createdEntity);
    }

    protected void handlePreCreate(T entity) {
        log.info("Creating entity: {}", entity);
    }

    protected void handlePostCreate(T createdEntity) {
        log.info("Created entity: {}", createdEntity);
    }

    @PatchMapping
    @ResponseStatus(CREATED)
    public void update(T entity) {
        handlePreUpdate(entity);
        T updatedEntity = getEntityService().update(entity);
        handlePostUpdate(updatedEntity);
    }

    protected void handlePreUpdate(T entity) {
        log.info("Updating entity: {}", entity);
    }

    protected void handlePostUpdate(T updatedEntity) {
        log.info("Updated entity: {}", updatedEntity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(CREATED)
    public void delete(@PathVariable String id) {
        handlePreDelete(id);
        T deletedEntity = getEntityService().delete(id);
        handlePostDelete(deletedEntity);
    }

    protected void handlePreDelete(String id) {
        log.info("Deleting entity with id: {}", id);
    }

    protected void handlePostDelete(T deletedEntity) {
        log.info("Deleted entity: {}", deletedEntity);
    }
}
