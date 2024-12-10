package blogTracker.blogTracker.v1.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record Blogger(
        /**
         * KEY
         * */
        @Id
        ObjectId id,

        /**
         * 이름
         * */
        String name,

        /**
         * 이메일
         * */
        String email,

        /**
         * 블로그 주소
         * */
        String blogUrl
) {
}
