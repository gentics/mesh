package com.gentics.vertx.cailun.perm;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.vertx.cailun.perm.model.User;

public interface UserRepository extends GraphRepository<User> {
//
//	@Override
//	@PreAuthorize("hasRole('ROLE_ADMIN')")
//	Page<User> findAll(Pageable pageable);
//
//	@Override
//	@PostAuthorize("returnObject.firstname == principal.username or hasRole('ROLE_ADMIN')")
//	User findOne(Long aLong);
//
//	@PreAuthorize("hasRole('ROLE_ADMIN')")
//	List<User> findByFirstnameLike(@Param("firstname") String firstname);

	User findByFirstnameEquals(String firstname);

	User findByUsername(String username);

}
