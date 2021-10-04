package com.gentics.mesh.core.data.group;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for group.
 */
public interface HibGroup extends HibCoreElement<GroupResponse>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.GROUP, GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Transform the group to a reference POJO.
	 * 
	 * @return
	 */
	GroupReference transformToReference();

	/**
	 * Delete the element.
	 * 
	 * TODO: This method should be removed in the future.
	 */
	void removeElement();

	/**
	 * Return the current element version.
	 * 
	 * TODO: Check how versions can be accessed via Hibernate and refactor / remove this method accordingly
	 * @return
	 */
	String getElementVersion();

	@Override
	default String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/groups/" + getUuid();
	}

	@Override
	default GroupResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GroupDao groupDao = Tx.get().groupDao();
		return groupDao.transformToRestSync(this, ac, level, languageTags);
	}
}
