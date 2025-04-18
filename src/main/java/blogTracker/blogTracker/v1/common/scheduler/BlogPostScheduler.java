package blogTracker.blogTracker.v1.common.scheduler;

import blogTracker.blogTracker.v1.common.repository.BloggerRepository;
import blogTracker.blogTracker.v1.common.repository.CheckDateRepository;
import blogTracker.blogTracker.v1.domain.checker.service.CheckerService;
import blogTracker.blogTracker.v1.domain.sender.service.SenderService;
import blogTracker.blogTracker.v1.entity.Blogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlogPostScheduler {
    private final BloggerRepository bloggerRepository;
    private final CheckerService checkerService;
    private final SenderService senderService;
    private final CheckDateRepository checkDateRepository;


    @Scheduled(cron = "0 00 16 * * *")
    public void checkForBlogPosts() {
        LocalDate today = LocalDate.now();
        log.info("Scheduler started for date: {}", today);

        checkDateRepository.findByScheduledDateAndChecked(today, false)
                .doOnNext(checkDate -> {
                    log.info("Found check date document: {}", checkDate);
                    log.info("Document date: {}, Today: {}, Equals: {}",
                            checkDate.checkDate(), today, checkDate.checkDate().equals(today));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("오늘은 검사 날짜가 아닙니다: {}", today);
                    return Mono.empty();
                }))
                .flatMapMany(checkDate -> {
                    log.info("============================================= 예약된 체크를 시작합니다. =============================================");

                    return bloggerRepository.findAll()
                            .doOnNext(blogger -> log.info("블로거 도큐먼트 찾기 : {}", blogger))
                            .flatMap(this::checkAndSendAlert)
                            .doOnError(error -> log.error("에러 : ", error))
                            .doOnComplete(() -> log.info("블로그 포스트 체크 완료"))
                            .thenMany(Flux.just(true));
                })
                .collectList()
                .flatMap(results -> {
                    if (!results.isEmpty()) {
                        log.info("체크 완료 상태 업데이트를 시작합니다.");
                        return checkDateRepository.updateCompleted(today);
                    }
                    return Mono.empty();
                })
                .subscribe(
                        null,
                        error -> log.error("스케줄러 실행 중 에러 발생: ", error),
                        () -> log.info("스케줄러 실행이 완료되었습니다.")
                );
    }


    private Mono<Void> checkAndSendAlert(Blogger blogger) {
        log.info("blogger.blogUrl() 확인 : {}", blogger.blogUrl());
        return hasPosted(blogger)
                .doOnNext(posted -> log.info("결과 {}: {}", blogger.blogUrl(), posted))
                .filter(posted -> !posted)
                .doOnNext(unused -> log.info("Sending alert for {}", blogger.blogUrl()))
                .flatMap(unused -> senderService.sendAlertEmail(blogger))
                .doOnError(error -> log.error("Error in checkAndSendAlert for {}: ", blogger.blogUrl(), error))
                .then();
    }

    private Mono<Boolean> hasPosted(Blogger blogger) {
        String blogUrl = blogger.blogUrl();
        log.debug("블로그 url 확인 : {}", blogUrl);

        if (isSupportedPlatform(blogUrl)) {
            log.debug("블로그 url 확인 : {}", blogUrl);
            return checkBlogPosts(blogUrl);
        }

        log.warn("=======================================");
        log.warn("지원하지 않는 플랫폼 : {}", blogUrl);
        log.warn("=======================================");
        return Mono.just(false);
    }

    private boolean isSupportedPlatform(String blogUrl) {
        return blogUrl.contains("velog.io") || blogUrl.contains("tistory.com");
    }

    private Mono<Boolean> checkBlogPosts(String blogUrl) {
        return checkerService.checkRecentPosts(blogUrl)
                .doOnNext(result -> log.info("Check result for {}: {}", blogUrl, result));
    }
}