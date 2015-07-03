package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.*;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public abstract class AbstractRootVertex<T extends MeshVertex> extends MeshVertexImpl implements RootVertex<T> {

	abstract protected Class<? extends T> getPersistanceClass();

	abstract protected String getRootLabel();

	@Override
	public List<? extends T> findAll() {
		return out(getRootLabel()).has(getPersistanceClass()).toListExplicit(getPersistanceClass());
	}

	@Override
	public T findByName(String name) {
		return out(getRootLabel()).has(getPersistanceClass()).has("name", name).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	@Override
	public T findByUUID(String uuid) {
		return out(getRootLabel()).has(getPersistanceClass()).has("uuid", uuid).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	protected T findByNameAndProject(String projectName, String name) {
		return out(getRootLabel()).has(getPersistanceClass()).has("name", name).mark().has(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).has("name" , projectName).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	@Override
	public Page<? extends T> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// @Override
		// public Page<SchemaContainer> findAll(UserImpl requestUser, Pageable pageable) {
		// // @Query(value =
		// //
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return schema ORDER BY schema.name",
		// // countQuery =
		// //
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(schema:ObjectSchema) where id(requestUser) = {0} and perm.`permissions-read` = true return count(schema)")
		// return null;
		// }

		// return groupRepository.findAll(requestUser, new MeshPageRequest(pagingInfo));
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleGroup ORDER BY visibleGroup.name",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleGroup:Group) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleGroup)")
		// Page<Group> findAll(User requestUser, Pageable pageable);
		// /**
		// * Find all users that are visible for the given user.
		// *
		// * @throws InvalidArgumentException
		// */
		// @Override
		// public Page<? extends User> findAllVisible(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().out(HAS_GROUP).out(HAS_ROLE).out(READ_PERM.label()).has(UserImpl.class);
		// VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().out(HAS_GROUP).out(HAS_ROLE).out(READ_PERM.label()).has(UserImpl.class);
		// return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, UserImpl.class);
		// }

		VertexTraversal<?, ?, ?> traversal = out(getRootLabel()).has(getPersistanceClass());
		VertexTraversal<?, ?, ?> countTraversal = out(getRootLabel()).has(getPersistanceClass());
		Page<? extends T> items = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, getPersistanceClass());

		return items;
	}
}
