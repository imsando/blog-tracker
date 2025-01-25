package blogTracker.blogTracker.v1.domain.sender.platform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class TistoryService {

    private final WebClient webClient;

    private boolean isXmlContent(String content) {
        return content != null && content.trim().startsWith("<?xml");
    }

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        final String rssUrl = blogUrl.startsWith("http") ? blogUrl + "/rss" : "https://" + blogUrl + "/rss";

        return webClient.get()
                .uri(rssUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseAndCheckLastPostDate)
                .onErrorResume(e -> {
                    log.error("Error checking Tistory posts for URL {}: {}", rssUrl, e.getMessage());
                    return Mono.just(false);
                });
    }

    private String sanitizeXmlContent(String content) {
        log.debug("Original RSS content: {}", content);

        if (!isXmlContent(content)) {
            throw new RuntimeException("Response is not valid XML.");
        }

        String sanitizedContent = content.replaceAll("\uFEFF", "") // BOM 제거
                .replaceAll("&(?!\\w+;)", "&amp;") // 엔터티 정리
                .replaceAll("<script.*?>.*?</script>", "") // script 태그 제거
                .replaceAll("<.*?>", "") // HTML 태그 제거
                .trim(); // 공백 제거

        log.debug("Sanitized RSS content: {}", sanitizedContent);
        return sanitizedContent;
    }

    private boolean parseAndCheckLastPostDate(String xmlContent) {
        if (xmlContent == null || !xmlContent.contains("<rss")) {
            return false;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() > 0) {
                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    String pubDate = getElementValue(item, "pubDate");
                    if (pubDate != null && isRecent(pubDate)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error parsing RSS: {}", e.getMessage());
            return false;
        }
    }
    // NOTE : 2주 이내에 작성된 글인지 확인
    private boolean isRecent(String pubDate) {
        try {
            LocalDateTime postDate = LocalDateTime.parse(
                    pubDate.trim().replace(" +0000", ""),
                    DateTimeFormatter.RFC_1123_DATE_TIME
            );
            return postDate.isAfter(LocalDateTime.now().minusWeeks(2));
        } catch (Exception e) {
            log.error("Date parsing error: {}", pubDate, e);
            return false;
        }
    }

    private String getElementValue(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}
