package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public abstract class AbstractRootVertex<T extends MeshCoreVertex<? extends RestModel, T>> extends MeshVertexImpl implements RootVertex<T> {

	private static Logger log = LoggerFactory.getLogger(AbstractRootVertex.class);

	@Override
	abstract public Class<? extends T> getPersistanceClass();

	@Override
	abstract public String getRootLabel();

	@Override
	public void addItem(T item) {
		setUniqueLinkOutTo(item.getImpl(), getRootLabel());
	}

	@Override
	public void removeItem(T item) {
		unlinkOut(item.getImpl(), getRootLabel());
	}

	@Override
	public List<? extends T> findAll() {
		return out(getRootLabel()).has(getPersistanceClass()).toListExplicit(getPersistanceClass());
	}

	@Override
	public Observable<T> findByName(String name) {
		return Observable.just(out(getRootLabel()).has(getPersistanceClass()).has("name", name).nextOrDefaultExplicit(getPersistanceClass(), null));
	}

	@Override
	public Observable<T> findByUuid(String uuid) {
		return Observable.just(findByUuidSync(uuid));
	}
	
	@Override
	public T findByUuidSync(String uuid) {
		return out(getRootLabel()).has(getPersistanceClass()).has("uuid", uuid).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	@Override
	public PageImpl<? extends T> findAll(InternalActionContext ac, PagingParameter pagingInfo) throws InvalidArgumentException {
		MeshAuthUser requestUser = ac.getUser();
		VertexTraversal<?, ?, ?> traversal = out(getRootLabel()).has(getPersistanceClass()).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		VertexTraversal<?, ?, ?> countTraversal = out(getRootLabel()).has(getPersistanceClass()).mark().in(READ_PERM.label()).out(HAS_ROLE)
				.in(HAS_USER).retain(requestUser.getImpl()).back();
		PageImpl<? extends T> items = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, getPersistanceClass());
		return items;
	}

	@Override
	public Observable<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + getPersistanceClass().getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			return Observable.just(this);
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				return findByUuid(uuid);
			} else {
				return Observable.error(new Exception("Can't resolve remaining segments. Next segment would be: " + stack.peek()));
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

	@Override
	public Observable<T> loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		Database db = MeshSpringConfiguration.getInstance().database();
		reload();
		return findByUuid(uuid).map(element -> {
			if (element == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
			}

			T result = db.noTrx(() -> {
				MeshAuthUser requestUser = ac.getUser();
				String elementUuid = element.getUuid();
				if (requestUser.hasPermissionSync(ac, element, perm)) {
					return element;
				} else {
					throw error(FORBIDDEN, "error_missing_perm", elementUuid);
				}
				//				return requestUser.hasPermissionAsync(ac, element, perm).map(hasPerm -> {
				//					if (hasPerm) {
				//						return element;
				//					} else {
				//						throw error(FORBIDDEN, "error_missing_perm", elementUuid);
				//					}
				//				});
			});

			return result;
		});

	}

}
