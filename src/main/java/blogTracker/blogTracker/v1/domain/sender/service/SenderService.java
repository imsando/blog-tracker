package blogTracker.blogTracker.v1.domain.sender.service;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import blogTracker.blogTracker.v1.entity.Blogger;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SenderService {

    private final JavaMailSender mailSender;

    public Mono<Void> sendAlertEmail(Blogger blogger) {
        return Mono.fromRunnable(() -> {
            try {
                log.debug("Sending alert email to: {}", blogger.email());

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setTo(blogger.email());
                helper.setSubject("블로그 글 등록 알림");

                String body = String.format("""
            <h2>안녕하세요~ %s님.</h2>
            <h2>%s 에 글이 포스팅되지 않았습니다.</h2>
            <h2>확인해주세요 ~ !!!</h2>
            <a href='https://ifh.cc/v-f5RjDp' target='_blank'><img src='https://ifh.cc/g/f5RjDp.jpg' style='width: 800px; height: 300px;'></a>
            """,
                        blogger.name(),
                        blogger.blogUrl()
                );

                helper.setText(body, true);
                mailSender.send(mimeMessage);
                log.info("이메일 로직 로그 {}", blogger.email());
            } catch (Exception e) {
                log.error("전송에 실패한 이메일 주소 확인하자 {},  에러메시지 : {}", blogger.email(), e.getMessage());
                throw new CustomException(ErrorCodes.BAD_REQUEST);
            }
        }).then();
    }
}
