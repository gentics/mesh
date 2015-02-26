package com.gentics.cailun.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.core.rest.response.RestRole;

@Component
@Transactional
public class RoleServiceImpl extends GenericNodeServiceImpl<Role> implements RoleService {

	@Autowired
	RoleRepository roleRepository;

	@Override
	public Role findByUUID(String uuid) {
		return roleRepository.findByUUID(uuid);
	}

	@Override
	public Role findByName(String name) {
		return roleRepository.findByName(name);
	}

	@Override
	public RestRole getReponseObject(Role role) {
		RestRole restRole = new RestRole();
		restRole.setUuid(role.getUuid());
		restRole.setName(role.getName());
		return restRole;
	}

}
