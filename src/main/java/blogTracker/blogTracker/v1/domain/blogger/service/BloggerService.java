package blogTracker.blogTracker.v1.domain.blogger.service;

import blogTracker.blogTracker.v1.common.enums.ErrorCodes;
import blogTracker.blogTracker.v1.common.exception.CustomException;
import blogTracker.blogTracker.v1.common.repository.BloggerRepository;
import blogTracker.blogTracker.v1.domain.blogger.dto.BloggerInsertRequestDto;
import blogTracker.blogTracker.v1.entity.Blogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloggerService {
    private final BloggerRepository bloggerRepository;

    public Mono<Blogger> insertBlogger(BloggerInsertRequestDto requestDto) {
        if(Objects.equals(requestDto.adminPassword(), "ssddo0524")) {
            return bloggerRepository.insert(requestDto.of(requestDto));
        } else {
            return Mono.error(new CustomException(ErrorCodes.NOT_ADMIN));
        }
    }
}
