package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;

/**
 * Graph domain model interface for a role.
 */
public interface Role extends MeshCoreVertex<RoleResponse, Role>, ReferenceableElement<RoleReference>, UserTrackingVertex {

	/**
	 * Type Value: {@value #TYPE}
	 */
	public static final String TYPE = "role";

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
	 * @return
	 */
	Set<GraphPermission> getPermissions(MeshVertex element);

	/**
	 * Return a list of groups to which this role was assigned.
	 * 
	 * @return
	 */
	List<? extends Group> getGroups();

	/**
	 * Check whether the role grants the given permission on the given element.
	 * 
	 * @param permission
	 * @param element
	 * @return
	 */
	boolean hasPermission(GraphPermission permission, MeshVertex element);

}
