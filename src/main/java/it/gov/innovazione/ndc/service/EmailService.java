package it.gov.innovazione.ndc.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;

@Component
@RequiredArgsConstructor
@Slf4j
class EmailService {

    private final JavaMailSender javaMailSender;
    @Value("${alerter.mail.sender}")
    private final String from;

    void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }

    @SneakyThrows
    void sendHtmlEmail(String to, String subject, String htmlMessage) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
        helper.setText(htmlMessage, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(from);
        javaMailSender.send(mimeMessage);
    }
}
