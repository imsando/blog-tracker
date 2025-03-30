package blogTracker.blogTracker.v1.domain.checker.service;

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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckerService {
    private final WebClient webClient;

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        int days = 14;
        String rssUrl = convertToRssUrl(blogUrl);
        log.debug("체크할 RSS URL : {}", rssUrl);

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

                        // 모든 <pubDate> 찾기
                        Pattern datePattern = Pattern.compile("<pubDate>([^<]+)</pubDate>");
                        Matcher dateMatcher = datePattern.matcher(cleanedContent);

                        List<LocalDateTime> postDates = new ArrayList<>();

                        while (dateMatcher.find()) {
                            String postDateStr = dateMatcher.group(1).trim();
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                            Date parsedDate = sdf.parse(postDateStr);
                            LocalDateTime postDate = LocalDateTime.ofInstant(parsedDate.toInstant(), ZoneId.systemDefault());
                            postDates.add(postDate);
                        }

                        if (postDates.isEmpty()) {
                            log.info("RSS에서 게시물의 pubDate를 찾을 수 없습니다.");
                            sink.next(false);
                            return;
                        }

                        // 가장 최신 글의 날짜 찾기
                        LocalDateTime latestPostDate = Collections.max(postDates);

                        LocalDateTime checkTime = LocalDateTime.now().minusDays(days);
                        boolean isRecentPost = latestPostDate.isAfter(checkTime);

                        log.info("가장 최신 게시물 작성 시간: {}, {}일 이내 작성 여부: {}", latestPostDate, days, isRecentPost);
                        sink.next(!isRecentPost);

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

            // Velog RSS 변환
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host != null && host.equals("velog.io")) {
                String path = uri.getPath();
                if (path.startsWith("/@")) {
                    String username = path.substring(2);
                    return "https://v2.velog.io/rss/" + username;
                }
            }

            // Tistory RSS 변환
            if (!url.endsWith("/rss")) {
                return url.replaceAll("/+$", "") + "/rss";
            }

            return url;

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CustomException(ErrorCodes.RSS_CONVERSION_ERROR);
        }
    }
}
