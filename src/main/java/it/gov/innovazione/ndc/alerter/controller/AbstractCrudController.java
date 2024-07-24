package it.gov.innovazione.ndc.alerter.controller;


import it.gov.innovazione.ndc.alerter.data.EntityService;
import it.gov.innovazione.ndc.alerter.dto.SlimPager;
import it.gov.innovazione.ndc.alerter.entities.Nameable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.CREATED;


@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCrudController<T extends Nameable, D extends Nameable> {

    abstract EntityService<T, D> getEntityService();

    @GetMapping
    public SlimPager<D> getPaginated(Pageable pageable) {
        return toSlimPager(getEntityService().getPaginated(pageable));
    }

    private SlimPager<D> toSlimPager(Page<D> paginated) {
        return SlimPager.of(
                paginated.getContent(),
                SlimPager.PageInfo.of(
                        paginated.getSort().get()
                                .map(order -> SlimPager.SlimOrder.of(
                                        order.getProperty(),
                                        order.getDirection()))
                                .toList(),
                        paginated.getPageable().getPageNumber(),
                        paginated.getPageable().getPageSize(),
                        paginated.getTotalPages(),
                        paginated.getTotalElements(),
                        paginated.isFirst(),
                        paginated.isLast()
                ));
    }

    @GetMapping("{id}")
    public D getOne(@PathVariable String id) {
        return getEntityService().getById(id);
    }

    @PostMapping
    @ResponseStatus(CREATED)
    public D create(@Valid @RequestBody D dto) {
        handlePreCreate(dto);
        D createdEntity = getEntityService().create(dto);
        handlePostCreate(createdEntity);
        return createdEntity;
    }

    protected void handlePreCreate(D dto) {
        log.info("Creating entity from dto: {}", dto);
    }

    protected void handlePostCreate(D createdEntity) {
        log.info("Created entity: {}", createdEntity);
    }

    @PatchMapping
    @ResponseStatus(CREATED)
    public D update(@Valid @RequestBody D dto) {
        handlePreUpdate(dto);
        D updatedDto = getEntityService().update(dto);
        handlePostUpdate(updatedDto);
        return updatedDto;
    }

    protected void handlePreUpdate(D dto) {
        log.info("Updating entity from dto: {}", dto);
    }

    protected void handlePostUpdate(D updatedDto) {
        log.info("Updated entity: {}", updatedDto);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(CREATED)
    public void delete(@PathVariable String id) {
        handlePreDelete(id);
        D deletedDto = getEntityService().delete(id);
        handlePostDelete(deletedDto);
    }

    protected void handlePreDelete(String id) {
        log.info("Deleting entity with id: {}", id);
    }

    protected void handlePostDelete(D deletedDto) {
        log.info("Deleted entity: {}", deletedDto);
    }
}
