package blogTracker.blogTracker.v1.domain.sender.platform;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class VelogService {

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        return Mono.fromCallable(() -> {
            try {
                Document doc = Jsoup.connect(blogUrl).get();
                Elements posts = doc.select(".sc-16qplb7-0"); //1. 벨로그의 게시물 CSS 선택자
                if (posts.isEmpty()) {
                    return false;
                }
                String latestPostDate = posts.first().select(".date").text();
                LocalDateTime latestDate = parseVelogDate(latestPostDate);
                return latestDate.isAfter(LocalDateTime.now().minusMinutes(2));
            } catch (Exception e) {
                throw new CustomException(ErrorCodes.BAD_REQUEST);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private LocalDateTime parseVelogDate(String dateText) {
        //1. 벨로그의 날짜 형식에 맞게 파싱
        if (dateText.contains("분 전")) {
            int minutes = Integer.parseInt(dateText.replace("분 전", "").trim());
            return LocalDateTime.now().minusMinutes(minutes);
        } else if (dateText.contains("시간 전")) {
            int hours = Integer.parseInt(dateText.replace("시간 전", "").trim());
            return LocalDateTime.now().minusHours(hours);
        }
        //2. 그 외 다른 형식은 기본 포맷으로 처리
        return LocalDateTime.parse(dateText, DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }
}
