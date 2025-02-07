package blogTracker.blogTracker.v1.common.repository;

import blogTracker.blogTracker.v1.entity.CheckDate;
import org.springframework.stereotype.Repository;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
@Repository
public interface CheckDateRepository extends ReactiveMongoRepository<CheckDate, ObjectId>, CheckDateRepositoryCustom {
}
