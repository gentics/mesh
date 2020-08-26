package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibInNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.event.EventQueueBatch;

public interface HibNode extends HibCoreElement, HibCreatorTracking, HibInNode {

	String getElementVersion();

	NodeReference transformToReference(InternalActionContext ac);

	void updateTags(InternalActionContext ac, EventQueueBatch batch, List<TagReference> tags);

	HibProject getProject();

	// TODO rename method
	HibSchema getSchemaContainer();

	void setSchemaContainer(HibSchema container);

}
