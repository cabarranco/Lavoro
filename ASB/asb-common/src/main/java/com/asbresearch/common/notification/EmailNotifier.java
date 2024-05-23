package com.asbresearch.common.notification;

import com.asbresearch.common.ThreadUtils;
import com.asbresearch.common.config.EmailProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@EnableConfigurationProperties(EmailProperties.class)
@Service
@Slf4j
public class EmailNotifier {
    private final JavaMailSender emailSender;
    private final ExecutorService worker;
    private final ObjectMapper objectMapper;
    private final EmailProperties emailProperties;

    @Autowired
    public EmailNotifier(JavaMailSender emailSender, ObjectMapper objectMapper, EmailProperties emailProperties) {
        this.emailSender = emailSender;
        this.objectMapper = objectMapper;
        this.emailProperties = emailProperties;
        this.worker = Executors.newSingleThreadScheduledExecutor(ThreadUtils.threadFactoryBuilder("email").build());
    }

    public void sendMessage(String text, String subject, List<String> toList) {
        if (emailProperties.isNotification()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toList.toArray(String[]::new));
                message.setSubject(subject);
                message.setText(text);
                emailSender.send(message);
                log.info("Email successfully sent to={} subject={} text={}", toList, subject, text);
            } catch (RuntimeException exception) {
                log.error("Error sending email to={} subject={} text={}", toList, subject, text, exception);
            }
        }
    }

    public void sendMessageAsync(String content, String subject, List<String> toList) {
        CompletableFuture.runAsync(() -> sendMessage(content, subject, toList), worker);
    }
}
