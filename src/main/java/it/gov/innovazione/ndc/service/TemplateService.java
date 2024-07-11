package it.gov.innovazione.ndc.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
@Getter
public class TemplateService {

    @Value("classpath:templates/alerter_mail_template.html")
    private final Resource alerterMailTemplateResource;
    @Value("classpath:templates/event_list_template.html")
    private final Resource eventListTemplateResource;
    @Value("classpath:templates/event_template.html")
    private final Resource eventTemplateResource;

    private String alerterMailTemplate;
    private String eventListTemplate;
    private String eventTemplate;

    @PostConstruct
    public void init() {
        alerterMailTemplate = readTemplate(alerterMailTemplateResource);
        eventListTemplate = readTemplate(eventListTemplateResource);
        eventTemplate = readTemplate(eventTemplateResource);
    }

    @SneakyThrows
    private String readTemplate(Resource eventTemplateResource) {
        return IOUtils.toString(eventTemplateResource.getInputStream(), StandardCharsets.UTF_8);
    }

}
