package blogTracker.blogTracker.v1.domain.sender.platform;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
    private final WebClient webClient;

    public TistoryService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://example.com").build(); // base URL 설정
    }

    public Mono<Boolean> checkRecentPosts(String rssUrl) {
        return webClient.get()
                .uri(rssUrl)
                .retrieve()
                .bodyToMono(String.class)
                .<Boolean>handle((rssContent, sink) -> {
                    try {
                        // XML 파싱
                        // RSS feed parsing: 사용하는 라이브러리에서 직접 XML 파싱 후, 원하는 데이터 추출
                        log.info("RSS content 확인해보자 : {}", rssContent);
                        int pubDateStartIndex = rssContent.indexOf("<pubDate>");
                        int pubDateEndIndex = rssContent.indexOf("</pubDate>");
                        if (pubDateStartIndex != -1 && pubDateEndIndex != -1) {
                            String latestPostDate = rssContent.substring(pubDateStartIndex + 9, pubDateEndIndex);
                            LocalDateTime latestDate = LocalDateTime.parse(latestPostDate, DateTimeFormatter.RFC_1123_DATE_TIME);
                            sink.next(latestDate.isAfter(LocalDateTime.now().minusMinutes(2)));
                            return;
                        }
                        sink.next(false);
                    } catch (Exception e) {
                        log.error("Error parsing RSS feed: {}", e.getMessage());
                        sink.error(new CustomException(ErrorCodes.BAD_REQUEST));
                    }
                }).subscribeOn(Schedulers.boundedElastic());
    }

//    public Mono<Boolean> checkRecentPosts(String blogUrl) {
//        return Mono.fromCallable(() -> {
//            try {
//                Document doc = Jsoup.connect(blogUrl).get();
//                Elements posts = doc.select("article.post-item"); // 티스토리의 게시물 CSS 선택자
//                if (posts.isEmpty()) {
//                    return false;
//                }
//                String latestPostDate = posts.first().select("time").attr("datetime");
//                LocalDateTime latestDate = LocalDateTime.parse(latestPostDate, DateTimeFormatter.ISO_DATE_TIME);
//                return latestDate.isAfter(LocalDateTime.now().minusMinutes(2));
//            } catch (Exception e) {
//                throw new CustomException(ErrorCodes.BAD_REQUEST);
//            }
//        }).subscribeOn(Schedulers.boundedElastic());
//    }
}