package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;

import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;

import com.gentics.mesh.core.data.schema.impl.AbstractGraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.graphdb.spi.Database;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelHelper;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RestModelHelper;

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
