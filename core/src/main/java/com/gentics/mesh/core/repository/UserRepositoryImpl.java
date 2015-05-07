package com.gentics.mesh.core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.auth.UserRoot;
import com.gentics.mesh.core.repository.action.UserActions;

public class UserRepositoryImpl implements UserActions {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Override
	public User save(User user) {
		UserRoot root = userRepository.findRoot();
		if (root == null) {
			throw new NullPointerException("The user root node could not be found.");
		}
		user = neo4jTemplate.save(user);
		root.getUsers().add(user);
		neo4jTemplate.save(root);
		return user;
	}

}
