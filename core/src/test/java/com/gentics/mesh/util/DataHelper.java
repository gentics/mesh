package com.gentics.mesh.util;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.UserService;

/**
 * Various helper methods that can be used to setup test data.
 * 
 * @author johannes2
 *
 */
@Component
//@Transactional(readOnly = true)
public class DataHelper {

	@Autowired
	private RoleService roleService;

	@Autowired
	private MeshNodeService nodeService;

	@Autowired
	private UserService userService;

	public MeshNode addNode(MeshNode parentNode, String name, Role role, PermissionType... perms) {
		MeshNode node = nodeService.create();
//		try (Transaction tx = graphDb.beginTx()) {

			for (PermissionType perm : perms) {
				roleService.addPermission(role, node, perm);
			}
//			tx.success();
//		}
		return node;
	}

	public User addUser(String name, Role role, PermissionType... perms) {
		User user = userService.create("extraUser");
//		try (Transaction tx = graphDb.beginTx()) {
			for (PermissionType perm : perms) {
				roleService.addPermission(role, user, perm);
			}
//			tx.success();
//		}
		return user;
	}
}
