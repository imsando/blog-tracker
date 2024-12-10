package blogTracker.blogTracker.v1.domain.sender.service;

import blogTracker.blogTracker.v1.entity.Blogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SenderBusinessService {

    public Mono<Void> sendAlertEmail(Blogger blogger) {
        log.debug("Sending alert email to: {}", blogger.email());
        // TODO : 이메일 전송 로직
        return Mono.empty();
    }
}