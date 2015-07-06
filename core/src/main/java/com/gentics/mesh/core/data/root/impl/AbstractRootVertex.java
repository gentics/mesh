package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
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

	protected void addItem(T item) {
		linkOut(item.getImpl(), getRootLabel());
	}

	protected void removeItem(T item) {
		unlinkOut(item.getImpl(), getRootLabel());
	}

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
		return out(getRootLabel()).has(getPersistanceClass()).has("name", name).mark().out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class)
				.has("name", projectName).back().nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	@Override
	public Page<? extends T> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		// @Override
		// public Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// // TODO filter for permissions
		// VertexTraversal<?, ?, ?> traversal = out(HAS_ROLE).has(RoleImpl.class);
		// VertexTraversal<?, ?, ?> countTraversal = out(HAS_ROLE).has(RoleImpl.class);
		// return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
		// // public Page<Role> findAll(String userUuid, Pageable pageable) {
		// // // @Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
		// //
		// // // countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		// // return null;
		// // }
		// // TODO filter for permissions?
		// }
		// @Override
		// public Page<? extends Project> findAllVisible(User requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// // @Query(value =
		// //
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return project ORDER BY project.name",
		// // countQuery =
		// //
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return count(project)")
		// // TODO check whether it is faster to use meshroot for starting the traversal
		// VertexTraversal<?, ?, ?> traversal = out(HAS_PROJECT).has(ProjectImpl.class);
		// VertexTraversal<?, ?, ?> countTraversal = out(HAS_PROJECT).has(ProjectImpl.class);
		//
		// return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, ProjectImpl.class);
		//
		// }
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

//	@Override
//	public MeshVertexImpl getImpl() {
//		return super.getImpl();
//	}
}
