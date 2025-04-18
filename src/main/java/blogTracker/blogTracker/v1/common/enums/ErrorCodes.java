package blogTracker.blogTracker.v1.common.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCodes {
    //400
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다."),
    RSS_CONVERSION_ERROR(HttpStatus.BAD_REQUEST.value(), "RSS 변환 중 오류가 발생했습니다."),

    //403
    NOT_ADMIN(HttpStatus.FORBIDDEN.value(), "관리자가 아닙니다."),

    //500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류입니다.");

    private final int httpStatusCode;
    private final String message;

    ErrorCodes(int httpStatusCode, String message) {
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }

    public int httpStatusCode() {
        return httpStatusCode;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
