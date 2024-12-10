package blogTracker.blogTracker.v1.common.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;

@Primary
@Slf4j
@RequiredArgsConstructor
public class BloggerRepositoryCustomImpl implements BloggerRepositoryCustom{
    private final ReactiveMongoOperations mongoOperations;
}
