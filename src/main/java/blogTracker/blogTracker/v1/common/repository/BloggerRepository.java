package blogTracker.blogTracker.v1.common.repository;

import blogTracker.blogTracker.v1.entity.Blogger;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloggerRepository extends ReactiveMongoRepository<Blogger, ObjectId>, BloggerRepositoryCustom {

}
