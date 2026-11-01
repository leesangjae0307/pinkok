package com.example.pinkok_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 재설정 인증번호를 이메일로 보낸다.
 *
 * <p>SMTP({@code spring.mail.host})가 설정돼 있으면 실제로 보내고, 설정이 없으면(개발 중) 메일 대신
 * 서버 로그에 인증번호를 남긴다. 메일 발송이 실패해도 예외를 밖으로 던지지 않는다 — 던지면
 * "가입된 이메일인지"가 응답으로 새어 나가기 때문이다.
 */
@Component
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public PasswordResetMailer(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendCode(String to, String code, long validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("[메일 미설정] {} 의 비밀번호 재설정 인증번호: {}", to, code);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (!from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(to);
            message.setSubject("[PinKok] 비밀번호 재설정 인증번호");
            message.setText("인증번호: " + code + "\n\n"
                    + validMinutes + "분 안에 앱에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시해도 됩니다.");
            sender.send(message);
        } catch (RuntimeException e) {
            log.error("비밀번호 재설정 메일 발송 실패 (to={})", to, e);
        }
    }
}
