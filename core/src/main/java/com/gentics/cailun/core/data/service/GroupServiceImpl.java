package com.gentics.cailun.core.data.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.rest.response.RestGroup;

@Component
@Transactional
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
	public RestGroup transformToRest(Group group) {
		RestGroup restGroup = new RestGroup();
		restGroup.setUuid(group.getUuid());
		restGroup.setName(group.getName());

		for (User user : group.getUsers()) {
			restGroup.getUsers().add(user.getUsername());
		}

		for (Role role : group.getRoles()) {
			restGroup.getRoles().add(role.getName());
		}

//		Set<Group> children = groupRepository.findChildren(group);
		Set<Group> children = group.getGroups();
		for (Group childGroup : children) {
			restGroup.getChildGroups().add(childGroup.getName());
		}

		return restGroup;
	}
}
