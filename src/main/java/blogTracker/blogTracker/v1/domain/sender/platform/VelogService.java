package blogTracker.blogTracker.v1.domain.sender.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class VelogService {

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        // TODO : Velog API를 이용하여 최근 포스트를 확인하는 로직
        log.debug("벨로그 주소 확인! : {}", blogUrl);
        return Mono.fromSupplier(() -> Math.random() > 0.5); // Mocked response
    }
}