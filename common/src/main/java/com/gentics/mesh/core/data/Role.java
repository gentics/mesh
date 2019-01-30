package com.gentics.mesh.core.data;

import static com.gentics.mesh.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.MeshEvent.ROLE_UPDATED;

import java.util.Objects;
import java.util.Set;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Graph domain model interface for a role.
 */
public interface Role extends MeshCoreVertex<RoleResponse, Role>, ReferenceableElement<RoleReference>, UserTrackingVertex {

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "role";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, ROLE_CREATED.address, ROLE_UPDATED.address, ROLE_DELETED.address);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Compose the index name for the role index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return TYPE.toLowerCase();
	}

	/**
	 * Compose the document id for role index documents.
	 * 
	 * @param roleUuid
	 * @return
	 */
	static String composeDocumentId(String roleUuid) {
		Objects.requireNonNull(roleUuid, "A roleUuid must be provided.");
		return roleUuid;
	}

	/**
	 * Grant the given permissions on the vertex.
	 * 
	 * @param vertex
	 * @param permissions
	 */
	void grantPermissions(MeshVertex vertex, GraphPermission... permissions);

	/**
	 * Revoke the given permissions on the vertex.
	 * 
	 * @param vertex
	 * @param permissions
	 */
	void revokePermissions(MeshVertex vertex, GraphPermission... permissions);

	/**
	 * Return a set of permissions which the role is granting to the given element.
	 * 
	 * @param element
	 * @return Set of permissions of the element
	 */
	Set<GraphPermission> getPermissions(MeshVertex element);

	/**
	 * Return a traversal result of groups to which this role was assigned.
	 * 
	 * @return Result
	 */
	TraversalResult<? extends Group> getGroups();

	/**
	 * Return a page of groups to which this role was assigned.
	 * 
	 * @param user
	 * @param params
	 * @return Loaded page
	 */
	Page<? extends Group> getGroups(User user, PagingParameters params);

	/**
	 * Check whether the role grants the given permission on the given element.
	 * 
	 * @param permission
	 * @param element
	 * @return
	 */
	boolean hasPermission(GraphPermission permission, MeshVertex element);

}
