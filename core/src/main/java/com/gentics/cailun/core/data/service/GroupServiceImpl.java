package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Group;
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
	public RestGroup getReponseObject(Group group) {
		RestGroup restGroup = new RestGroup();
		restGroup.setName(group.getName());
		restGroup.setUuid(group.getUuid());
		return restGroup;
	}

}
