package com.gentics.cailun.core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.GroupRoot;
import com.gentics.cailun.core.repository.action.GroupActions;

public class GroupRepositoryImpl implements GroupActions {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private GroupRepository groupRepository;

	@Override
	public Group save(Group group) {
		GroupRoot root = groupRepository.findRoot();
		if (root == null) {
			throw new NullPointerException("The group root node could not be found.");
		}
		group = neo4jTemplate.save(group);
		root.getGroups().add(group);
		neo4jTemplate.save(root);
		return group;
	}
}
