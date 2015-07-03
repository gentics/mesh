package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	@Override
	protected Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_PROJECT;
	}

	public void addProject(Project project) {
		linkOut(project.getImpl(), HAS_PROJECT);
	}

	// TODO unique

	@Override
	public Project create(String name) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getOrCreateRootNode();
		SchemaContainerRoot schemaRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
		project.setSchemaRoot(schemaRoot);
		addProject(project);

		TagFamilyRoot tagFamilyRoot = project.createTagFamilyRoot();
		project.setTagFamilyRoot(tagFamilyRoot);
		return project;
	}

	@Override
	public ProjectRootImpl getImpl() {
		return this;
	}

	@Override
	public Page<? extends Project> findAllVisible(User requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return project ORDER BY project.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return count(project)")
		// TODO check whether it is faster to use meshroot for starting the traversal
		VertexTraversal<?, ?, ?> traversal = out(HAS_PROJECT).has(ProjectImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_PROJECT).has(ProjectImpl.class);

		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, ProjectImpl.class);

	}

}
