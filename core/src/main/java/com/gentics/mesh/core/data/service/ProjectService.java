package com.gentics.mesh.core.data.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class ProjectService extends AbstractMeshGraphService<Project> {

	@Autowired
	protected MeshUserService userService;

	public Project findByName(String name) {
		return findByName(name, ProjectImpl.class);
	}

	@Override
	public List<? extends Project> findAll() {
		return fg.v().has(ProjectImpl.class).toListExplicit(ProjectImpl.class);
	}

	public Page<? extends Project> findAllVisible(MeshUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return project ORDER BY project.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return count(project)")
		// TODO check whether it is faster to use meshroot for starting the traversal
		VertexTraversal<?, ?, ?> traversal = fg.v().has(ProjectRootImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = fg.v().has(ProjectRootImpl.class);

		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, ProjectImpl.class);

	}

	@Override
	public Project findByUUID(String uuid) {
		return findByUUID(uuid, ProjectImpl.class);
	}

}
