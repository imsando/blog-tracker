package blogTracker.blogTracker.v1.domain.sender.platform;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VelogService {
    private final WebClient webClient;

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        String rssUrl = convertToRssUrl(blogUrl);
        log.info("체크할 RSS URL: {}", rssUrl);

        return webClient.get()
                .uri(rssUrl)
                .retrieve()
                .bodyToMono(String.class)
                .<Boolean>handle((response, sink) -> {
                    try {
                        if (response.trim().startsWith("<!DOCTYPE html>") ||
                            response.trim().startsWith("<html")) {
                            log.error("RSS 응답이 올바르지 않습니다. URL: {}", rssUrl);
                            sink.error(new CustomException(ErrorCodes.BAD_REQUEST));
                            return;
                        }

                        String cleanedContent = response
                                .replaceAll("&(?!(?:amp|lt|gt|apos|quot);)", "&amp;")
                                .replaceAll("]]>", "]]&gt;");

                        // 게시물의 <item> 태그 찾기
                        Pattern itemPattern = Pattern.compile("<item>([\\s\\S]*?)</item>");
                        Matcher itemMatcher = itemPattern.matcher(cleanedContent);

                        if (!itemMatcher.find()) {
                            log.info("게시물이 없는 블로그입니다: {}", rssUrl);
                            sink.next(false); // 게시물이 없으면 false 반환
                            return;
                        }

                        // 첫 번째 item의 pubDate 찾기
                        String firstItem = itemMatcher.group(1);
                        Pattern datePattern = Pattern.compile("<pubDate>([^<]+)</pubDate>");
                        Matcher dateMatcher = datePattern.matcher(firstItem);

                        if (!dateMatcher.find()) {
                            log.error("게시물에서 발행일자를 찾을 수 없습니다.");
                            sink.error(new CustomException(ErrorCodes.BAD_REQUEST));
                            return;
                        }

                        String latestPostDate = dateMatcher.group(1).trim();
                        log.debug("파싱할 게시물 날짜: {}", latestPostDate);

                        // pubDate를 UTC 기준으로 변환
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                        Date parsedDate = sdf.parse(latestPostDate);
                        ZonedDateTime postZonedDateTime = parsedDate.toInstant().atZone(ZoneOffset.UTC);

                        // 24시간 전과 비교 (UTC 기준)
                        ZonedDateTime oneDayAgo = ZonedDateTime.now(ZoneOffset.UTC).minusDays(13);
                        boolean isRecentPost = postZonedDateTime.isAfter(oneDayAgo);

                        log.info("최근 게시물 작성 시간(UTC): {}, 1일 이내 작성 여부: {}", postZonedDateTime, isRecentPost);
                        sink.next(isRecentPost);

                    } catch (Exception e) {
                        log.error("RSS 처리 중 에러 발생: {}, 원본 메시지: {}", e.getClass().getName(), e.getMessage());
                        sink.error(new CustomException(ErrorCodes.BAD_REQUEST));
                    }
                }).subscribeOn(Schedulers.boundedElastic());
    }

    private String convertToRssUrl(String blogUrl) {
        try {
            String url = blogUrl.trim();
            if (!url.startsWith("http")) {
                url = "https://" + url;
            }

            // velog.io/@username 형식을 RSS 주소로 변환
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host != null && host.equals("velog.io")) {
                String path = uri.getPath();
                if (path.startsWith("/@")) {
                    String username = path.substring(2);
                    return "https://v2.velog.io/rss/" + username;
                }
            }

            throw new CustomException(ErrorCodes.BAD_REQUEST);

        } catch (Exception e) {
            log.error("URL 변환 중 에러 발생: {}", e.getMessage());
            throw new CustomException(ErrorCodes.BAD_REQUEST);
        }
    }
}
