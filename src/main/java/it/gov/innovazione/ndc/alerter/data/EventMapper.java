package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.entities.Event;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class EventMapper implements EntityMapper<Event, EventDto> {

    @Setter
    protected ContextUtils contextUtils;

    @Override
    @Mapping(target = "context", expression = "java(contextUtils.toContext(entity.getContext()))")
    public abstract EventDto toDto(Event entity);

    @Override
    @Mapping(target = "context", expression = "java(contextUtils.fromContext(dto.getContext()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", expression = "java(MapUtils.defaultIfNull(dto.getCreatedBy()))")
    public abstract Event toEntity(EventDto dto);
}
