package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RxUtil;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public abstract class AbstractRootVertex<T extends MeshCoreVertex<? extends RestModel, T>> extends MeshVertexImpl implements RootVertex<T> {

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

	/**
	 * Add the given item to the this root vertex.
	 * 
	 * @param item
	 */
	protected void addItem(T item) {
		setLinkOutTo(item.getImpl(), getRootLabel());
	}

	/**
	 * Remove the given item from this root vertex.
	 * 
	 * @param item
	 */
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
	public Page<? extends T> findAll(MeshAuthUser requestUser, PagingParameter pagingInfo) throws InvalidArgumentException {
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

	/**
	 * Return the object with the given uuid if found within the specified root vertex. This method will not return null. Instead a
	 * {@link HttpStatusCodeErrorException} will be thrown when the object could not be found.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param root
	 * @return The found object
	 * @deprecated Use {@link #loadObjectByUuid(InternalActionContext, String, GraphPermission, RootVertex, Handler)} instead
	 */
	@Deprecated
	@Override
	public T loadObjectByUuidBlocking(InternalActionContext ac, String uuid, GraphPermission perm) {

		T object = findByUuidBlocking(uuid);
		if (object == null) {
			throw new EntityNotFoundException(ac.i18n("object_not_found_for_uuid", uuid));
		} else {
			MeshAuthUser requestUser = ac.getUser();
			if (requestUser.hasPermission(ac, object, perm)) {
				return object;
			} else {
				throw new InvalidPermissionException(ac.i18n("error_missing_perm", object.getUuid()));
			}

		}

	}

	/**
	 * Load the object by uuid and check the given permission.
	 * 
	 * @param ac
	 *            Context to be used in order to check user permissions
	 * @param uuid
	 *            Uuid of the object that should be loaded
	 * @param perm
	 *            Permission that must be granted in order to load the object
	 * @param root
	 *            Aggregation root vertex that should be used to find the element
	 * @param handler
	 *            handler that should be called when the object was successfully loaded or when an error occurred (401,404)
	 */
	@Override
	public void loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, Handler<AsyncResult<T>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		reload();
		findByUuid(uuid, rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
				return;
			} else if (rh.result() == null) {
				handler.handle(Future.failedFuture(new EntityNotFoundException(ac.i18n("object_not_found_for_uuid", uuid))));
				return;
			} else {
				db.noTrx(tc -> {
					T node = rh.result();
					MeshAuthUser requestUser = ac.getUser();
					requestUser.hasPermission(ac, node, perm, ph -> {
						db.noTrx(noTx -> {
							if (ph.failed()) {
								log.error("Error while checking permissions", ph.cause());
								handler.handle(failedFuture(BAD_REQUEST, "error_internal"));
							} else if (ph.succeeded() && ph.result()) {
								handler.handle(Future.succeededFuture(node));
								return;
							} else {
								handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", node.getUuid()))));
								return;
							}
						});
					});
				});
			}
		});

	}

	@Override
	public void loadObject(InternalActionContext ac, String uuidParameterName, GraphPermission perm, Handler<AsyncResult<T>> handler) {

		String uuid = ac.getParameter(uuidParameterName);
		if (StringUtils.isEmpty(uuid)) {
			handler.handle(failedFuture(BAD_REQUEST, "error_request_parameter_missing", uuidParameterName));
		} else {
			loadObjectByUuid(ac, uuid, perm, handler);
		}
	}

}
