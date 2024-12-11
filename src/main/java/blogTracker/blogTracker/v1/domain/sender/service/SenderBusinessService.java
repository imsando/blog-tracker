package blogTracker.blogTracker.v1.domain.sender.service;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import blogTracker.blogTracker.v1.entity.Blogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SenderBusinessService {

    private final JavaMailSender mailSender;

    public Mono<Void> sendAlertEmail(Blogger blogger) {
        return Mono.fromRunnable(() -> {
            try {
                log.debug("Sending alert email to: {}", blogger.email());
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(blogger.email());
                message.setSubject("블로그 글 등록 알림");
                message.setText(String.format("안녕하세요, %s님! 글을 등록하셔야 합니다 왜 안함 올려야지", blogger.name()));
                mailSender.send(message);
                log.info("Alert email sent to {}", blogger.email());
            } catch (Exception e) {
                log.error("전송에 실패한 이메일 주소 확인하자 {},  에러메시지 : {}", blogger.email(), e.getMessage());
                throw new CustomException(ErrorCodes.BAD_REQUEST);
            }
        }).then(); // 명시적으로 Mono<Void> 반환
    }
}
