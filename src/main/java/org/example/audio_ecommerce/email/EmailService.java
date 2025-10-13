package org.example.audio_ecommerce.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateResolver templateResolver;

    @Async
    public void sendEmail(EmailTemplateType type, Object data) throws MessagingException {
        EmailTemplate template = templateResolver.resolve(type, data);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(template.getTo());
        helper.setSubject(template.getSubject());
        helper.setText(template.getContent(), true);

        mailSender.send(message);
    }
}
