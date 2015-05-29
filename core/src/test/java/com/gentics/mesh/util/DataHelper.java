package com.gentics.mesh.util;

import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.User;
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

	@Autowired
	private GraphDatabase graphDb;

	public MeshNode addNode(MeshNode parentNode, String name, Role role, PermissionType... perms) {
		MeshNode node = new MeshNode();
		try (Transaction tx = graphDb.beginTx()) {

			node = nodeService.save(node);

			for (PermissionType perm : perms) {
				roleService.addPermission(role, node, perm);
			}
			tx.success();
		}
		return node;
	}

	public User addUser(String name, Role role, PermissionType... perms) {
		User user = new User("extraUser");
		try (Transaction tx = graphDb.beginTx()) {
			user = userService.save(user);
			for (PermissionType perm : perms) {
				roleService.addPermission(role, user, perm);
			}
			tx.success();
		}
		return user;
	}
}
