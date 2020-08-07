package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.event.EventQueueBatch;

public interface TagFamilyDaoWrapper extends TagFamilyDao, TagFamilyRoot {

	boolean update(TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch);

}
