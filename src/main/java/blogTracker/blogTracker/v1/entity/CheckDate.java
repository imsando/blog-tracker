package blogTracker.blogTracker.v1.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document
public record CheckDate(
        /*
         * KEY
         * */
        @Id
        ObjectId id,

        /*
        * 체크 날짜
        * */
        LocalDate checkDate,

        /*
        * 완료 여부
        * */
        boolean completed
) {
}
