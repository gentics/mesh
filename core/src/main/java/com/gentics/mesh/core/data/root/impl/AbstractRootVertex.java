package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractRootVertex<T extends GenericVertex<? extends RestModel>> extends MeshVertexImpl implements RootVertex<T> {

	private static Logger log = LoggerFactory.getLogger(AbstractRootVertex.class);

	/**
	 * Return the ferma graph persistance class for the items of the root vertex. (eg. NodeImpl, TagImpl...)
	 * 
	 * @return
	 */
	abstract protected Class<? extends T> getPersistanceClass();

	/**
	 * Return the label for the item edges.
	 * 
	 * @return
	 */
	abstract protected String getRootLabel();

	protected void addItem(T item) {
		setLinkOutTo(item.getImpl(), getRootLabel());
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
		resultHandler.handle(Future.succeededFuture(
				out(getRootLabel()).has(getPersistanceClass()).has("uuid", uuid).nextOrDefaultExplicit(getPersistanceClass(), null)));
		return this;
	}

	@Override
	public T findByUuidBlocking(String uuid) {
		return out(getRootLabel()).has(getPersistanceClass()).has("uuid", uuid).nextOrDefaultExplicit(getPersistanceClass(), null);
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

	@Override
	public void resolveToElement(Stack<String> stack, Handler<AsyncResult<? extends MeshVertex>> resultHandler) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + getPersistanceClass().getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			resultHandler.handle(Future.succeededFuture(this));
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				findByUuid(uuid, rh -> {
					if (rh.succeeded()) {
						resultHandler.handle(Future.succeededFuture(rh.result()));
					} else {
						resultHandler.handle(Future.failedFuture(rh.cause()));
					}
				});
			} else {
				resultHandler.handle(Future.failedFuture("Can't resolve remaining segments. Next segment would be: " + stack.peek()));
			}
		}
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (T t : findAll()) {
				t.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

}
