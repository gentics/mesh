package com.gentics.mesh.search.index.schema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

/**
 * Handler for schema container index.
 */
@Singleton
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	@Inject
	SchemaTransformer transformer;

	@Inject
	SchemaMappingProvider mappingProvider;

	@Inject
	public SchemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return SchemaContainer.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return SchemaContainer.composeIndexName();
	}

	@Override
	public Class<SchemaContainer> getElementClass() {
		return SchemaContainer.class;
	}

	@Override
	public SchemaTransformer getTransformer() {
		return transformer;
	}

	@Override
	public MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(SchemaContainer.TYPE.toLowerCase());
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = SchemaContainer.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping());
		return Collections.singletonMap(indexName, info);
	}

	@Override
	public RootVertex<SchemaContainer> getRootVertex() {
		return boot.meshRoot().getSchemaContainerRoot();
	}

}
