package com.gentics.vertx.cailun.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import com.gentics.vertx.cailun.model.perm.User;

public interface UserRepository extends GraphRepository<User> {

	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	Page<User> findAll(Pageable pageable);

	@Override
	@PostAuthorize("returnObject.firstName == principal.username or hasRole('ROLE_ADMIN')")
	User findOne(Long aLong);

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	List<User> findByFirstNameLike(@Param("firstName") String firstName);

	User findByFirstNameEquals(String firstName);

}
