package it.gov.innovazione.ndc.alerter.controller;


import it.gov.innovazione.ndc.alerter.data.EntityMapper;
import it.gov.innovazione.ndc.alerter.data.EntityService;
import it.gov.innovazione.ndc.alerter.entities.Nameable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCrudController<T extends Nameable, D extends Nameable> {

    abstract EntityService<T> getEntityService();

    abstract EntityMapper<T, D> getEntityMapper();

    @GetMapping
    public List<D> getAll() {
        return getEntityMapper().toDto(getEntityService().getAll());
    }

    @GetMapping("{id}")
    public D getOne(@PathVariable String id) {
        return getEntityMapper().toDto(getEntityService().getById(id));
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public void create(@Valid @RequestBody D dto) {
        handlePreCreate(dto);
        T createdEntity = getEntityService().create(getEntityMapper().toEntity(dto));
        handlePostCreate(createdEntity);
    }

    protected void handlePreCreate(D dto) {
        log.info("Creating entity from dto: {}", dto);
    }

    protected void handlePostCreate(T createdEntity) {
        log.info("Created entity: {}", createdEntity);
    }

    @PatchMapping
    @ResponseStatus(CREATED)
    public void update(@Valid @RequestBody D dto) {
        handlePreUpdate(dto);
        T updatedEntity = getEntityService().update(getEntityMapper().toEntity(dto));
        handlePostUpdate(updatedEntity);
    }

    protected void handlePreUpdate(D dto) {
        log.info("Updating entity from dto: {}", dto);
    }

    protected void handlePostUpdate(T updatedEntity) {
        log.info("Updated entity: {}", updatedEntity);
    }

    @DeleteMapping("{id}")
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
