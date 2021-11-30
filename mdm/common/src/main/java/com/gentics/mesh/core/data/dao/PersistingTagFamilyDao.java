package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A persisting extension to {@link TagFamilyDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingTagFamilyDao extends TagFamilyDao, PersistingDaoGlobal<HibTagFamily>, ElementResolvingRootDao<HibProject, HibTagFamily> {

	@Override
	default boolean update(HibProject project, HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		// Don't update the item, if it does not belong to the requested root.
		if (!project.getUuid().equals(tagFamily.getProject().getUuid())) {
			throw error(NOT_FOUND, "object_not_found_for_uuid", tagFamily.getUuid());
		}
		return update(tagFamily, ac, batch);
	}
}
