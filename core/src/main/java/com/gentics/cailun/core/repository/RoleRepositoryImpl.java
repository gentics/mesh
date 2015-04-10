package com.gentics.cailun.core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.RoleRoot;
import com.gentics.cailun.core.repository.action.RoleActions;

public class RoleRepositoryImpl implements RoleActions {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Override
	public Role save(Role role) {
		RoleRoot root = roleRepository.findRoot();
		if (root == null) {
			throw new NullPointerException("The role root node could not be found.");
		}
		role = neo4jTemplate.save(role);
		root.getRoles().add(role);
		neo4jTemplate.save(root);
		return role;
	}

}
