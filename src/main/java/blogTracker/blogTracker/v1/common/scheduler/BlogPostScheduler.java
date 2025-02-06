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

    @Scheduled(cron = "0 0/1 * * * *") // every 1 minute

    public void checkForBlogPosts() {
        log.info("============================================= 주기가 시작되었습니다. =============================================");
        bloggerRepository.findAll()
                .doOnNext(blogger -> log.info("Found blogger: {}", blogger))
                .flatMap(this::checkAndSendAlert)
                .doOnError(error -> log.error("Error during blog post check: ", error))
                .doOnComplete(() -> log.info("Blog post check completed"))
                .subscribe(
                        null,
                        error -> log.error("Error in subscription: ", error),
                        () -> log.info("Subscription completed")
                );
    }

    private Mono<Void> checkAndSendAlert(Blogger blogger) {
        log.info("Checking for new posts from: {}", blogger.blogUrl());
        return hasPosted(blogger)
                .doOnNext(posted -> log.info("Has posted check result for {}: {}", blogger.blogUrl(), posted))
                .filter(posted -> !posted)
                .doOnNext(unused -> log.info("Sending alert for {}", blogger.blogUrl()))
                .flatMap(unused -> senderBusinessService.sendAlertEmail(blogger))
                .doOnError(error -> log.error("Error in checkAndSendAlert for {}: ", blogger.blogUrl(), error))
                .then();
    }

    private Mono<Boolean> hasPosted(Blogger blogger) {
        log.info("Checking blogger platform: {}", blogger);
        if (blogger.blogUrl().contains("velog.io")) {
            log.info("Processing Velog blog: {}", blogger.blogUrl());
            return velogService.checkRecentPosts(blogger.blogUrl())
                    .doOnNext(result -> log.info("Velog check result for {}: {}", blogger.blogUrl(), result));
        } else if (blogger.blogUrl().contains("tistory.com")) {
            log.info("Processing Tistory blog: {}", blogger.blogUrl());
            return tistoryService.checkRecentPosts(blogger.blogUrl())
                    .doOnNext(result -> log.info("Tistory check result for {}: {}", blogger.blogUrl(), result));
        }
        log.warn("Unsupported blog platform: {}", blogger.blogUrl());
        return Mono.just(false);
    }
}