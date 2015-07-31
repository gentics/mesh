package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SEARCH_QUEUE_ROOT;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.root.SearchQueueRoot;

public class SearchQueueRootImpl extends AbstractRootVertex<GenericVertex<?>> implements SearchQueueRoot {

	@Override
	protected Class<? extends GenericVertex<?>> getPersistanceClass() {
		return null;
	}

	@Override
	protected String getRootLabel() {
		return HAS_SEARCH_QUEUE_ROOT;
	}

}
