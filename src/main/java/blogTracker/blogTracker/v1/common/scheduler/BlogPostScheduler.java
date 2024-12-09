package blogTracker.blogTracker.v1.common.scheduler;

import blogTracker.blogTracker.v1.common.repository.BloggerRepository;
import blogTracker.blogTracker.v1.domain.sender.SenderProcessor;
import blogTracker.blogTracker.v1.entity.Blogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class BlogPostScheduler {

    private final BloggerRepository bloggerRepository;
    private final SenderProcessor senderProcessor;

    @Scheduled(cron = "0 * * * * *") // 1분마다 실행
    public void checkForBlogPosts() {
        // TODO : 블로거 목록을 조회하는 로직
    }

    private boolean hasPosted(Blogger blogger) {
        // TODO : 블로거가 최근 00일 이내에 포스팅을 했는지 확인하는 로직
        return false;
    }
}
