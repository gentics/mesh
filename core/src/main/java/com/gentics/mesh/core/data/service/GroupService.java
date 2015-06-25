package com.gentics.mesh.core.data.service;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class GroupService extends AbstractMeshService {

	public static GroupService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static GroupService getGroupService() {
		return instance;
	}

	public Group findByName(String name) {
		return fg.v().has("name", name).nextOrDefault(Group.class, null);
	}

	public Group findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).nextOrDefault(Group.class, null);
	}

	public Group findOne(Long id) {
		// TODO move this in a dedicated utility class or ferma?
		return fg.frameElement(fg.getVertex(id), Group.class);
	}

	public GroupRoot findRoot() {
		return fg.v().has(GroupRoot.class).nextOrDefault(GroupRoot.class, null);
	}

	public Page<? extends Group> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
		// Page<Group> findAll(User requestUser, Pageable pageable);
		VertexTraversal<?, ?, ?> traversal = fg.v().has(MeshUser.class);
		VertexTraversal<?, ?, ?> countTraversal = fg.v().has(MeshUser.class);
		Page<? extends Group> groups = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, Group.class);

		return groups;
	}

	public void delete(Group group) {
		group.getVertex().remove();
	}

}
