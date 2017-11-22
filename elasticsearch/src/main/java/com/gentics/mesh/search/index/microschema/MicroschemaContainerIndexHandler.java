package com.gentics.mesh.search.index.microschema;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

/**
 * Handler for the elastic search microschema index.
 */
@Singleton
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	@Inject
	MicroschemaTransformer transformer;

	@Inject
	MicroschemaMappingProvider mappingProvider;

	@Inject
	public MicroschemaContainerIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return MicroschemaContainer.composeIndexName();
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return MicroschemaContainer.composeDocumentId(entry.getElementUuid());
	}

	@Override
	public Class<?> getElementClass() {
		return MicroschemaContainer.class;
	}

	@Override
	public MicroschemaTransformer getTransformer() {
		return transformer;
	}

	@Override
	public MicroschemaMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(MicroschemaContainer.TYPE);
	}

	@Override
	public RootVertex<MicroschemaContainer> getRootVertex() {
		return boot.meshRoot().getMicroschemaContainerRoot();
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String indexName = MicroschemaContainer.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping());
		return Collections.singletonMap(indexName, info);
	}

}
