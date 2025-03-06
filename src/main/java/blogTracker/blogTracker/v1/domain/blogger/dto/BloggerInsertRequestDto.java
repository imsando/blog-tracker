package blogTracker.blogTracker.v1.domain.blogger.dto;

import blogTracker.blogTracker.v1.entity.Blogger;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import org.bson.types.ObjectId;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BloggerInsertRequestDto(
        @Schema(description = "블로거 이름", example = "")
        String name,

        @Schema(description = "블로거 이메일", example = "")
        String email,

        @Schema(description = "블로그 주소", example = "")
        String blogUrl,

        @Schema(description = "관리자 아이디", example = "관리자 문의 필요")
        String adminPassword
) {
        public Blogger of(BloggerInsertRequestDto requestDto){
                return new Blogger(
                        new ObjectId(),
                        requestDto.name(),
                        requestDto.email(),
                        requestDto.blogUrl()
                );
        }
}
