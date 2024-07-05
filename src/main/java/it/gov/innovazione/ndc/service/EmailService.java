package it.gov.innovazione.ndc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class EmailService {

    private final JavaMailSender javaMailSender;
    @Value("${alerter.mail.sender}")
    private final String from;
    @Value("${spring.mail.properties.mail.debug:false}")
    private boolean mailDebug;

    void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }

    @EventListener(ApplicationStartedEvent.class)
    void debugSendMail() {
        if (mailDebug) {
            log.info("Sending test email");
            sendEmail("servicedesk-schema@istat.it", "Test", "Test");
        }
    }

}
