package blogTracker.blogTracker.v1.common.exception;

import blogTracker.blogTracker.v1.common.response.ResultData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ResultData<String>> handleCustomException(CustomException e) {
        return ResponseEntity.status(e.httpStatusCode())
                .body(ResultData.<String>builder()
                        .body(e.body())
                        .message(e.message())
                        .build()
                );
    }
}
