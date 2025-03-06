package blogTracker.blogTracker.v1.domain.blogger;

import blogTracker.blogTracker.v1.common.exception.CustomException;
import blogTracker.blogTracker.v1.common.response.ResultData;
import blogTracker.blogTracker.v1.domain.blogger.dto.BloggerInsertRequestDto;
import blogTracker.blogTracker.v1.domain.blogger.service.BloggerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class BloggerController {
    private final BloggerService bloggerService;

    @PostMapping("/")
    @Operation(operationId = "blogerTracker", summary = "블로거 추가", description = """
            __블로거 정보를 추가하는 간단한 API 입니다.__
            <br>
            __아주 간단한... 정보만 받고 다른 정보를 리턴하지는 않습니다.__
            """)
    public Mono<ResultData<String>> insertBlogger(
            @RequestBody @Valid BloggerInsertRequestDto requestDto
    ) {
        return bloggerService.insertBlogger(requestDto)
                .then(Mono.just(new ResultData<>("짠 등록 완료!", "성공적. 굿.")))
                .onErrorResume(error -> {
                    if (error instanceof CustomException) {
                        return Mono.just(new ResultData<>(null, error.getMessage()));
                    }
                    return Mono.just(new ResultData<>(null, "등록 중 오류가 발생했습니다."));
                });
    }
}
