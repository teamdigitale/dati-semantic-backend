package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.entities.Nameable;

import java.util.List;
import java.util.stream.Collectors;

public interface EntityMapper<T extends Nameable, D extends Nameable> {

    default List<D> toDto(List<T> entities) {
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }

    D toDto(T entity);

    T toEntity(D dto);
}
