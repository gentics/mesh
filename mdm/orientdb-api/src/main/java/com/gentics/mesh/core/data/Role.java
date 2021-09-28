package com.gentics.mesh.core.data;

import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;

/**
 * Graph domain model interface for a role.
 */
public interface Role extends MeshCoreVertex<RoleResponse>, ReferenceableElement<RoleReference>, UserTrackingVertex, HibRole, GraphDBBucketableElement {

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
		return "role";
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

}
