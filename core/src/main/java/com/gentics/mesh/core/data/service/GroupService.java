package com.gentics.mesh.core.data.service;

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

	public Group findByName(String name) {
		return framedGraph.v().has("name", name).has(Group.class).nextExplicit(Group.class);
	}

	public Group findByUUID(String uuid) {
		return framedGraph.v().has("uuid", uuid).has(Group.class).nextExplicit(Group.class);
	}

	public Group findOne(Long id) {
		// TODO move this in a dedicated utility class or ferma?
		return framedGraph.frameElement(framedGraph.getVertex(id), Group.class);
	}

	public GroupRoot findRoot() {
		return framedGraph.v().has(GroupRoot.class).nextExplicit(GroupRoot.class);
	}

	public Page<? extends Group> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
		// Page<Group> findAll(User requestUser, Pageable pageable);
		VertexTraversal traversal = framedGraph.v().has(MeshUser.class);
		Page<? extends Group> groups = TraversalHelper.getPagedResult(traversal, pagingInfo, Group.class);
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
