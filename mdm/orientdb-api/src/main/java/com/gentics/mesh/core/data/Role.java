package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;

/**
 * Graph domain model interface for a role.
 */
public interface Role extends MeshCoreVertex<RoleResponse, Role>, ReferenceableElement<RoleReference>, UserTrackingVertex, HibRole {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.ROLE, ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED);

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

	/**
	 * Return a result of groups to which this role was assigned.
	 * 
	 * @return Result
	 */
	Result<? extends Group> getGroups();

}
