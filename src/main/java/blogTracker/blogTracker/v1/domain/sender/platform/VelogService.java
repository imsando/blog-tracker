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
public class VelogService {
    private final WebClient webClient;

    public Mono<Boolean> checkRecentPosts(String blogUrl) {
        String rssUrl = blogUrl + "/rss";

        return webClient.get()
                .uri(rssUrl)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseAndCheckLastPostDate)
                .onErrorResume(e -> {
                    log.error("Error checking Velog posts: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    private boolean parseAndCheckLastPostDate(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));

            NodeList items = doc.getElementsByTagName("item");
            if (items.getLength() > 0) {
                Element firstItem = (Element) items.item(0);
                NodeList pubDateList = firstItem.getElementsByTagName("pubDate");

                if (pubDateList.getLength() > 0) {
                    String pubDate = pubDateList.item(0).getTextContent();

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
            }
            return false;
        } catch (Exception e) {
            log.error("Error parsing RSS: {}", e.getMessage());
            return false;
        }
    }
}