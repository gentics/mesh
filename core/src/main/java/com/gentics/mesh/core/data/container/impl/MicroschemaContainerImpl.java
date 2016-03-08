package com.gentics.mesh.core.data.container.impl;

import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphdb.spi.Database;

public class MicroschemaContainerImpl
		extends AbstractGraphFieldSchemaContainer<Microschema, MicroschemaReference, MicroschemaContainer, MicroschemaContainerVersion>
		implements MicroschemaContainer {

	@Override
	protected Class<MicroschemaContainerImpl> getContainerClass() {
		return MicroschemaContainerImpl.class;
	}

	@Override
	public String getType() {
		return MicroschemaContainer.TYPE;
	}

	@Override
	protected Class<? extends MicroschemaContainerVersion> getContainerVersionClass() {
		return MicroschemaContainerVersionImpl.class;
	}

	public static void checkIndices(Database database) {
		database.addVertexType(MicroschemaContainerImpl.class);
	}

	@Override
	public MicroschemaReference createEmptyReferenceModel() {
		return new MicroschemaReference();
	}

	@Override
	public RootVertex<MicroschemaContainer> getRoot() {
		return MeshRoot.getInstance().getMicroschemaContainerRoot();
	}

}
