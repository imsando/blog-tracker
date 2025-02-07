package blogTracker.blogTracker.v1.common.repository;

import blogTracker.blogTracker.v1.entity.CheckDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import reactor.core.publisher.Mono;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Primary
@Slf4j
@RequiredArgsConstructor
public class CheckDateRepositoryCustomImpl implements CheckDateRepositoryCustom {
    private final ReactiveMongoOperations mongoOperations;

    @Override
    public Mono<CheckDate> findByScheduledDateAndChecked(LocalDate date, boolean checked) {
        LocalDateTime startOfDay = date.atStartOfDay(); // 2025-02-07T00:00:00
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX); // 2025-02-07T23:59:59.999

        Query query = new Query(
                Criteria.where("checkDate").gte(startOfDay).lte(endOfDay)
                        .and("completed").is(checked)
        );

        return mongoOperations.findOne(query, CheckDate.class)
                .doOnNext(result -> log.info("Found check date for date: {} with checked: {}", date, checked));
    }


    @Override
    public Mono<Void> updateCompleted(LocalDate date) {
        Query query = Query.query(Criteria.where("completed").is(false)
                .and("checkDate").lte(date));

        Update update = Update.update("checked", true);

        return mongoOperations.updateMulti(query, update, CheckDate.class)
                .doOnSuccess(result -> log.info("Updated {} documents to completed",
                        result.getModifiedCount()))
                .then();
    }
}