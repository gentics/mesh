package com.gentics.cailun.core.data.service;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericNode;
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
	public List<Role> findAll() {

		// TODO i assume this could create memory problems for big data
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			List<Role> list = new ArrayList<>();
			for (Role role : roleRepository.findAll()) {
				list.add(role);
			}
			tx.success();
			return list;
		}

	}

	@Override
	public void addPermission(Role role, GenericNode node, PermissionType... permissionTypes) {
		// TODO why is the @Transactional not working?
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			GraphPermission permission = getGraphPermission(role, node);
			// Create a new permission relation when no existing one could be found
			if (permission == null) {
				permission = new GraphPermission(role, node);
			}
			for (int i = 0; i < permissionTypes.length; i++) {
				permission.grant(permissionTypes[i]);
			}
			role.addPermission(permission);
			role = save(role);
			tx.success();
		}
	}

	@Override
	public GraphPermission getGraphPermission(Role role, GenericNode node) {
		return roleRepository.findPermission(role.getId(), node.getId());
	}

	@Override
	public GraphPermission revokePermission(Role role, GenericNode node, PermissionType... permissionTypes) {
		try (Transaction tx = springConfig.getGraphDatabaseService().beginTx()) {
			GraphPermission permission = getGraphPermission(role, node);
			// Create a new permission relation when no existing one could be found
			if (permission == null) {
				return null;
			}
			for (int i = 0; i < permissionTypes.length; i++) {
				permission.revoke(permissionTypes[i]);
			}
			role.addPermission(permission);
			role = save(role);
			tx.success();
			return permission;
		}
	}

	@Override
	public RestRole transformToRest(Role role) {
		if (role == null) {
			return null;
		}
		RestRole restRole = new RestRole();
		restRole.setUuid(role.getUuid());
		restRole.setName(role.getName());

		return restRole;
	}

}
