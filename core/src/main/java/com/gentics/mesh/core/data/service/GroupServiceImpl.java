package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.Collections;

import org.neo4j.cypher.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.mesh.core.repository.GroupRepository;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class GroupServiceImpl extends GenericNodeServiceImpl<Group> implements GroupService {

	@Autowired
	private GroupRepository groupRepository;

	@Override
	public Group findByName(String name) {
		return groupRepository.findByName(name);
	}

	@Override
	public Group findByUUID(String uuid) {
		try {
			return groupRepository.findByUUID(uuid);
		} catch (EntityNotFoundException e) {
			//TODO log error
			return null;
		}
	}

	// TODO handle depth
	@Override
	public GroupResponse transformToRest(RoutingContext rc, Group group) {
		GroupResponse restGroup = new GroupResponse();
		restGroup.setUuid(group.getUuid());
		restGroup.setName(group.getName());

		for (User user : group.getUsers()) {
			user = neo4jTemplate.fetch(user);
			String name = user.getUsername();
			if (name != null) {
				restGroup.getUsers().add(name);
			}
		}
		Collections.sort(restGroup.getUsers());

		for (Role role : group.getRoles()) {
			role = neo4jTemplate.fetch(role);
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(name);
			}
		}

		// // Set<Group> children = groupRepository.findChildren(group);
		// Set<Group> children = group.getGroups();
		// for (Group childGroup : children) {
		// restGroup.getGroups().add(childGroup.getName());
		// }

		return restGroup;
	}

	@Override
	public Page<Group> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
	}
}
