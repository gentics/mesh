package com.gentics.mesh.core.data.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.MeshUserImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class GroupService extends AbstractMeshGraphService<Group> {

	public static GroupService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static GroupService getGroupService() {
		return instance;
	}

	public Group findByName(String name) {
		return findByName(name, GroupImpl.class);
	}

	@Override
	public List<? extends Group> findAll() {
		return fg.v().has(GroupImpl.class).toListExplicit(GroupImpl.class);
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

	@Override
	public Group findByUUID(String uuid) {
		return findByUUID(uuid, GroupImpl.class);
	}

}
