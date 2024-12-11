package blogTracker.blogTracker.v1.domain.sender.platform;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@Slf4j
@Service
public class TistoryService {

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        return Mono.fromCallable(() -> {
            try {
                Document doc = Jsoup.connect(blogUrl).get();
                Elements posts = doc.select("article.post-item"); // 티스토리의 게시물 CSS 선택자
                if (posts.isEmpty()) {
                    return false;
                }
                String latestPostDate = posts.first().select("time").attr("datetime");
                LocalDateTime latestDate = LocalDateTime.parse(latestPostDate, DateTimeFormatter.ISO_DATE_TIME);
                return latestDate.isAfter(LocalDateTime.now().minusMinutes(2));
            } catch (Exception e) {
                throw new CustomException(ErrorCodes.BAD_REQUEST);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}