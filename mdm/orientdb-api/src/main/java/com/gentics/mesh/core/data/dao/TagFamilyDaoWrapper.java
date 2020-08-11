package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface TagFamilyDaoWrapper extends TagFamilyDao, DaoTransformable<TagFamily, TagFamilyResponse> {

	boolean update(TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch);

	// Find all tag families across all project.
	// TODO rename this method once ready
	TraversalResult<? extends TagFamily> findAllGlobal();

	TraversalResult<? extends TagFamily> findAll(Project project);

	TagFamily create(Project project, InternalActionContext ac, EventQueueBatch batch, String uuid);

	TagFamily findByName(Project project, String name);

	TagFamily findByUuid(Project project, String uuid);

	void delete(TagFamily tagFamily, BulkActionContext bac);
}
