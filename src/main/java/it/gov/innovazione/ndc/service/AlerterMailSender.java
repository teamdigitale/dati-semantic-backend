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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static it.gov.innovazione.ndc.harvester.service.ActualConfigService.ConfigKey.ALERTER_ENABLED;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlerterMailSender {

    private final EmailService emailService;
    private final ProfileService profileService;
    private final EventService eventService;
    private final UserService userService;
    private final ConfigService configService;

    private static boolean isAlertable(Instant lastAlertedAt, Integer aggregationTime, Instant now) {
        return lastAlertedAt.plusSeconds(aggregationTime).isBefore(now);
    }

    @Scheduled(fixedDelayString = "${alerter.mail-sender.fixed-delay:10000}")
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
                emailService.sendEmail(recipient.getEmail(),
                        "[SCHEMAGOV] [" + category + "] Alerter: Report degli eventi",
                        getMessageBody(eventDtos, recipient));
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

    private String getMessageBody(List<EventDto> eventDtos, UserDto recipient) {
        StringBuilder message = new StringBuilder("Ciao " + recipient.getName() + " " + recipient.getSurname() + ",\n\n"
                + "Di seguito i dettagli degli eventi riscontrati:\n");
        int i = 1;
        for (EventDto eventDto : eventDtos) {
            message.append(getDetailsForEvent(i, eventDto));
            i++;
        }
        message.append("Origine: Generata automaticamente dall'harvester.\n\n");
        message.append("Cordiali saluti,\n\nIl team di supporto di Schemagov");
        return message.toString();
    }

    private String getDetailsForEvent(int i, EventDto eventDto) {
        return i + ". Titolo: " + eventDto.getName() + "\n"
                + "Descrizione: " + eventDto.getDescription() + "\n"
                + "Severity: " + eventDto.getSeverity() + "\n"
                + "Contesto: " + eventDto.getContext() + "\n"
                + "Creato da: " + eventDto.getCreatedBy() + "\n"
                + "Creato il: " + eventDto.getCreatedAt() + "\n\n";
    }

    private boolean isSeverityGteThanMin(EventDto eventDto, ProfileDto profileDto) {
        return eventDto.getSeverity().ordinal() >= profileDto.getMinSeverity().ordinal();
    }

}
