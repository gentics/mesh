package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;

/**
 * Graph domain model interface for a role.
 */
public interface Role extends MeshCoreVertex<RoleResponse>, ReferenceableElement<RoleReference>, UserTrackingVertex, HibRole, GraphDBBucketableElement {
}
