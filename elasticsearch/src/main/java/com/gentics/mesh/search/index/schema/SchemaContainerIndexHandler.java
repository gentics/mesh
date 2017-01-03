package com.gentics.mesh.search.index.schema;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractIndexHandler;

/**
 * Handler for schema container index.
 */
@Singleton
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	@Inject
	SchemaTransformator transformator;

	@Inject
	public SchemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	public SchemaTransformator getTransformator() {
		return transformator;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return "schema_container";
	}

	@Override
	protected String getDocumentType(SearchQueueEntry entry) {
		return "schema_container";
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(getIndex(null));
	}

	@Override
	public String getKey() {
		return SchemaContainer.TYPE;
	}

	@Override
	protected RootVertex<SchemaContainer> getRootVertex() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

}
