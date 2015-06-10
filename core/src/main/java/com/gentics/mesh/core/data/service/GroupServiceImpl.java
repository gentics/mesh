package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.paging.PagingInfo;

@Component
public class GroupServiceImpl extends GenericNodeServiceImpl<Group> implements GroupService {

	@Override
	public Group findByName(String name) {
		return null;

	}

	@Override
	public Group findByUUID(String uuid) {
		return null;
	}

	// TODO handle depth
	@Override
	public GroupResponse transformToRest(RoutingContext rc, Group group) {
		GroupResponse restGroup = new GroupResponse();
		restGroup.setUuid(group.getUuid());
		restGroup.setName(group.getName());

		//		for (User user : group.getUsers()) {
		//			user = neo4jTemplate.fetch(user);
		//			String name = user.getUsername();
		//			if (name != null) {
		//				restGroup.getUsers().add(name);
		//			}
		//		}
		//		Collections.sort(restGroup.getUsers());

		for (Role role : group.getRoles()) {
			//			role = neo4jTemplate.fetch(role);
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
	public Group save(Group group) {
		//		GroupRoot root = findRoot();
		//		if (root == null) {
		//			throw new NullPointerException("The group root node could not be found.");
		//		}
		//		group = neo4jTemplate.save(group);
		//		root.getGroups().add(group);
		//		neo4jTemplate.save(root);
		//		return group;
		return null;
	}

	public Group findOne(Long id) {
		return null;

	}

	/**
	 * Return all groups that are assigned to the user
	 * 
	 * @param user
	 * @return
	 */
	public List<Group> listAllGroups(User user) {
		//		@Query("start u=node({0}) MATCH (u)-[MEMBER_OF*]->(g) return g")
		return null;
	}

	public GroupRoot findRoot() {

		//		@Query("MATCH (n:GroupRoot) return n")
		return null;
	}

	@Override
	public Page<Group> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		//		return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
		//		@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
		//		Page<Group> findAll(User requestUser, Pageable pageable);

		return null;
	}

	@Override
	public Group create(String name) {
		Group group = framedGraph.addVertex(Group.class);
		group.setName(name);
		return group;
	}

	@Override
	public GroupRoot createRoot() {
		GroupRoot root = framedGraph.addVertex(GroupRoot.class);
		return root;
	}
}
