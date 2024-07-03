package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.AlerterService;
import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.entities.Event;
import org.apache.commons.lang3.StringUtils;
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

    public static String defaultIfNull(String createdBy) {
        if (StringUtils.isEmpty(createdBy)) {
            return AlerterService.getUser();
        }
        return createdBy;
    }

    @Override
    @Mapping(target = "context", expression = "java(contextUtils.fromContext(dto.getContext()))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", expression = "java(EventMapper.defaultIfNull(dto.getCreatedBy()))")
    public abstract Event toEntity(EventDto dto);
}
