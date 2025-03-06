package blogTracker.blogTracker.v1.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI(){

        return new OpenAPI()
                .info(new Info()
                        .title("blogTracker API")
                        .description("블로그 적금 모임용<br/>" +
                                     "<br/>" +
                                     "----------------------------<br/>" +
                                     "api의 request 파라미터에 대한 개별적인 description은 해당 페이지 최하단의 \"Schemas\"를 참고하시면 됩니다.<br/>" +
                                     "별표 (<font color=\"red\">*</font>) : 필수값. <br/>" +
                                     "[...] 을 누르시면 해당 값의 자세한 설명을 확인 하실 수 있습니다.<br/>" +
                                     "----------------------------<br/>")
                )
                .addServersItem(new Server().url("/"));
    }
}
