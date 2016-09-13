package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.Iterator;
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
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.InvalidArgumentException;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * Abstract implementation for root vertices which are aggregation vertices for mesh core vertices. The abstract implementation contains various helper methods
 * that are useful for loading lists and items from the root vertex.
 * 
 * @see RootVertex
 * @param <T>
 */
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
	public T findByName(String name) {
		return out(getRootLabel()).has(getPersistanceClass()).has("name", name).nextOrDefaultExplicit(getPersistanceClass(), null);
	}

	@Override
	public T findByName(InternalActionContext ac, String name, GraphPermission perm) {
		Database db = MeshInternal.get().database();
		reload();
		T element = findByName(name);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_name", name);
		}

		T result = db.noTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			String elementUuid = element.getUuid();
			if (requestUser.hasPermission(element, perm)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", elementUuid);
			}
		});

		return result;
	}

	@Override
	public T findByUuid(String uuid) {
		FramedGraph graph = Database.getThreadLocalGraph();
		// 1. Find the element with given uuid within the whole graph
		Iterator<Vertex> it = MeshInternal.get().database().getVertices(getPersistanceClass(), new String[] { "uuid" }, new String[] { uuid });
		if (it.hasNext()) {
			Vertex potentialElement = it.next();
			// 2. Use the edge index to determine whether the element is part of this root vertex
			Iterable<Edge> edges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_inout",
					MeshInternal.get().database().createComposedIndexKey(potentialElement.getId(), getId()));
			if (edges.iterator().hasNext()) {
				return graph.frameElementExplicit(potentialElement, getPersistanceClass());
			}
		}
		return null;
	}

	@Override
	public PageImpl<? extends T> findAll(InternalActionContext ac, PagingParameters pagingInfo) throws InvalidArgumentException {

		int page = pagingInfo.getPage();
		int perPage = pagingInfo.getPerPage();

		if (page < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(page));
		}
		if (perPage < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(perPage));
		}

		// Internally we start with page 0 instead of 1 to keep page range calculations simple.
		// External (for the enduser) all pages start with 1.
		page = page - 1;

		int low = page * perPage - 1;
		int upper = low + perPage;

		if (perPage == 0) {
			low = 0;
			upper = 0;
		}

		MeshAuthUser requestUser = ac.getUser();

		// Iterate over all vertices that are managed by this root vertex
		int count = 0;
		FramedGraph graph = Database.getThreadLocalGraph();
		Iterable<Edge> itemEdges = graph.getEdges("e." + getRootLabel().toLowerCase() + "_out", this.getId());
		List<T> elementsOfPage = new ArrayList<>();
		for (Edge itemEdge : itemEdges) {
			Vertex item = itemEdge.getVertex(Direction.IN);

			// Only handle those vertices which the user can read
			if (requestUser.hasPermissionForId(item.getId(), READ_PERM)) {

				// Only add those vertices to the list which are within the bounds of the requested page
				if (count > low && count <= upper) {
					elementsOfPage.add(graph.frameElementExplicit(item, getPersistanceClass()));
				}
				count++;
			}
		}

		// The totalPages of the list response must be zero if the perPage parameter is also zero.
		int totalPages = 0;
		if (perPage != 0) {
			totalPages = (int) Math.ceil(count / (double) (perPage));
		}

		return new PageImpl<T>(elementsOfPage, count, ++page, totalPages, elementsOfPage.size(), perPage);
	}

	@Override
	public Single<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + getPersistanceClass().getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			return Single.just(this);
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				return Single.just(findByUuid(uuid));
			} else {
				return Single.error(new Exception("Can't resolve remaining segments. Next segment would be: " + stack.peek()));
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
	public Single<T> loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		Database db = MeshInternal.get().database();
		reload();
		T element = findByUuid(uuid);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		T result = db.noTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			String elementUuid = element.getUuid();
			if (requestUser.hasPermission(element, perm)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", elementUuid);
			}
		});
		return Single.just(result);
	}

	@Override
	public T loadObjectByUuidSync(InternalActionContext ac, String uuid, GraphPermission perm) {
		Database db = MeshInternal.get().database();
		reload();
		T element = findByUuid(uuid);
		if (element == null) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
		}

		T result = db.noTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			String elementUuid = element.getUuid();
			if (requestUser.hasPermission(element, perm)) {
				return element;
			} else {
				throw error(FORBIDDEN, "error_missing_perm", elementUuid);
			}
		});

		return result;

	}

}
