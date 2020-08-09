package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface TagDaoWrapper extends TagDao, DaoTransformable<Tag, TagResponse> {

	/**
	 * Find all tags of the given tagfamily.
	 * 
	 * @param tagFamily
	 * @return
	 */
	TraversalResult<? extends Tag> findAll(TagFamily tagFamily);

	Tag findByName(TagFamily tagFamily, String name);

	String getSubETag(Tag tag, InternalActionContext ac);

	void delete(Tag tag, BulkActionContext bac);

	boolean update(Tag tag, InternalActionContext ac, EventQueueBatch batch);

	String getETag(Tag tag, InternalActionContext ac);

	String getAPIPath(Tag tag, InternalActionContext ac);

	Tag loadObjectByUuid(Branch branch, InternalActionContext ac, String tagUuid, GraphPermission perm);

	TraversalResult<? extends Tag> findAllGlobal();

	Tag loadObjectByUuid(Project project, InternalActionContext ac, String tagUuid, GraphPermission readPerm);


}
