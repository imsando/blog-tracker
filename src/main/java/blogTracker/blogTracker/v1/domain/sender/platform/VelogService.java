package blogTracker.blogTracker.v1.domain.sender.platform;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class VelogService {
    private final WebClient webClient;  // HTTP 요청을 위한 WebClient 주입

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        return Mono.fromCallable(() -> {
            try {
                Document doc = Jsoup.connect(blogUrl).get();
                Elements posts = doc.select(".sc-16qplb7-0"); // 벨로그의 게시물 CSS 선택자

                if (posts.isEmpty()) {
                    log.debug("No posts found for blog URL: {}", blogUrl);
                    return true; // 포스트가 없으면 알림을 보내야 함
                }

                Element latestPost = posts.first();
                String latestPostDate = latestPost.select(".date").text();

                if (latestPostDate.isEmpty()) {
                    log.warn("Date not found in the latest post for blog URL: {}", blogUrl);
                    return true; // 날짜를 찾을 수 없으면 안전하게 알림을 보냄
                }

                LocalDateTime latestDate = parseVelogDate(latestPostDate);
                boolean hasRecentPost = latestDate.isAfter(LocalDateTime.now().minusMinutes(2));

                log.debug("Blog URL: {}, Latest post date: {}, Has recent post: {}",
                        blogUrl, latestDate, hasRecentPost);

                return !hasRecentPost; // 최근 포스트가 없으면 true 반환 (알림 발송)

            } catch (Exception e) {
                log.error("Error checking velog posts for URL {}: {}", blogUrl, e.getMessage());
                throw new CustomException(ErrorCodes.BAD_REQUEST);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private LocalDateTime parseVelogDate(String dateText) {
        try {
            if (dateText.contains("분 전")) {
                int minutes = Integer.parseInt(dateText.replace("분 전", "").trim());
                return LocalDateTime.now().minusMinutes(minutes);
            } else if (dateText.contains("시간 전")) {
                int hours = Integer.parseInt(dateText.replace("시간 전", "").trim());
                return LocalDateTime.now().minusHours(hours);
            } else if (dateText.contains("일 전")) {
                int days = Integer.parseInt(dateText.replace("일 전", "").trim());
                return LocalDateTime.now().minusDays(days);
            }

            // 그 외 다른 형식은 기본 포맷으로 처리
            return LocalDateTime.parse(dateText,
                    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));

        } catch (Exception e) {
            log.error("Error parsing velog date '{}': {}", dateText, e.getMessage());
            throw new CustomException(ErrorCodes.BAD_REQUEST);
        }
    }
}