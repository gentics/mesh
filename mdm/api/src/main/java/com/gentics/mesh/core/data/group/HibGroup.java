package com.gentics.mesh.core.data.group;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;

import java.util.Objects;
import java.util.Set;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.HibReferenceableElement;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for group.
 */
public interface HibGroup extends HibCoreElement<GroupResponse>, HibReferenceableElement<GroupReference>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.GROUP, GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	@Override
	default String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/groups/" + getUuid();
	}

	@Override
	default boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		GroupDao groupDao = Tx.get().groupDao();
		boolean permissionChanged = false;
		if (recursive) {
			for (HibUser user : groupDao.getUsers(this)) {
				permissionChanged = user.applyPermissions(batch, role, false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = HibCoreElement.super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}

	/**
	 * Compose the document id for the group index.
	 * 
	 * @param groupUuid
	 * @return
	 */
	static String composeDocumentId(String groupUuid) {
		Objects.requireNonNull(groupUuid, "A groupUuid must be provided.");
		return groupUuid;
	}

	/**
	 * Compose the index name for the group index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return "group";
	}
}
