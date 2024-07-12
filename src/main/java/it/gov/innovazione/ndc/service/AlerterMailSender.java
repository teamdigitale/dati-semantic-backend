package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.data.ProfileService;
import it.gov.innovazione.ndc.alerter.data.UserService;
import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.dto.UserDto;
import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.eventhandler.event.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey.ALERTER_ENABLED;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.text.StringSubstitutor.replace;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlerterMailSender {

    private final EmailService emailService;
    private final ProfileService profileService;
    private final EventService eventService;
    private final UserService userService;
    private final ConfigService configService;
    private final TemplateService templateService;

    private static boolean isAlertable(Instant lastAlertedAt, Integer aggregationTime, Instant now) {
        return lastAlertedAt.plusSeconds(aggregationTime).isBefore(now);
    }

    @Scheduled(fixedDelayString = "${alerter.mail-sender.fixed-delay-ms}")
    void getEventsAndAlert() {

        Collection<ProfileDto> profiles = profileService.findAll();
        for (ProfileDto profileDto : profiles) {

            Instant now = Instant.now();
            Instant lastAlertedAt = profileDto.getLastAlertedAt();

            if (isAlertable(lastAlertedAt, profileDto.getAggregationTime(), now)) {

                log.info("[{}-{}] Getting alertable events", profileDto.getName(), now);

                List<EventDto> eventsNewerThan = eventService.getEventsNewerThan(lastAlertedAt).stream()
                        .filter(eventDto -> isApplicableToProfile(eventDto, profileDto))
                        .collect(Collectors.toList());

                List<EventDto> filteredEvents = eventsNewerThan.stream()
                        .filter(eventDto -> eventDto.getCreatedAt().isBefore(now))
                        .filter(eventDto -> isSeverityGteThanMin(eventDto, profileDto))
                        .collect(Collectors.toList());

                log.info("[{}-{}] Found {} applicable events, {} match the conditions for alert",
                        profileDto.getName(),
                        now,
                        eventsNewerThan.size(),
                        filteredEvents.size());

                filteredEvents.stream()
                        .collect(Collectors.groupingBy(EventDto::getCategory))
                        .forEach((key, value) -> sendMessages(key, value, profileDto, now));

                log.info("[{}-{}] next check around {}",
                        profileDto.getName(),
                        now,
                        now.plusSeconds(profileDto.getAggregationTime()));
                profileService.setLastAlertedAt(profileDto.getId(), now);
            }
        }
    }

    private boolean isAlerterEnabled() {
        return (boolean) configService.fromGlobal(ALERTER_ENABLED).orElse(false);
    }

    private boolean isApplicableToProfile(EventDto eventDto, ProfileDto profileDto) {
        return emptyIfNull(profileDto.getEventCategories()).contains(eventDto.getCategory().name());
    }

    private void sendMessages(EventCategory category, List<EventDto> eventDtos, ProfileDto profileDto, Instant now) {

        if (eventDtos.isEmpty()) {
            return;
        }

        if (!isAlerterEnabled()) {
            log.warn("Alerter is disabled, no mails will be sent, following events will be just stored in the database."
                            + "Events: {}",
                    eventDtos.stream()
                            .map(EventDto::toString)
                            .collect(Collectors.joining(", ")));
            return;
        }

        List<UserDto> recipients = userService.findAll().stream()
                .filter(user -> StringUtils.equals(user.getProfile(), profileDto.getName()))
                .collect(Collectors.toList());

        if (!recipients.isEmpty()) {
            for (UserDto recipient : recipients) {
                log.info("[{}-{}] Sending email to user {} ({}) for detected {} events in category {}",
                        profileDto.getName(),
                        now,
                        recipient.getName() + " " + recipient.getSurname(),
                        recipient.getEmail(),
                        eventDtos.size(),
                        category);
                emailService.sendHtmlEmail(recipient.getEmail(),
                        "[SCHEMAGOV] [" + category + "] Alerter: Report degli eventi",
                        getHtmlMessageBodyFromTemplates(eventDtos, recipient));
            }
            return;
        }

        log.warn("[{}-{}] No recipients found - no mails will be sent. "
                        + "It might still be possible these events will be notified to other profiles. "
                        + "Events: {}",
                profileDto.getName(),
                now,
                eventDtos.stream()
                        .map(EventDto::toString)
                        .collect(Collectors.joining(", ")));
    }

    private String getHtmlMessageBodyFromTemplates(List<EventDto> eventDtos, UserDto recipient) {
        return replace(
                templateService.getAlerterMailTemplate(),
                Map.of(
                        "recipient.name", recipient.getName(),
                        "recipient.surname", recipient.getSurname(),
                        "eventList", replace(
                                templateService.getEventListTemplate(),
                                Map.of(
                                        "events", getEvents(eventDtos)))));
    }

    private String getEvents(List<EventDto> eventDtos) {
        return eventDtos.stream()
                .map(eventDto -> replace(
                        templateService.getEventTemplate(),
                        Map.of(
                                "event.name", eventDto.getName(),
                                "event.description", eventDto.getDescription(),
                                "event.severity", eventDto.getSeverity(),
                                "event.context", toSubList(eventDto.getContext()),
                                "event.createdBy", eventDto.getCreatedBy(),
                                "event.createdAt", toLocalDate(eventDto.getCreatedAt()))))
                .collect(Collectors.joining());
    }

    private String toLocalDate(Instant createdAt) {
        return DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss")
                .withZone(ZoneId.of("Europe/Rome"))
                .format(createdAt);
    }

    private String toSubList(Map<String, Object> context) {
        return "<ul>"
                + context.entrySet().stream()
                .map(entry -> "<li><b>" + entry.getKey() + ":</b> " + entry.getValue() + "</li>")
                .collect(Collectors.joining())
                + "</ul>";
    }

    private boolean isSeverityGteThanMin(EventDto eventDto, ProfileDto profileDto) {
        return eventDto.getSeverity().ordinal() >= profileDto.getMinSeverity().ordinal();
    }
}
