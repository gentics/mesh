package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;

public class MicroschemaContainerRootImpl extends AbstractRootVertex<MicroschemaContainer> implements MicroschemaContainerRoot {

	@Override
	protected Class<? extends MicroschemaContainer> getPersistanceClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_SCHEMA_CONTAINER;
	}

}
