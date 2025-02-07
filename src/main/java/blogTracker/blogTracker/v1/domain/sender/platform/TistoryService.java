package blogTracker.blogTracker.v1.domain.sender.platform;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
public class TistoryService {
    private final WebClient webClient;

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        String rssUrl = convertToRssUrl(blogUrl);
        log.debug("===================================================");
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

                        Pattern pattern = Pattern.compile("<pubDate>([^<]+)</pubDate>");
                        Matcher matcher = pattern.matcher(cleanedContent);

                        if (!matcher.find()) {
                            log.error("RSS에서 pubDate를 찾을 수 없습니다.");
                            sink.error(new CustomException(ErrorCodes.BAD_REQUEST));
                            return;
                        }

                        String latestPostDate = matcher.group(1).trim();
                        log.debug("파싱할 날짜: {}", latestPostDate);

                        // 날짜 파싱을 SimpleDateFormat으로 변경
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                        Date parsedDate = sdf.parse(latestPostDate);
                        LocalDateTime latestDate = LocalDateTime.ofInstant(
                                parsedDate.toInstant(),
                                ZoneId.systemDefault()
                        );

                        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
                        boolean isNewerThanTwoMinutes = latestDate.isAfter(twoMinutesAgo);

                        log.info("최근 포스팅 시간: {}, 2분 이내 포스팅 여부: {}", latestDate, isNewerThanTwoMinutes);
                        sink.next(isNewerThanTwoMinutes);  // 2분 이내 포스팅이 없으면 true (알림 발송)

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

            if (url.endsWith("/rss")) {
                return url;
            }

            return url.replaceAll("/+$", "") + "/rss";

        } catch (Exception e) {
            log.error("URL 변환 중 에러 발생: {}", e.getMessage());
            throw new CustomException(ErrorCodes.BAD_REQUEST);
        }
    }
}