package com.gentics.mesh.core.data.schema.impl;


import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;

import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.util.RestModelHelper;

import rx.Observable;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.util.RestModelHelper;

import rx.Observable;

/**
 * @see SchemaContainer
 */
public class SchemaContainerImpl extends AbstractGraphFieldSchemaContainer<Schema, SchemaReference, SchemaContainer, SchemaContainerVersion>
		implements SchemaContainer {

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected Class<? extends SchemaContainerVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaContainerImpl.class);
	}

	@Override
	public SchemaReference createEmptyReferenceModel() {
		return new SchemaReference();
	}

	@Override
	public RootVertex<SchemaContainer> getRoot() {
		return MeshRoot.getInstance().getSchemaContainerRoot();
	}

	@Override
	public String getType() {
		return SchemaContainer.TYPE;
	}

}
