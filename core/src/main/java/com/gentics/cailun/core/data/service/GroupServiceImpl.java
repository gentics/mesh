package com.gentics.cailun.core.data.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.path.PagingInfo;

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
		return groupRepository.findByUUID(uuid);
	}

	@Override
	public GroupResponse transformToRest(Group group) {
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
		return groupRepository.findAll(requestUser, new PageRequest(pagingInfo.getPage(), pagingInfo.getPerPage()));
	}
}
