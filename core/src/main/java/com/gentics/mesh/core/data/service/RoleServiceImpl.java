package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.auth.GraphPermission;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.mesh.core.repository.RoleRepository;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.paging.PagingInfo;

@Component
@Transactional(readOnly = true)
public class RoleServiceImpl extends GenericNodeServiceImpl<Role> implements RoleService {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private GraphDatabaseService graphDb;

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private UserService userService;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Override
	public Role findByUUID(String uuid) {
		return roleRepository.findByUUID(uuid);
	}

	@Override
	public Role findByName(String name) {
		return roleRepository.findByName(name);
	}

	@Override
	public Result<Role> findAll() {
		return roleRepository.findAll();
	}

	@Override
	public void addPermission(Role role, AbstractPersistable node, PermissionType... permissionTypes) {
		GraphPermission permission = getGraphPermission(role, node);
		// Create a new permission relation when no existing one could be found
		if (permission == null) {
			permission = new GraphPermission(role, node);
		}
		for (int i = 0; i < permissionTypes.length; i++) {
			permission.grant(permissionTypes[i]);
		}
		neo4jTemplate.save(permission);
	}

	@Override
	public GraphPermission getGraphPermission(Role role, AbstractPersistable node) {
		return roleRepository.findPermission(role.getId(), node.getId());
	}

	@Override
	public GraphPermission revokePermission(Role role, AbstractPersistable node, PermissionType... permissionTypes) {
		GraphPermission permission = getGraphPermission(role, node);
		// Create a new permission relation when no existing one could be found
		if (permission == null) {
			return null;
		}
		for (int i = 0; i < permissionTypes.length; i++) {
			permission.revoke(permissionTypes[i]);
		}
		role.addPermission(permission);
		permission = neo4jTemplate.save(permission);
		return permission;
	}

	@Override
	public RoleResponse transformToRest(Role role) {
		if (role == null) {
			throw new HttpStatusCodeErrorException(500, "Role can't be null");
		}
		RoleResponse restRole = new RoleResponse();
		restRole.setUuid(role.getUuid());
		restRole.setName(role.getName());

		for (Group group : role.getGroups()) {
			group = neo4jTemplate.fetch(group);
			GroupResponse restGroup = new GroupResponse();
			restGroup.setName(group.getName());
			restGroup.setUuid(group.getUuid());
			restRole.getGroups().add(restGroup);
		}

		return restRole;
	}

	@Override
	public void addCRUDPermissionOnRole(RoutingContext rc, MeshPermission meshPermission, GenericNode targetNode) {

		User user = userService.findByUUID(rc.session().getPrincipal().getString("uuid"));

		// 1. Determine all roles that grant given permission
		Node userNode = neo4jTemplate.getPersistentState(user);
		Set<Role> roles = new HashSet<>();
		for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
				.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
				.relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
				.traverse(userNode).relationships()) {

			if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getType().name())) {
				// Check whether this relation in fact targets our object we want to check
				boolean matchesTargetNode = rel.getEndNode().getId() == meshPermission.getTargetNode().getId();
				if (matchesTargetNode) {
					// Convert the api relationship to a SDN relationship
					GraphPermission perm = neo4jTemplate.load(rel, GraphPermission.class);
					if (meshPermission.implies(perm) == true) {
						// This permission is permitting. Add it to the list of roles
						roles.add(perm.getRole());
					}
				}
			}
		}

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : roles) {
			neo4jTemplate.fetch(role);
			addPermission(role, targetNode, PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE, PermissionType.DELETE);
		}
	}

	@Override
	public Page<Role> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		return roleRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
	}

}
