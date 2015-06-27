package com.gentics.mesh.core.data.service;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.Group;
import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.data.model.impl.GroupImpl;
import com.gentics.mesh.core.data.model.impl.MeshUserImpl;
import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.impl.GroupRootImpl;
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
		return fg.v().has("name", name).has(GroupImpl.class).nextOrDefault(GroupImpl.class, null);
	}

	public Group findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).has(GroupImpl.class).nextOrDefault(GroupImpl.class, null);
	}

	public Group findOne(Long id) {
		// TODO move this in a dedicated utility class or ferma?
		return fg.frameElement(fg.getVertex(id), GroupImpl.class);
	}

	public GroupRoot findRoot() {
		return fg.v().has(GroupRootImpl.class).nextOrDefault(GroupRootImpl.class, null);
	}

	public Page<? extends Group> findAllVisible(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
		// Page<Group> findAll(User requestUser, Pageable pageable);
		VertexTraversal<?, ?, ?> traversal = fg.v().has(MeshUserImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = fg.v().has(MeshUserImpl.class);
		Page<? extends Group> groups = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, GroupImpl.class);

		return groups;
	}

	public void delete(Group group) {
		group.getImpl().getVertex().remove();
	}

}
