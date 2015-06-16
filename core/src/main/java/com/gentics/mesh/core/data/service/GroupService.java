package com.gentics.mesh.core.data.service;

import io.vertx.ext.web.RoutingContext;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.PagingHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class GroupService extends AbstractMeshService {

	public Group findByName(String name) {
		return framedGraph.v().has("name", name).has(Group.class).nextExplicit(Group.class);
	}

	public Group findByUUID(String uuid) {
		return framedGraph.v().has("uuid", uuid).has(Group.class).nextExplicit(Group.class);
	}

	// TODO handle depth
	public GroupResponse transformToRest(RoutingContext rc, Group group) {
		GroupResponse restGroup = new GroupResponse();
		restGroup.setUuid(group.getUuid());
		restGroup.setName(group.getName());

		// for (User user : group.getUsers()) {
		// user = neo4jTemplate.fetch(user);
		// String name = user.getUsername();
		// if (name != null) {
		// restGroup.getUsers().add(name);
		// }
		// }
		// Collections.sort(restGroup.getUsers());

		for (Role role : group.getRoles()) {
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

	// @Override
	// public Group save(Group group) {
	// GroupRoot root = findRoot();
	// if (root == null) {
	// throw new NullPointerException("The group root node could not be found.");
	// }
	// group = neo4jTemplate.save(group);
	// root.getGroups().add(group);
	// neo4jTemplate.save(root);
	// return group;
	// return null;
	// }

	public Group findOne(Long id) {
		// TODO move this in a dedicated utility class or ferma?
		return framedGraph.frameElement(framedGraph.getVertex(id), Group.class);
	}

	public GroupRoot findRoot() {
		return framedGraph.v().has(GroupRoot.class).nextExplicit(GroupRoot.class);
	}

	public Page<? extends Group> findAllVisible(User requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
		// Page<Group> findAll(User requestUser, Pageable pageable);
		VertexTraversal traversal = framedGraph.v().has(User.class);
		Page<? extends Group> groups = PagingHelper.getPagedResult(traversal, pagingInfo, Group.class);
		return groups;
	}

	public Group create(String name) {
		Group group = framedGraph.addFramedVertex(Group.class);
		group.setName(name);
		GroupRoot root = findRoot();
		root.addGroup(group);
		return group;
	}

	public GroupRoot createRoot() {
		GroupRoot root = framedGraph.addFramedVertex(GroupRoot.class);
		return root;
	}

	public void delete(Group group) {
		group.getVertex().remove();
	}
}
