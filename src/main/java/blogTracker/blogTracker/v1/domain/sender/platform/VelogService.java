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
                            sink.next(true); // 게시물이 없으면 알림 발송
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

                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                        Date parsedDate = sdf.parse(latestPostDate);
                        LocalDateTime lastPostDate = LocalDateTime.ofInstant(
                                parsedDate.toInstant(),
                                ZoneId.systemDefault()
                        );

                        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
                        boolean isNewerThanTwoMinutes = lastPostDate.isAfter(twoMinutesAgo);

                        log.info("최근 게시물 작성 시간: {}, 2분 이내 작성 여부: {}", lastPostDate, isNewerThanTwoMinutes);
                        sink.next(!isNewerThanTwoMinutes);  // 2분 이내 게시물이 없으면 true (알림 발송)

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

            // velog.io/@username 형식을 rss 주소로 변환
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