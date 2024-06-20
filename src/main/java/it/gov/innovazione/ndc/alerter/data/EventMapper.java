package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.entities.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class EventMapper implements EntityMapper<Event, EventDto> {

    protected ContextUtils contextUtils;

    @Autowired
    public void setContextUtils(ContextUtils contextUtils) {
        this.contextUtils = contextUtils;
    }

    @Override
    @Mapping(target = "context", expression = "java(contextUtils.toContext(entity.getContext()))")
    public abstract EventDto toDto(Event entity);

    @Override
    @Mapping(target = "context", expression = "java(contextUtils.fromContext(dto.getContext()))")
    public abstract Event toEntity(EventDto dto);
}
