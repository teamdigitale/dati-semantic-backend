package it.gov.innovazione.ndc.service;

import it.gov.innovazione.ndc.alerter.data.EventService;
import it.gov.innovazione.ndc.alerter.data.ProfileService;
import it.gov.innovazione.ndc.alerter.data.UserService;
import it.gov.innovazione.ndc.alerter.dto.EventDto;
import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.dto.UserDto;
import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlerterMailSender {

    private final EmailService emailService;
    private final ProfileService profileService;
    private final EventService eventService;
    private final UserService userService;

    @Scheduled(fixedDelayString = "${alerter.mail-sender.fixed-delay:60000}")
    void getEventsAndAlert() {
        Collection<ProfileDto> profiles = profileService.findAll();
        for (ProfileDto profileDto : profiles) {

            Instant now = Instant.now();
            Instant lastAlertedAt = profileDto.getLastAlertedAt();

            eventService.getEventsNewerThan(lastAlertedAt).stream()
                    .filter(eventDto -> eventDto.getCreatedAt().plusSeconds(profileDto.getAggregationTime()).isBefore(now))
                    .filter(eventDto -> isSeverityHigherThanOrEqualsToMinSeverity(eventDto, profileDto))
                    .filter(eventDto -> isApplicableToProfile(eventDto, profileDto))
                    .collect(Collectors.groupingBy(EventDto::getCategory))
                    .forEach((key, value) -> sendMessages(key, value, profileDto));
        }
    }

    private boolean isApplicableToProfile(EventDto eventDto, ProfileDto profileDto) {
        return emptyIfNull(profileDto.getEventCategories()).contains(eventDto.getCategory().name());
    }

    private void sendMessages(EventCategory category, List<EventDto> eventDtos, ProfileDto profileDto) {
        if (eventDtos.isEmpty()) {
            return;
        }
        log.info("Sending email for detected {} events, to users with profile {} for category: {}",
                eventDtos.size(), profileDto.getName(), category);
        List<UserDto> recipients = userService.findAll().stream()
                .filter(user -> StringUtils.equals(user.getProfile(), profileDto.getName()))
                .collect(Collectors.toList());
        if (recipients.isEmpty()) {
            log.warn("No recipients found for profile {}, "
                            + "for this profile no mails will be sent. "
                            + "It might still be possible these events will be notified to other profiles. "
                            + "Events: {}",
                    profileDto.getName(),
                    eventDtos.stream()
                            .map(EventDto::toString)
                            .collect(Collectors.joining(", ")));
            return;
        }
        for (UserDto recipient : recipients) {
            emailService.sendEmail(recipient.getEmail(),
                    "[SCHEMAGOV] [" + category + "] Alerter: Report degli eventi",
                    getMessageBody(eventDtos, recipient, profileDto));
        }
        profileService.setLastUpdated(profileDto.getId());
    }

    private String getMessageBody(List<EventDto> eventDtos, UserDto recipient, ProfileDto profileDto) {
        StringBuilder message = new StringBuilder("Ciao " + recipient.getName() + " " + recipient.getSurname() + ",\n\n"
                + "Di seguito i dettagli degli errori riscontrati:\n");
        int i = 1;
        for (EventDto eventDto : eventDtos) {
            message.append(getDetailsForEvent(i, eventDto, recipient, profileDto));
            i++;
        }
        message.append("Origine: Generata automaticamente dall'harvester.\n\n");
        message.append("Cordiali saluti,\n\nIl team di supporto di Schemagov");
        return message.toString();
    }

    private String getDetailsForEvent(int i, EventDto eventDto, UserDto recipient, ProfileDto profileDto) {
        return i + ". Titolo: " + eventDto.getName() + "\n"
                + "Descrizione: " + eventDto.getDescription() + "\n"
                + "Severity: " + eventDto.getSeverity() + "\n"
                + "Contesto: " + eventDto.getContext() + "\n"
                + "Creato da: " + eventDto.getCreatedBy() + "\n"
                + "Creato il: " + eventDto.getCreatedAt() + "\n\n";
    }

    private boolean isSeverityHigherThanOrEqualsToMinSeverity(EventDto eventDto, ProfileDto profileDto) {
        return eventDto.getSeverity().ordinal() >= profileDto.getMinSeverity().ordinal();
    }

}
