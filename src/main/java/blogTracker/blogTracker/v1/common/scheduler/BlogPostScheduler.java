package blogTracker.blogTracker.v1.common.scheduler;

import blogTracker.blogTracker.v1.common.repository.BloggerRepository;
import blogTracker.blogTracker.v1.domain.sender.SenderProcessor;
import blogTracker.blogTracker.v1.domain.sender.platform.TistoryService;
import blogTracker.blogTracker.v1.domain.sender.platform.VelogService;
import blogTracker.blogTracker.v1.domain.sender.service.SenderBusinessService;
import blogTracker.blogTracker.v1.entity.Blogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogPostScheduler {

    private final BloggerRepository bloggerRepository;
    private final VelogService velogService;
    private final TistoryService tistoryService;
    private final SenderBusinessService senderBusinessService;

    @Scheduled(cron = "0 0/2 * * * *") // NOTE : 테스트 전송을 확인하기 위해 2분마다 실행 -> 테스트 이후에는 2주로 변경 예정
    public void checkForBlogPosts() {
        bloggerRepository.findAll()
                .flatMap(this::checkAndSendAlert)
                .subscribe();
    }

    private Mono<Void> checkAndSendAlert(Blogger blogger) {
        log.debug("Checking for new posts from: {}", blogger.blogUrl());
        return hasPosted(blogger)
                .filter(posted -> !posted)
                .flatMap(unused -> senderBusinessService.sendAlertEmail(blogger))
                .then();
    }

    private Mono<Boolean> hasPosted(Blogger blogger) {
        if (blogger.blogUrl().contains("velog.io")) { // 벨로그
            return velogService.checkRecentPosts(blogger.blogUrl());
        } else if (blogger.blogUrl().contains("tistory.com")) { // 티스토리
            return tistoryService.checkRecentPosts(blogger.blogUrl());
        }
        return Mono.just(false); // 다른 플랫폼은 지원하지 않음
    }
}
