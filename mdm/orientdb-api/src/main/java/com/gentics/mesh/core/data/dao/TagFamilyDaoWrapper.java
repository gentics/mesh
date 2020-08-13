package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface TagFamilyDaoWrapper extends TagFamilyDao, DaoWrapper<HibTagFamily>, DaoTransformable<HibTagFamily, TagFamilyResponse> {

	boolean update(HibTagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch);

	// Find all tag families across all project.
	// TODO rename this method once ready
	TraversalResult<? extends HibTagFamily> findAllGlobal();

	TraversalResult<? extends HibTagFamily> findAll(HibProject project);

	HibTagFamily create(HibProject project, InternalActionContext ac, EventQueueBatch batch, String uuid);

	HibTagFamily findByName(HibProject project, String name);

	HibTagFamily findByUuid(HibProject project, String uuid);

	void delete(HibTagFamily tagFamily, BulkActionContext bac);

	String getETag(HibTagFamily tagfamily, InternalActionContext mockActionContext);

	String getAPIPath(HibTagFamily tagFamily, InternalActionContext ac);
}
