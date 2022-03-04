package com.gentics.mesh.core.data.role;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.HibReferenceableElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for role.
 */
public interface HibRole extends HibCoreElement<RoleResponse>, HibReferenceableElement<RoleReference>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.ROLE, ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED);

	/**
	 * Remove the element.
	 */
	void removeElement();

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return a result of groups to which this role was assigned.
	 * 
	 * @return Result
	 */
	Result<? extends HibGroup> getGroups();

	@Override
	default String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/roles/" + getUuid();
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
