package com.gentics.mesh.core.data;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.parameter.PagingParameters;
import com.tinkerpop.blueprints.Vertex;

/**
 * A mesh vertex is a mesh element that exposes various graph OGM specific methods. We use the interface abstraction in order to hide certain ferma methods
 * which would otherwise clutter the API.
 */
public interface MeshVertex extends MeshElement, VertexFrame, HibBaseElement {

	String UUID_KEY = "uuid";

	/**
	 * Return the tinkerpop blueprint vertex of this mesh vertex.
	 * 
	 * @return Underlying vertex
	 */
	Vertex getVertex();

	/**
	 * Delete the element. Additional entries will be added to the batch to keep the search index in sync.
	 * 
	 * @param bac
	 *            Deletion context which keeps track of the deletion process
	 */
	void delete(BulkActionContext bac);

	/**
	 * Invoke deletion without any given bulk action context.
	 */
	default void delete() {
		delete(new DummyBulkActionContext());
	}

	/**
	 * Sets the cached uuid for the vertex.
	 * @param uuid
	 */
	void setCachedUuid(String uuid);

	/**
	 * Set the role uuid for the given permission.
	 *
	 * @param permission
	 * @param allowedRoles
	 */
	void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles);

	/**
	 * Return set of role uuids for the given permission that were granted on the element.
	 *
	 * @param permission
	 * @return
	 */
	Set<String> getRoleUuidsForPerm(InternalPermission permission);

	/**
	 * Parse a filter operation into the OrientDB's WHERE clause.
	 * 
	 * @param filter
	 * @param ctype container type to filter out
	 * @return
	 */
	String parseFilter(FilterOperation<?> filter, ContainerType ctype);

	/**
	 * Create a SQL permission restriction filter.
	 * 
	 * @param user running user
	 * @param permission actual permission
	 * @param maybeOwner optional owner entity
	 * @return
	 */
	public Optional<String> permissionFilter(HibUser user, InternalPermission permission, Optional<String> maybeOwner, Optional<ContainerType> containerType);

	/**
	 * Parse a filter operation into the OrientDB's WHERE clause.
	 * 
	 * @param filter
	 * @param ctype container type to filter out
	 * @return
	 */
	default String parseFilter(FilterOperation<?> filter, ContainerType ctype, HibUser user, InternalPermission permission, Optional<String> maybeOwner) {
		return parseFilter(filter, ctype) + permissionFilter(user, permission, maybeOwner, Optional.ofNullable(ctype)).map(permFilter -> " AND " + permFilter).orElse(StringUtils.EMPTY);
	}

	/**
	 * Set up native permission filter if the sorting is requested, since the sorting forces the native SQL data fetcher.
	 * 
	 * @param pagingInfo
	 * @param user
	 * @param permission
	 * @param maybeOwner
	 * @param containerType
	 * @return
	 */
	default Optional<String> permissionFilterIfRequired(PagingParameters pagingInfo, HibUser user, InternalPermission permission, Optional<String> maybeOwner, Optional<ContainerType> containerType) {
		return PersistingRootDao.shouldSort(pagingInfo) ? permissionFilter(user, permission, maybeOwner, containerType) : Optional.empty();
	}
}
