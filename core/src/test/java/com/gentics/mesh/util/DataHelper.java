package com.gentics.mesh.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.Role;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.MeshUserService;
import com.gentics.mesh.core.data.service.RoleService;

/**
 * Various helper methods that can be used to setup test data.
 * 
 * @author johannes2
 *
 */
@Component
public class DataHelper {

	@Autowired
	private RoleService roleService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private MeshUserService userService;

	public MeshNode addNode(MeshNode parentNode, String name, Role role, Permission... perms) {
		MeshNode node = parentNode.create();
		for (Permission perm : perms) {
			role.addPermissions(node, perm);
		}
		return node;
	}

	public MeshUser addUser(UserRoot root, String name, Role role, Permission... perms) {
		
		MeshUser user = root.create("extraUser");
		for (Permission perm : perms) {
			role.addPermissions(user, perm);
		}
		return user;
	}
}
