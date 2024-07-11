package com.gentics.mesh.core.data.group;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.BucketableElement;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.NamedBaseElement;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.data.user.UserTracking;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for group.
 */
public interface Group extends CoreElement<GroupResponse>, ReferenceableElement<GroupReference>, UserTracking, BucketableElement, NamedBaseElement {

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
	default boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		GroupDao groupDao = Tx.get().groupDao();
		UserDao userDao = Tx.get().userDao();
		boolean permissionChanged = false;
		if (recursive) {
			for (User user : groupDao.getUsers(this).stream().filter(e -> userDao.hasPermission(authUser.getDelegate(), this, InternalPermission.READ_PERM)).collect(Collectors.toList())) {
				permissionChanged = user.applyPermissions(authUser, batch, role, false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = CoreElement.super.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
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
