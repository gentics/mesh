package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public abstract class AbstractRootVertex<T extends GenericVertex<? extends RestModel>> extends MeshVertexImpl implements RootVertex<T> {

	abstract protected Class<? extends T> getPersistanceClass();

	abstract protected String getRootLabel();

	protected void addItem(T item) {
		// 1. Unlink all edges between both objects with the given label
		unlinkOut(item.getImpl(), getRootLabel());
		// 2. Create a new edge with the given label
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
	public RootVertex<T> findByUuid(String uuid, Handler<AsyncResult<T>> resultHandler) {
		Vertx vertx = MeshSpringConfiguration.getMeshSpringConfiguration().vertx();
		vertx.executeBlocking(rh -> {
			rh.complete(out(getRootLabel()).has(getPersistanceClass()).has("uuid", uuid).nextOrDefaultExplicit(getPersistanceClass(), null));
		}, resultHandler);
		return this;
	}

	@Override
	public Page<? extends T> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		VertexTraversal<?, ?, ?> traversal = out(getRootLabel()).has(getPersistanceClass()).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		VertexTraversal<?, ?, ?> countTraversal = out(getRootLabel()).has(getPersistanceClass()).mark().in(READ_PERM.label()).out(HAS_ROLE)
				.in(HAS_USER).retain(requestUser.getImpl()).back();
		Page<? extends T> items = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, getPersistanceClass());
		return items;
	}

}
